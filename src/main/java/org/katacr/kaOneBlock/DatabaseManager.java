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
            String url = "jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/playerdata.db";
            connection = DriverManager.getConnection(url);

            // 创建表（如果不存在）
            String createTableSQL = "CREATE TABLE IF NOT EXISTS generated_blocks (" + "id INTEGER PRIMARY KEY AUTOINCREMENT," + "player_uuid TEXT NOT NULL," + "player_name TEXT NOT NULL," + "world_name TEXT NOT NULL," + "x INTEGER NOT NULL," + "y INTEGER NOT NULL," + "z INTEGER NOT NULL," + "block_type TEXT NOT NULL DEFAULT 'STONE'," + "generated_time DATETIME DEFAULT CURRENT_TIMESTAMP" + ")";

            try (Statement stmt = connection.createStatement()) {
                stmt.execute(createTableSQL);
                plugin.getLogger().info("Database table created/verified");

                // 检查并添加缺失的列
                addMissingColumnIfNeeded();
            }
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    /**
     * 检查并添加缺失的列
     */
    private void addMissingColumnIfNeeded() {
        try {
            // 检查列是否存在
            String checkSQL = "SELECT " + "block_type" + " FROM generated_blocks LIMIT 1";
            try (Statement stmt = connection.createStatement()) {
                stmt.executeQuery(checkSQL);
            }
        } catch (SQLException e) {
            // 如果列不存在，添加它
            if (e.getMessage().contains("no such column")) {
                try (Statement stmt = connection.createStatement()) {
                    String alterSQL = "ALTER TABLE generated_blocks ADD COLUMN " + "block_type" + " " + "TEXT DEFAULT 'STONE'";
                    stmt.execute(alterSQL);
                    plugin.getLogger().info("Added missing column: " + "block_type");
                } catch (SQLException ex) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to add column " + "block_type", ex);
                }
            } else {
                plugin.getLogger().log(Level.SEVERE, "Error checking column " + "block_type", e);
            }
        }
    }

    public void logBlockGeneration(UUID playerUuid, String playerName, String worldName, int x, int y, int z, String blockType) {
        String insertSQL = "INSERT INTO generated_blocks (player_uuid, player_name, world_name, x, y, z, block_type) " + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(insertSQL)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, playerName);
            pstmt.setString(3, worldName);
            pstmt.setInt(4, x);
            pstmt.setInt(5, y);
            pstmt.setInt(6, z);
            pstmt.setString(7, blockType);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to log block generation", e);
        }
        // 记录调试信息
        Map<String, String> replacements = new HashMap<>();
        replacements.put("world", worldName);
        replacements.put("x", String.valueOf(x));
        replacements.put("y", String.valueOf(y));
        replacements.put("z", String.valueOf(z));
        replacements.put("block", blockType);
        plugin.debug("debug-logged-generation", replacements);
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
     *
     * @param worldName 世界名称
     * @param x         X坐标
     * @param y         Y坐标
     * @param z         Z坐标
     * @return 包含方块信息的Map，如果没有找到则返回null
     */
    public Map<String, Object> findBlockByLocation(String worldName, int x, int y, int z) {
        String querySQL = "SELECT * FROM generated_blocks WHERE world_name = ? AND x = ? AND y = ? AND z = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, worldName);
            pstmt.setInt(2, x);
            pstmt.setInt(3, y);
            pstmt.setInt(4, z);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> blockInfo = new HashMap<>();
                blockInfo.put("id", rs.getInt("id"));
                blockInfo.put("player_uuid", rs.getString("player_uuid"));
                blockInfo.put("player_name", rs.getString("player_name"));
                blockInfo.put("world_name", rs.getString("world_name"));
                blockInfo.put("x", rs.getInt("x"));
                blockInfo.put("y", rs.getInt("y"));
                blockInfo.put("z", rs.getInt("z"));
                blockInfo.put("block_type", rs.getString("block_type"));
                return blockInfo;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to find block at location: " + worldName + ", " + x + ", " + y + ", " + z, e);
        }
        return null;
    }


    /**
     * 更新方块类型
     *
     * @param recordId  记录ID
     * @param blockType 新的方块类型
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
     * 检查玩家是否在指定世界生成过方块
     *
     * @param playerUuid 玩家UUID
     * @param worldName  世界名称
     * @return 如果存在记录返回true，否则返回false
     */
    public boolean hasBlockInWorld(UUID playerUuid, String worldName) {
        String querySQL = "SELECT COUNT(*) FROM generated_blocks WHERE player_uuid = ? AND world_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, worldName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check block in world for player: " + playerUuid, e);
        }
        return false;
    }

    /**
     * 根据玩家UUID和世界名称查找方块记录
     *
     * @param playerUuid 玩家UUID
     * @param worldName  世界名称
     * @return 包含方块信息的Map，如果没有找到则返回null
     */
    public Map<String, Object> findBlockByPlayerAndWorld(UUID playerUuid, String worldName) {
        String querySQL = "SELECT * FROM generated_blocks WHERE player_uuid = ? AND world_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, worldName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                Map<String, Object> blockInfo = new HashMap<>();
                blockInfo.put("id", rs.getInt("id"));
                blockInfo.put("world_name", rs.getString("world_name"));
                blockInfo.put("x", rs.getInt("x"));
                blockInfo.put("y", rs.getInt("y"));
                blockInfo.put("z", rs.getInt("z"));
                blockInfo.put("block_type", rs.getString("block_type")); // 添加方块类型
                return blockInfo;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to find block for player in world: " + playerUuid + ", " + worldName, e);
        }
        return null;
    }

    /**
     * 根据玩家UUID和世界名称删除方块记录
     *
     * @param playerUuid 玩家UUID
     * @param worldName  世界名称
     * @return 是否成功删除
     */
    public boolean deleteBlockByPlayerAndWorld(UUID playerUuid, String worldName) {
        String deleteSQL = "DELETE FROM generated_blocks WHERE player_uuid = ? AND world_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, worldName);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete block for player in world: " + playerUuid + ", " + worldName, e);
            return false;
        }
    }
}