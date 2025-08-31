package org.katacr.kaOneBlock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StageManager {
    private final KaOneBlock plugin;
    private final Map<UUID, PlayerStageProgress> playerProgress = new HashMap<>();

    public StageManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * 初始化玩家进度
     */
    public void initPlayerProgress(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerProgress.containsKey(playerId)) {
            // 默认从 normal.yml 开始
            setPlayerStage(player, "normal.yml");
        }
    }

    // 文档15: StageManager.java
    public void loadAllPlayerProgress() {
        Map<UUID, PlayerStageProgress> progressMap = plugin.getDatabaseManager().loadAllPlayerProgress();

        playerProgress.clear();
        playerProgress.putAll(progressMap);
    }

    /**
     * 设置玩家阶段并发送开始消息
     */
    public void setPlayerStageAndSendMessage(Player player, String stageFile) {
        UUID playerId = player.getUniqueId();

        // 确保文件名有扩展名
        if (!stageFile.endsWith(".yml")) {
            stageFile += ".yml";
        }

        // 加载阶段配置
        StageConfig config = plugin.getStageConfigManager().loadStageConfig(stageFile);
        if (config == null) return;

        // 设置或更新玩家进度
        PlayerStageProgress progress = playerProgress.get(playerId);
        if (progress == null) {
            progress = new PlayerStageProgress(stageFile, 0);
            playerProgress.put(playerId, progress);
        } else {
            progress.stageFile = stageFile;
            progress.blocksBroken = 0;
        }

        plugin.debug("设置玩家 " + player.getName() + " 阶段: " + stageFile);

        // 发送阶段开始消息
        if (config.message != null && !config.message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.message));
            plugin.debug("发送阶段消息: " + config.message);
        }
    }

    // StageManager.java
    public void onPlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerProgress.containsKey(playerId)) {
            // 从数据库加载进度
            PlayerStageProgress progress = plugin.getDatabaseManager().loadPlayerProgress(playerId);

            if (progress != null) {
                playerProgress.put(playerId, progress);
            } else {
                // 新玩家：设置初始阶段
                setPlayerStage(player, "normal.yml");
            }
        }
    }

    /**
     * 获取玩家当前阶段配置文件名
     */
    public String getCurrentStageFile(UUID playerId) {
        PlayerStageProgress progress = playerProgress.get(playerId);
        return progress != null ? progress.stageFile : "normal.yml";
    }

    /**
     * 增加玩家破坏方块计数
     */
    public void incrementBlocksBroken(UUID playerId) {
        PlayerStageProgress progress = playerProgress.get(playerId);
        if (progress != null) {
            progress.blocksBroken++;
            plugin.debug("玩家 " + Objects.requireNonNull(Bukkit.getPlayer(playerId)).getName() + " 破坏方块数: " + progress.blocksBroken);

            // 检查是否需要进入下一阶段
            checkStageAdvancement(playerId, progress);
        }
    }

    /**
     * 重置玩家阶段为指定阶段
     */
    public void resetPlayerStage(Player player, String stageFile) {
        UUID playerId = player.getUniqueId();

        // 确保文件名有扩展名
        if (!stageFile.endsWith(".yml")) {
            stageFile += ".yml";
        }

        // 加载阶段配置
        StageConfig config = plugin.getStageConfigManager().loadStageConfig(stageFile);
        if (config == null) return;

        // 设置玩家进度
        PlayerStageProgress progress = playerProgress.get(playerId);
        if (progress == null) {
            progress = new PlayerStageProgress(stageFile, 0);
            playerProgress.put(playerId, progress);
        } else {
            progress.stageFile = stageFile;
            progress.blocksBroken = 0;
        }

        plugin.debug("重置玩家 " + player.getName() + " 阶段: " + stageFile);

        // 发送阶段开始消息
        if (config.message != null && !config.message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.message));
            plugin.debug("发送阶段消息: " + config.message);
        }
    }

    /**
     * 检查玩家是否应该进入下一阶段
     */
    private void checkStageAdvancement(UUID playerId, PlayerStageProgress progress) {
        // 加载当前阶段配置
        StageConfig currentConfig = plugin.getStageConfigManager().loadStageConfig(progress.stageFile);
        if (currentConfig == null) return;

        // 检查是否达到阶段要求
        if (progress.blocksBroken >= currentConfig.amount) {
            // 切换到下一阶段
            String nextStage = currentConfig.nextStage;
            if (!nextStage.endsWith(".yml")) {
                nextStage += ".yml";
            }

            // 检查文件是否存在
            File nextStageFile = new File(plugin.getDataFolder(), "blocks/" + nextStage);
            if (!nextStageFile.exists()) {
                plugin.getLogger().warning("下一阶段配置文件不存在: " + nextStageFile.getAbsolutePath());
                return;
            }

            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                setPlayerStageAndSendMessage(player, nextStage);
            }
        }
    }

    /**
     * 获取玩家当前阶段的宝箱概率配置
     */
    public Map<String, Double> getChestChances(UUID playerId) {
        String stageFile = getCurrentStageFile(playerId);
        StageConfig config = plugin.getStageConfigManager().loadStageConfig(stageFile);
        return config != null ? config.chestChances : new HashMap<>();
    }

    /**
     * 设置玩家阶段（强制设置）
     *
     * @param player    玩家
     * @param stageFile 阶段配置文件
     */
    public void setPlayerStage(Player player, String stageFile) {
        if (player == null) {
            plugin.getLogger().warning("Cannot set stage for null player");
            return;
        }

        UUID playerId = player.getUniqueId();

        // 确保文件名有扩展名
        if (!stageFile.endsWith(".yml")) {
            stageFile += ".yml";
        }

        // 加载阶段配置
        StageConfig config = plugin.getStageConfigManager().loadStageConfig(stageFile);
        if (config == null) {
            plugin.getLogger().warning("无法加载阶段配置: " + stageFile);
            return;
        }

        // 设置或更新玩家进度
        PlayerStageProgress progress = playerProgress.get(playerId);
        if (progress == null) {
            progress = new PlayerStageProgress(stageFile, 0);
            playerProgress.put(playerId, progress);
        } else {
            progress.stageFile = stageFile;
            progress.blocksBroken = 0; // 重置破坏计数
        }

        plugin.debug("设置玩家 " + player.getName() + " 阶段: " + stageFile + " (重置计数)");

        // 发送阶段开始消息
        if (config.message != null && !config.message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.message));
            plugin.debug("发送阶段消息: " + config.message);
        }

        // 更新数据库
        plugin.getDatabaseManager().updatePlayerStage(playerId, stageFile, 0);
    }

    // 添加公共方法获取玩家进度
    public PlayerStageProgress getPlayerProgress(UUID playerId) {
        return playerProgress.get(playerId);
    }

    /**
     * 玩家阶段进度内部类
     */

    public static class PlayerStageProgress {
        public String stageFile;
        public int blocksBroken;

        public PlayerStageProgress(String stageFile, int blocksBroken) {
            this.stageFile = stageFile;
            this.blocksBroken = blocksBroken;
        }
    }
}