package org.katacr.kaOneBlock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.katacr.kaOneBlock.chest.EnhancedChestManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Boots and coordinates the KaOneBlock gameplay, persistence and optional integrations.
 */
public final class KaOneBlock extends JavaPlugin {
    boolean debugMode;
    private StageManager stageManager;
    private StageConfigManager stageConfigManager;
    private BlockListManager blockListManager;
    private LanguageManager languageManager;
    private BlockGenerator blockGenerator;
    private DatabaseManager databaseManager;
    private EnhancedChestManager enhancedChestManager;
    private LogManager logManager;
    private ItemsAdderManager itemsAdderManager;
    private EntityManager entityManager;

    /**
     * Creates common debug placeholders for a location.
     */
    public static Map<String, String> createDebugReplacements(Location location) {
        Map<String, String> replacements = new HashMap<>();
        if (location != null) {
            World world = location.getWorld();
            replacements.put("world", world == null ? "unknown" : world.getName());
            replacements.put("x", String.valueOf(location.getBlockX()));
            replacements.put("y", String.valueOf(location.getBlockY()));
            replacements.put("z", String.valueOf(location.getBlockZ()));
            replacements.put("location", String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        }
        return replacements;
    }

    /**
     * Adds a generated block identifier to location debug placeholders.
     */
    public static Map<String, String> createDebugReplacements(Location location, String blockType) {
        Map<String, String> replacements = createDebugReplacements(location);
        if (blockType != null) {
            replacements.put("block", blockType);
        }
        return replacements;
    }

    /**
     * Adds a chest configuration identifier to location and block debug placeholders.
     */
    public static Map<String, String> createDebugReplacements(Location location, String blockType, String chestConfig) {
        Map<String, String> replacements = createDebugReplacements(location, blockType);
        if (chestConfig != null) {
            replacements.put("chestConfig", chestConfig);
        }
        return replacements;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledResources();
        reloadConfig();

        languageManager = new LanguageManager(this);
        languageManager.loadLanguageFiles();
        debugMode = getConfig().getBoolean("debug", false);
        logManager = new LogManager(this);

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.initialize()) {
            getLogger().severe(languageManager.getMessage("database-init-failed"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        stageConfigManager = new StageConfigManager(this);
        blockListManager = new BlockListManager(this);
        stageManager = new StageManager(this);
        itemsAdderManager = new ItemsAdderManager(this);
        enhancedChestManager = new EnhancedChestManager(this);
        entityManager = new EntityManager(this);
        blockGenerator = new BlockGenerator(this);

        getServer().getPluginManager().registerEvents(new BlockBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        PluginCommand command = getCommand("kaoneblock");
        if (command == null) {
            throw new IllegalStateException("kaoneblock command is missing from plugin.yml");
        }
        CommandManager commandManager = new CommandManager(this);
        command.setExecutor(commandManager);
        command.setTabCompleter(commandManager);
        getLogger().info(languageManager.getMessage("plugin-enable"));
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (logManager != null) {
            logManager.close();
        }
        if (languageManager != null) {
            getLogger().info(languageManager.getMessage("plugin-disable"));
        }
    }

    /**
     * Reloads mutable configuration and all file-backed caches without resetting player progress.
     */
    public void reloadPlugin() {
        reloadConfig();
        debugMode = getConfig().getBoolean("debug", false);
        languageManager.loadLanguageFiles();
        stageConfigManager.clearCache();
        blockListManager.clearCache();
        entityManager.clearCache();
        enhancedChestManager.loadChestConfigs();
        logManager.setEnabled(getConfig().getBoolean("log", true));
        getLogger().info(languageManager.getMessage("config-reloaded"));
    }

    /**
     * Writes a raw debug message only when debug mode is enabled.
     */
    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    /**
     * Resolves and formats a localized debug message only when debug mode is enabled.
     */
    public void debug(String key, Map<String, String> replacements) {
        if (!debugMode || languageManager == null) {
            return;
        }
        debug(ChatColor.stripColor(languageManager.getMessage(key, replacements)));
    }

    /**
     * Saves all editable bundled YAML resources without overwriting server customizations.
     */
    private void saveBundledResources() {
        saveResourceGroup("lang", "lang_en_US.yml", "lang_zh_CN.yml");
        saveResourceGroup("blocks", "normal.yml", "nether.yml", "end.yml");
        saveResourceGroup("chests", "example_chest.yml", "advanced_chest.yml", "common_chest.yml", "end_chest.yml", "nether_chest.yml");
        saveResourceGroup("entities", "normal_entity.yml", "nether_entity.yml", "end_entity.yml");
    }

    /**
     * Creates one data subdirectory and copies the named bundled resources into it.
     */
    private void saveResourceGroup(String directory, String... resourceNames) {
        File targetDirectory = new File(getDataFolder(), directory);
        if (!targetDirectory.exists() && !targetDirectory.mkdirs()) {
            throw new IllegalStateException("Cannot create resource directory " + targetDirectory);
        }
        for (String resourceName : resourceNames) {
            saveResource(directory + "/" + resourceName, false);
        }
    }

    public StageManager getStageManager() {
        return stageManager;
    }

    public StageConfigManager getStageConfigManager() {
        return stageConfigManager;
    }

    public BlockListManager getBlockListManager() {
        return blockListManager;
    }

    public EnhancedChestManager getEnhancedChestManager() {
        return enhancedChestManager;
    }

    public LogManager getLogManager() {
        return logManager;
    }

    public ItemsAdderManager getItemsAdderManager() {
        return itemsAdderManager;
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    public boolean isDebugEnabled() {
        return debugMode;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public BlockGenerator getBlockGenerator() {
        return blockGenerator;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public boolean isItemsAdderEnabled() {
        return itemsAdderManager != null && itemsAdderManager.isEnabled();
    }

    public boolean isItemsAdderLoaded() {
        return itemsAdderManager != null && itemsAdderManager.isLoaded();
    }

    /**
     * Formats an enum material identifier for concise player-facing output.
     */
    public String formatMaterialName(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }
}
