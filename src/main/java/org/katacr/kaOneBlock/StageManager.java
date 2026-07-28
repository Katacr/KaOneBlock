package org.katacr.kaOneBlock;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maintains in-memory player stage state and coordinates explicit persistence updates.
 */
public class StageManager {
    private final KaOneBlock plugin;
    private final Map<UUID, PlayerStageProgress> playerProgress = new HashMap<>();

    public StageManager(KaOneBlock plugin) {
        this.plugin = plugin;
        loadAllPlayerProgress();
    }

    /**
     * Hydrates the stage cache from the database's world-aware record cache.
     */
    public void loadAllPlayerProgress() {
        playerProgress.clear();
        playerProgress.putAll(plugin.getDatabaseManager().loadAllPlayerProgress());
    }

    /**
     * Initializes a player's stage from persisted state or the configured starting stage.
     */
    public PlayerStageProgress initPlayerProgress(Player player) {
        return playerProgress.computeIfAbsent(player.getUniqueId(), ignored ->
                new PlayerStageProgress(getStartingStageFile(), 0));
    }

    /**
     * Loads a joining player's cached stage without resetting an existing record.
     */
    public void onPlayerJoin(Player player) {
        initPlayerProgress(player);
    }

    /**
     * Increments progress, performs at most one stage transition and returns the resulting state.
     */
    public PlayerStageProgress incrementBlocksBroken(Player player) {
        PlayerStageProgress progress = initPlayerProgress(player);
        progress.blocksBroken++;
        plugin.debug("玩家 " + player.getName() + " 破坏方块数: " + progress.blocksBroken);
        checkStageAdvancement(player, progress);
        return progress;
    }

    /**
     * Returns the current stage filename, falling back to the configured starting stage.
     */
    public String getCurrentStageFile(UUID playerId) {
        PlayerStageProgress progress = playerProgress.get(playerId);
        return progress == null ? getStartingStageFile() : progress.stageFile;
    }

    /**
     * Returns the current stage configuration for a player when it is valid.
     */
    public StageConfig getCurrentStageConfig(UUID playerId) {
        return plugin.getStageConfigManager().loadStageConfig(getCurrentStageFile(playerId));
    }

    /**
     * Returns the current stage's immutable chest chance mapping.
     */
    public Map<String, Double> getChestChances(UUID playerId) {
        StageConfig config = getCurrentStageConfig(playerId);
        return config == null ? Map.of() : config.chestChances;
    }

    /**
     * Forcefully sets and persists a player's stage and sends its configured message.
     */
    public boolean setPlayerStage(Player player, String stageFile) {
        return setStage(player, stageFile, true, true);
    }

    /**
     * Resets and persists a player's stage for the administrative reset command.
     */
    public boolean resetPlayerStage(Player player, String stageFile) {
        return setStage(player, stageFile, true, true);
    }

    /**
     * Sends the current stage message without mutating or resetting progress.
     */
    public void sendCurrentStageMessage(Player player) {
        StageConfig config = getCurrentStageConfig(player.getUniqueId());
        if (config != null) {
            sendStageMessage(player, config);
        }
    }

    /**
     * Removes transient stage state after a player's OneBlock record is deleted.
     */
    public void clearPlayerProgress(UUID playerId) {
        playerProgress.remove(playerId);
    }

    /**
     * Restores a previous persisted snapshot when a scheduled world replacement cannot complete.
     */
    public void restoreProgress(UUID playerId, String stageFile, int blocksBroken) {
        playerProgress.put(playerId, new PlayerStageProgress(stageFile, blocksBroken));
    }

    /**
     * Returns the mutable main-thread stage state used by the gameplay pipeline.
     */
    public PlayerStageProgress getPlayerProgress(UUID playerId) {
        return playerProgress.get(playerId);
    }

    /**
     * Advances to a configured next stage and treats a blank next value as a terminal stage.
     */
    private void checkStageAdvancement(Player player, PlayerStageProgress progress) {
        StageConfig current = plugin.getStageConfigManager().loadStageConfig(progress.stageFile);
        if (current == null || progress.blocksBroken < current.amount || current.nextStage.isBlank()) {
            return;
        }

        String nextStage;
        try {
            nextStage = StageConfigManager.normalizeStageFile(current.nextStage);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("下一阶段文件名无效: " + current.nextStage);
            return;
        }
        StageConfig next = plugin.getStageConfigManager().loadStageConfig(nextStage);
        if (next == null) {
            plugin.getLogger().warning("下一阶段配置无效: " + nextStage);
            return;
        }

        progress.stageFile = nextStage;
        progress.blocksBroken = 0;
        sendStageMessage(player, next);
        plugin.debug("玩家 " + player.getName() + " 进入阶段: " + nextStage);
    }

    /**
     * Applies one validated stage mutation and optionally persists and announces it.
     */
    private boolean setStage(Player player, String stageFile, boolean sendMessage, boolean persist) {
        String normalized;
        try {
            normalized = StageConfigManager.normalizeStageFile(stageFile);
        } catch (IllegalArgumentException exception) {
            return false;
        }
        StageConfig config = plugin.getStageConfigManager().loadStageConfig(normalized);
        if (config == null) {
            return false;
        }

        PlayerStageProgress progress = playerProgress.computeIfAbsent(
                player.getUniqueId(),
                ignored -> new PlayerStageProgress(normalized, 0)
        );
        progress.stageFile = normalized;
        progress.blocksBroken = 0;

        if (persist) {
            plugin.getDatabaseManager().findBlockByPlayer(player.getUniqueId()).ifPresent(record ->
                    plugin.getDatabaseManager().updateState(player.getUniqueId(), record.blockType(), normalized, 0));
        }
        if (sendMessage) {
            sendStageMessage(player, config);
        }
        return true;
    }

    /**
     * Sends a translated stage message when the stage defines one.
     */
    private void sendStageMessage(Player player, StageConfig config) {
        if (config.message != null && !config.message.isBlank()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', config.message));
        }
    }

    /**
     * Resolves and normalizes the configured starting stage.
     */
    private String getStartingStageFile() {
        String configured = plugin.getConfig().getString("start-list", "normal");
        try {
            return StageConfigManager.normalizeStageFile(configured);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("start-list 配置无效，已回退到 normal.yml: " + configured);
            return "normal.yml";
        }
    }

    /**
     * Mutable main-thread state for a player's current stage and progress counter.
     */
    public static class PlayerStageProgress {
        private String stageFile;
        private int blocksBroken;

        public PlayerStageProgress(String stageFile, int blocksBroken) {
            this.stageFile = stageFile;
            this.blocksBroken = blocksBroken;
        }

        /**
         * Returns the normalized current stage filename.
         */
        public String stageFile() {
            return stageFile;
        }

        /**
         * Returns the number of blocks broken in the current stage.
         */
        public int blocksBroken() {
            return blocksBroken;
        }
    }
}
