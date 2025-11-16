package org.katacr.kaOneBlock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.katacr.kaOneBlock.chest.EnhancedChestManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

    // 位置信息工具方法
    public static Map<String, String> createDebugReplacements(Location location) {
        Map<String, String> replacements = new HashMap<>();
        if (location != null) {
            World world = location.getWorld();
            String worldName = world != null ? world.getName() : "未知世界";
            replacements.put("world", worldName);
            replacements.put("x", String.valueOf(location.getBlockX()));
            replacements.put("y", String.valueOf(location.getBlockY()));
            replacements.put("z", String.valueOf(location.getBlockZ()));
        }
        return replacements;
    }

    public static Map<String, String> createDebugReplacements(Location location, String blockType) {
        Map<String, String> replacements = createDebugReplacements(location);
        if (blockType != null) {
            replacements.put("block", blockType);
        }
        return replacements;
    }

    public static Map<String, String> createDebugReplacements(Location location, String blockType, String chestConfig) {
        Map<String, String> replacements = createDebugReplacements(location, blockType);
        if (chestConfig != null) {
            replacements.put("chestConfig", chestConfig);
        }
        return replacements;
    }

    @Override
    public void onEnable() {
        // 插件启动逻辑
        saveDefaultConfig(); // 保存默认配置（如果不存在）
        createLangDirectory();
        createBlockDirectory();
        createChestsDirectory(); // 创建宝箱目录
        createEntitysDirectory(); // 创建实体目录
        createEntitysDirectory(); // 创建实体目录
        createEntitysDirectory(); // 创建实体目录

        // 加载配置
        reloadConfig();

        // 初始化 ItemsAdder 管理器
        itemsAdderManager = new ItemsAdderManager(this);
        enhancedChestManager = new EnhancedChestManager(this);
        
        // 初始化实体管理器
        entityManager = new EntityManager(this);

        // 初始化日志管理器
        logManager = new LogManager(this);

        // 初始化调试模式
        debugMode = getConfig().getBoolean("debug", false);
        getLogger().info("Debug mode: " + (debugMode ? "enabled" : "disabled"));

        // 检查方块目录
        File blocksDir = new File(getDataFolder(), "blocks");
        if (!blocksDir.exists() || !blocksDir.isDirectory()) {
            getLogger().warning("方块目录不存在: " + blocksDir.getAbsolutePath());
        } else {
            File[] files = blocksDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                getLogger().info("加载方块目录中的文件: " + files.length);
                for (File file : files) {
                    getLogger().info(" - " + file.getName());
                }
            }
        }
        // 注册事件监听器
        BlockBreakListener blockBreakListener = new BlockBreakListener(this);
        getServer().getPluginManager().registerEvents(blockBreakListener, this);

        // 初始化管理器
        languageManager = new LanguageManager(this);
        languageManager.loadLanguageFiles();

        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 初始化阶段系统
        stageConfigManager = new StageConfigManager(this);
        stageManager = new StageManager(this);
        blockListManager = new BlockListManager(this);

        // 初始化方块生成器
        blockGenerator = new BlockGenerator(this);
        // 注册玩家登录监听器
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        // 初始化命令管理器
        CommandManager commandManager = new CommandManager(this);
        Objects.requireNonNull(this.getCommand("kaoneblock")).setExecutor(commandManager);
        Objects.requireNonNull(this.getCommand("kaoneblock")).setTabCompleter(commandManager);

        getLogger().info(languageManager.getMessage("plugin-enable"));
    }

    private void createBlockDirectory() {
        File blockDir = new File(getDataFolder(), "blocks");
        if (!blockDir.exists() && !blockDir.mkdirs()) {
            getLogger().warning("Failed to create block directory: " + blockDir.getAbsolutePath());
        } else {
            // 保存默认方块列表文件
            saveResource("blocks/normal.yml", false);
            saveResource("blocks/nether.yml", false);
            saveResource("blocks/end.yml", false);
            getLogger().info("Created block directory and saved default files");
        }
    }

    private void createChestsDirectory() {
        File chestsDir = new File(getDataFolder(), "chests");
        if (!chestsDir.exists() && !chestsDir.mkdirs()) {
            getLogger().warning("Failed to create chests directory: " + chestsDir.getAbsolutePath());
        } else {
            // 保存默认宝箱配置文件
            saveResource("chests/example_chest.yml", false);
            saveResource("chests/advanced_chest.yml", false);
            saveResource("chests/common_chest.yml", false);
            saveResource("chests/end_chest.yml", false);
            saveResource("chests/nether_chest.yml", false);
            getLogger().info("Created chests directory and saved default files");
        }
    }

    private void createEntitysDirectory() {
        File entitysDir = new File(getDataFolder(), "entitys");
        if (!entitysDir.exists() && !entitysDir.mkdirs()) {
            getLogger().warning("Failed to create entitys directory: " + entitysDir.getAbsolutePath());
        } else {
            // 保存默认实体配置文件
            saveResource("entitys/default.yml", false);
            getLogger().info("Created entitys directory and saved default files");
        }
    }

    @Override
    public void onDisable() {

        // 新增：从数据库加载玩家进度
        stageManager.loadAllPlayerProgress();

        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.close();
        }

        // 插件关闭逻辑
        getLogger().info(languageManager.getMessage("plugin-disable"));
    }

    @Override
    public void saveDefaultConfig() {
        super.saveDefaultConfig();
        // 确保语言目录存在
        createLangDirectory();
        // 保存默认语言文件
        saveResource("lang/lang_en_US.yml", false);
        saveResource("lang/lang_zh_CN.yml", false);
    }

    private void createLangDirectory() {
        File langDir = new File(getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            getLogger().warning("Failed to create language directory: " + langDir.getAbsolutePath());
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        debugMode = getConfig().getBoolean("debug", false);
        getLogger().info("Debug mode: " + (debugMode ? "enabled" : "disabled"));
        languageManager.loadLanguageFiles();

        // 重新加载阶段配置
        stageConfigManager.clearCache();
        blockListManager.clearCache();

        // 重载宝箱配置
        if (enhancedChestManager != null) {
            enhancedChestManager.loadChestConfigs();
        }

        // 更新日志状态
        boolean logEnabled = getConfig().getBoolean("log", true);
        logManager.setEnabled(logEnabled);
        getLogger().info("Logging " + (logEnabled ? "enabled" : "disabled"));

        getLogger().info(languageManager.getMessage("config-reloaded"));
    }

    // 记录调试信息
    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
    }

    public void debug(String key, Map<String, String> replacements) {
        if (!debugMode) return;

        // 获取原始消息
        String message = languageManager.getMessage(key);

        // 应用替换
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String placeholder = "%" + entry.getKey() + "%";
            message = message.replace(placeholder, entry.getValue());
        }

        // 去除颜色格式
        String cleanMessage = ChatColor.stripColor(message);
        debug(cleanMessage);
    }

    // 添加 getter 方法
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

    public String formatMaterialName(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }
}