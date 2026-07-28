package org.katacr.kaOneBlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Owns the SQLite schema, world-aware in-memory indexes and serialized persistence queue.
 */
public class DatabaseManager {
    private static final String UPSERT_SQL = """
            INSERT INTO generated_blocks
                (player_uuid, player_name, world_uuid, world_name, x, y, z, block_type, stage_file, blocks_broken)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET
                player_name = excluded.player_name,
                world_uuid = excluded.world_uuid,
                world_name = excluded.world_name,
                x = excluded.x,
                y = excluded.y,
                z = excluded.z,
                block_type = excluded.block_type,
                stage_file = excluded.stage_file,
                blocks_broken = excluded.blocks_broken
            """;

    private final KaOneBlock plugin;
    private final Map<UUID, GeneratedBlockRecord> recordsByPlayer = new ConcurrentHashMap<>();
    private final Map<BlockPosition, GeneratedBlockRecord> recordsByPosition = new ConcurrentHashMap<>();
    private final Map<UUID, GeneratedBlockRecord> pendingWrites = new ConcurrentHashMap<>();
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "KaOneBlock-SQLite");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean flushScheduled = new AtomicBoolean();
    private String databaseUrl;
    private volatile boolean initialized;
    private volatile boolean closed;

    public DatabaseManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates or migrates the database and loads all authoritative records into memory.
     */
    public boolean initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            File databaseFile = new File(plugin.getDataFolder(), "data.db");
            databaseUrl = "jdbc:sqlite:" + databaseFile.getAbsolutePath();

            try (Connection connection = openConnection()) {
                connection.setAutoCommit(false);
                createOrMigrateSchema(connection);
                if (!migrateLegacyWorld(connection)) {
                    connection.rollback();
                    return false;
                }
                connection.commit();
                if (!loadRecords(connection)) {
                    return false;
                }
            }

            initialized = true;
            plugin.debug("Connected to SQLite database with " + recordsByPlayer.size() + " records");
            return true;
        } catch (ClassNotFoundException | SQLException | IllegalArgumentException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", exception);
            return false;
        }
    }

    /**
     * Inserts a new block record synchronously so an untracked physical block is never created.
     */
    public boolean createBlock(GeneratedBlockRecord record) {
        ensureInitialized();
        if (recordsByPlayer.containsKey(record.playerId()) || recordsByPosition.containsKey(record.position())) {
            return false;
        }

        try {
            persistImmediately(record);
            recordsByPlayer.put(record.playerId(), record);
            recordsByPosition.put(record.position(), record);
            return true;
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create block record for " + record.playerId(), exception);
            return false;
        }
    }

    /**
     * Finds the registered block at an exact world-aware Bukkit location.
     */
    public Optional<GeneratedBlockRecord> findBlockByLocation(Location location) {
        if (location.getWorld() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(recordsByPosition.get(BlockPosition.from(location)));
    }

    /**
     * Finds the single registered block owned by a player.
     */
    public Optional<GeneratedBlockRecord> findBlockByPlayer(UUID playerId) {
        return Optional.ofNullable(recordsByPlayer.get(playerId));
    }

    /**
     * Reports whether a player already owns a registered OneBlock.
     */
    public boolean hasBlock(UUID playerId) {
        return recordsByPlayer.containsKey(playerId);
    }

    /**
     * Updates cached gameplay state immediately and coalesces its SQLite write off the server thread.
     */
    public void updateState(UUID playerId, String blockType, String stageFile, int blocksBroken) {
        GeneratedBlockRecord current = recordsByPlayer.get(playerId);
        if (current == null) {
            return;
        }

        GeneratedBlockRecord updated = current.withState(blockType, stageFile, blocksBroken);
        recordsByPlayer.put(playerId, updated);
        recordsByPosition.put(updated.position(), updated);
        pendingWrites.put(playerId, updated);
        scheduleFlush();
    }

    /**
     * Deletes a player's record after all earlier queued writes and then removes its cache entries.
     */
    public boolean deleteBlock(UUID playerId) {
        GeneratedBlockRecord record = recordsByPlayer.get(playerId);
        if (record == null) {
            return false;
        }

        pendingWrites.remove(playerId);
        try {
            Future<Boolean> deletion = writer.submit(() -> {
                try (Connection connection = openConnection();
                     PreparedStatement statement = connection.prepareStatement("DELETE FROM generated_blocks WHERE player_uuid = ?")) {
                    statement.setString(1, playerId.toString());
                    return statement.executeUpdate() > 0;
                }
            });
            if (!deletion.get()) {
                return false;
            }
            recordsByPlayer.remove(playerId, record);
            recordsByPosition.remove(record.position(), record);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete block for " + playerId, exception);
            return false;
        }
    }

    /**
     * Returns an immutable snapshot of all persisted player progress for startup hydration.
     */
    public Map<UUID, StageManager.PlayerStageProgress> loadAllPlayerProgress() {
        Map<UUID, StageManager.PlayerStageProgress> progress = new HashMap<>();
        recordsByPlayer.forEach((playerId, record) -> progress.put(
                playerId,
                new StageManager.PlayerStageProgress(record.stageFile(), record.blocksBroken())
        ));
        return Map.copyOf(progress);
    }

    /**
     * Logs the current SQLite columns for the administrative diagnostic command.
     */
    public void checkTableStructure() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(generated_blocks)")) {
            plugin.getLogger().info("Database table structure:");
            while (result.next()) {
                plugin.getLogger().info(" - " + result.getString("name") + " (" + result.getString("type") + ")");
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to inspect database structure", exception);
        }
    }

    /**
     * Flushes all coalesced writes and shuts down the dedicated SQLite writer.
     */
    public void close() {
        if (!initialized || closed) {
            return;
        }
        closed = true;
        try {
            writer.submit(this::flushPendingWrites).get();
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to flush database writes", exception);
        }
        writer.shutdown();
        try {
            if (!writer.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Timed out while closing the SQLite writer");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates the latest schema while retaining compatible columns from older releases.
     */
    private void createOrMigrateSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS generated_blocks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL UNIQUE,
                        player_name TEXT NOT NULL,
                        world_uuid TEXT,
                        world_name TEXT,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        block_type TEXT NOT NULL DEFAULT 'STONE',
                        stage_file TEXT NOT NULL DEFAULT 'normal.yml',
                        blocks_broken INTEGER NOT NULL DEFAULT 0,
                        generated_time DATETIME DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
        }

        Map<String, String> columns = readColumns(connection);
        addColumnIfMissing(connection, columns, "world_uuid", "TEXT");
        addColumnIfMissing(connection, columns, "world_name", "TEXT");
        addColumnIfMissing(connection, columns, "stage_file", "TEXT NOT NULL DEFAULT 'normal.yml'");
        addColumnIfMissing(connection, columns, "blocks_broken", "INTEGER NOT NULL DEFAULT 0");
        addColumnIfMissing(connection, columns, "block_type", "TEXT NOT NULL DEFAULT 'STONE'");

        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE INDEX IF NOT EXISTS idx_generated_blocks_location ON generated_blocks(world_uuid, x, y, z)");
        }
    }

    /**
     * Reads the current table columns without relying on localized SQLite error messages.
     */
    private Map<String, String> readColumns(Connection connection) throws SQLException {
        Map<String, String> columns = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("PRAGMA table_info(generated_blocks)")) {
            while (result.next()) {
                columns.put(result.getString("name"), result.getString("type"));
            }
        }
        return columns;
    }

    /**
     * Adds one known schema column when upgrading an older database.
     */
    private void addColumnIfMissing(Connection connection, Map<String, String> columns, String name, String definition) throws SQLException {
        if (columns.containsKey(name)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE generated_blocks ADD COLUMN " + name + " " + definition);
        }
    }

    /**
     * Backfills legacy rows through an explicit configured world rather than guessing by coordinates.
     */
    private boolean migrateLegacyWorld(Connection connection) throws SQLException {
        int missingWorlds;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM generated_blocks WHERE world_uuid IS NULL OR world_uuid = ''")) {
            missingWorlds = result.next() ? result.getInt(1) : 0;
        }
        if (missingWorlds == 0) {
            return true;
        }

        String legacyWorldName = plugin.getConfig().getString("legacy-world", "world");
        World legacyWorld = legacyWorldName == null ? null : Bukkit.getWorld(legacyWorldName);
        if (legacyWorld == null) {
            plugin.getLogger().severe("Cannot migrate " + missingWorlds + " legacy block records: configured legacy-world is not loaded");
            return false;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE generated_blocks SET world_uuid = ?, world_name = ? WHERE world_uuid IS NULL OR world_uuid = ''")) {
            statement.setString(1, legacyWorld.getUID().toString());
            statement.setString(2, legacyWorld.getName());
            statement.executeUpdate();
        }
        plugin.getLogger().warning("Migrated " + missingWorlds + " legacy records to world " + legacyWorld.getName());
        return true;
    }

    /**
     * Hydrates both indexes and rejects ambiguous duplicate world positions.
     */
    private boolean loadRecords(Connection connection) throws SQLException {
        recordsByPlayer.clear();
        recordsByPosition.clear();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT * FROM generated_blocks")) {
            while (result.next()) {
                GeneratedBlockRecord record = mapRecord(result);
                if (recordsByPosition.putIfAbsent(record.position(), record) != null) {
                    plugin.getLogger().severe("Duplicate OneBlock location detected at " + record.position());
                    return false;
                }
                recordsByPlayer.put(record.playerId(), record);
            }
        }
        return true;
    }

    /**
     * Maps a SQLite row to the typed domain record used by gameplay code.
     */
    private GeneratedBlockRecord mapRecord(ResultSet result) throws SQLException {
        UUID playerId = UUID.fromString(result.getString("player_uuid"));
        UUID worldId = UUID.fromString(result.getString("world_uuid"));
        String worldName = result.getString("world_name");
        if (worldName == null || worldName.isBlank()) {
            worldName = worldId.toString();
        }
        BlockPosition position = new BlockPosition(worldId, result.getInt("x"), result.getInt("y"), result.getInt("z"));
        return new GeneratedBlockRecord(
                playerId,
                result.getString("player_name"),
                position,
                worldName,
                result.getString("block_type"),
                result.getString("stage_file"),
                result.getInt("blocks_broken")
        );
    }

    /**
     * Schedules a single drain task while allowing later state changes to replace older pending writes.
     */
    private void scheduleFlush() {
        if (closed || !flushScheduled.compareAndSet(false, true)) {
            return;
        }
        writer.execute(() -> {
            try {
                flushPendingWrites();
            } finally {
                flushScheduled.set(false);
                if (!pendingWrites.isEmpty()) {
                    scheduleFlush();
                }
            }
        });
    }

    /**
     * Persists the latest snapshot for every player in one SQLite transaction.
     */
    private void flushPendingWrites() {
        List<GeneratedBlockRecord> batch = new ArrayList<>();
        pendingWrites.forEach((playerId, record) -> {
            if (pendingWrites.remove(playerId, record)) {
                batch.add(record);
            }
        });
        if (batch.isEmpty()) {
            return;
        }

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
                for (GeneratedBlockRecord record : batch) {
                    bindRecord(statement, record);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            batch.forEach(record -> pendingWrites.putIfAbsent(record.playerId(), record));
            plugin.getLogger().log(Level.SEVERE, "Failed to persist " + batch.size() + " block updates", exception);
        }
    }

    /**
     * Writes a newly created record before the corresponding block is placed in the world.
     */
    private void persistImmediately(GeneratedBlockRecord record) throws SQLException {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(UPSERT_SQL)) {
            bindRecord(statement, record);
            statement.executeUpdate();
        }
    }

    /**
     * Binds all record fields in the canonical upsert order.
     */
    private void bindRecord(PreparedStatement statement, GeneratedBlockRecord record) throws SQLException {
        statement.setString(1, record.playerId().toString());
        statement.setString(2, record.playerName());
        statement.setString(3, record.position().worldId().toString());
        statement.setString(4, record.worldName());
        statement.setInt(5, record.position().x());
        statement.setInt(6, record.position().y());
        statement.setInt(7, record.position().z());
        statement.setString(8, record.blockType());
        statement.setString(9, record.stageFile());
        statement.setInt(10, record.blocksBroken());
    }

    /**
     * Opens a configured short-lived connection suitable for serialized SQLite access.
     */
    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(databaseUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout = 5000");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA synchronous = NORMAL");
        }
        return connection;
    }

    /**
     * Prevents gameplay calls from silently using an unavailable database.
     */
    private void ensureInitialized() {
        if (!initialized || closed) {
            throw new IllegalStateException("Database is not available");
        }
    }
}
