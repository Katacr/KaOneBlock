package org.katacr.kaOneBlock;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {
    private final KaOneBlock plugin;
    private Connection connection;

    public DatabaseManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/data.db");
            plugin.debug("Connected to SQLite database");

            // 创建表
            String createTableSQL = "CREATE TABLE IF NOT EXISTS generated_blocks (" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "player_uuid TEXT NOT NULL UNIQUE," + "player_name TEXT NOT NULL," + "x INTEGER NOT NULL," + "y INTEGER NOT NULL," + "z INTEGER NOT NULL," + "block_type TEXT NOT NULL DEFAULT 'STONE'," + "stage_file TEXT NOT NULL DEFAULT 'normal.yml'," + "blocks_broken INTEGER NOT NULL DEFAULT 0," + "generated_time DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
                plugin.debug("Created table generated_blocks");
            }

            // 检查并添加缺失的列
            addMissingColumnIfNeeded("stage_file", "TEXT", "'normal.yml'");
            addMissingColumnIfNeeded("blocks_broken", "INTEGER", "0");

        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    private void addMissingColumnIfNeeded(String columnName, String columnType, String defaultValue) {
        try {
            // 尝试查询该列
            String testSQL = "SELECT " + columnName + " FROM generated_blocks LIMIT 1";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeQuery(testSQL);
            }
        } catch (SQLException e) {
            // 如果列不存在，添加它
            if (e.getMessage().contains("no such column")) {
                try (Statement stmt = connection.createStatement()) {
                    String alterSQL = "ALTER TABLE generated_blocks ADD COLUMN " + columnName + " " + columnType + " DEFAULT " + defaultValue;
                    stmt.execute(alterSQL);
                    plugin.debug("Added missing column: " + columnName);
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to add column " + columnName, ex);
                }
            }
        }
    }

    /**
     * 检查并添加缺失的列
     */
    private void addMissingColumnIfNeeded() {
        try {
            // 检查 block_type 列是否存在
            String checkSQL = "SELECT block_type FROM generated_blocks LIMIT 1";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeQuery(checkSQL);
            }
        } catch (SQLException e) {
            // 如果列不存在，添加它
            if (e.getMessage().contains("no such column")) {
                try (Statement stmt = connection.createStatement()) {
                    String alterSQL = "ALTER TABLE generated_blocks ADD COLUMN block_type TEXT DEFAULT 'STONE'";
                    stmt.execute(alterSQL);
                    plugin.getLogger().info("Added missing column: block_type");
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to add column block_type", ex);
                }
            } else {
                plugin.getLogger().log(Level.SEVERE, "Error checking column block_type", e);
            }
        }
    }

    /**
     * 更新玩家阶段信息
     */
    public void updatePlayerStage(UUID playerUuid, String stageFile, int blocksBroken) {
        String updateSQL = "UPDATE generated_blocks SET stage_file = ?, blocks_broken = ? WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setString(1, stageFile);
            pstmt.setInt(2, blocksBroken);
            pstmt.setString(3, playerUuid.toString());
            int updatedRows = pstmt.executeUpdate();

            if (updatedRows == 0) {
                plugin.getLogger().warning("No rows updated for player: " + playerUuid);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update player stage", e);

            // 输出详细的错误信息
            plugin.getLogger().severe("SQL: " + updateSQL);
            plugin.getLogger().severe("Parameters: ");
            plugin.getLogger().severe(" - stage_file: " + stageFile);
            plugin.getLogger().severe(" - blocks_broken: " + blocksBroken);
            plugin.getLogger().severe(" - player_uuid: " + playerUuid);
        }

        plugin.debug("Updated player stage: " + playerUuid + " -> " + stageFile + " (blocks: " + blocksBroken + ")");
    }

    public void checkTableStructure() {
        // 列出所有列
        String querySQL = "PRAGMA table_info(generated_blocks)";

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(querySQL)) {

            plugin.getLogger().info("Database table structure:");
            while (rs.next()) {
                String name = rs.getString("name");
                String type = rs.getString("type");
                String dflt = rs.getString("dflt_value");
                plugin.getLogger().info(" - " + name + " (" + type + ") DEFAULT " + dflt);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check table structure", e);
        }
    }

    /**
     * 记录方块生成事件
     */
    public void logBlockGeneration(UUID playerUuid, String playerName, int x, int y, int z, String blockType, String stageFile, int blocksBroken) {

        String insertSQL = "INSERT INTO generated_blocks (player_uuid, player_name, x, y, z, block_type, stage_file, blocks_broken) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, playerName);
            pstmt.setInt(3, x);
            pstmt.setInt(4, y);
            pstmt.setInt(5, z);
            pstmt.setString(6, blockType);
            pstmt.setString(7, stageFile);
            pstmt.setInt(8, blocksBroken);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to log block generation", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to close database connection", e);
        }
    }

    /**
     * 根据位置查找方块记录
     */
    public Map<String, Object> findBlockByLocation(int x, int y, int z) {
        String querySQL = "SELECT * FROM generated_blocks WHERE x = ? AND y = ? AND z = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setInt(1, x);
            pstmt.setInt(2, y);
            pstmt.setInt(3, z);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> blockInfo = new HashMap<>();
                blockInfo.put("id", rs.getInt("id"));
                blockInfo.put("player_uuid", rs.getString("player_uuid"));
                blockInfo.put("player_name", rs.getString("player_name"));
                blockInfo.put("x", rs.getInt("x"));
                blockInfo.put("y", rs.getInt("y"));
                blockInfo.put("z", rs.getInt("z"));
                blockInfo.put("block_type", rs.getString("block_type"));
                return blockInfo;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to find block at location: " + x + ", " + y + ", " + z, e);
        }
        return null;
    }

    /**
     * 更新方块类型
     */
    public void updateBlockType(int recordId, String blockType) {
        String updateSQL = "UPDATE generated_blocks SET block_type = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(updateSQL)) {
            pstmt.setString(1, blockType);
            pstmt.setInt(2, recordId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update block record: " + recordId, e);
        }
    }

    /**
     * 检查玩家是否生成过方块
     */
    public boolean hasBlock(UUID playerUuid) {
        String querySQL = "SELECT COUNT(*) FROM generated_blocks WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check block for player: " + playerUuid, e);
        }
        return false;
    }

    /**
     * 根据玩家UUID查找方块记录
     */
    public Map<String, Object> findBlockByPlayer(UUID playerUuid) {
        String querySQL = "SELECT * FROM generated_blocks WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> blockInfo = new HashMap<>();
                blockInfo.put("id", rs.getInt("id"));
                blockInfo.put("x", rs.getInt("x"));
                blockInfo.put("y", rs.getInt("y"));
                blockInfo.put("z", rs.getInt("z"));
                blockInfo.put("block_type", rs.getString("block_type"));
                return blockInfo;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to find block for player: " + playerUuid, e);
        }
        return null;
    }

    /**
     * 根据玩家UUID删除方块记录
     */
    public boolean deleteBlock(UUID playerUuid) {
        String deleteSQL = "DELETE FROM generated_blocks WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setString(1, playerUuid.toString());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete block for player: " + playerUuid, e);
            return false;
        }
    }

    // DatabaseManager.java
    public StageManager.PlayerStageProgress loadPlayerProgress(UUID playerUuid) {
        String sql = "SELECT stage_file, blocks_broken FROM generated_blocks WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return new StageManager.PlayerStageProgress(rs.getString("stage_file"), rs.getInt("blocks_broken"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "加载玩家进度失败: " + playerUuid, e);
        }
        return null;
    }

    // 文档8: DatabaseManager.java
    public Map<UUID, StageManager.PlayerStageProgress> loadAllPlayerProgress() {
        Map<UUID, StageManager.PlayerStageProgress> progressMap = new HashMap<>();
        String sql = "SELECT player_uuid, stage_file, blocks_broken FROM generated_blocks";

        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String stageFile = rs.getString("stage_file");
                int blocksBroken = rs.getInt("blocks_broken");

                progressMap.put(uuid, new StageManager.PlayerStageProgress(stageFile, blocksBroken));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player progress", e);
        }
        return progressMap;
    }
}