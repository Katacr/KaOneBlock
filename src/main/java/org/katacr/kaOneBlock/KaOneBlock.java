package org.katacr.kaOneBlock;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.katacr.kaOneBlock.chest.EnhancedChestManager;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class KaOneBlock extends JavaPlugin {
    private final Map<String, WeightedRandom<Object>> worldBlockLists = new HashMap<>();
    boolean debugMode;
    private LanguageManager languageManager;
    private BlockGenerator blockGenerator;
    private DatabaseManager databaseManager;
    private EnhancedChestManager enhancedChestManager;
    private LogManager logManager;
    private ItemsAdderManager itemsAdderManager;

    @Override
    public void onEnable() {
        // 插件启动逻辑
        saveDefaultConfig(); // 保存默认配置（如果不存在）
        createLangDirectory();
        createBlockDirectory();
        createChestsDirectory(); // 创建宝箱目录

        // 加载配置（重要：在初始化管理器之前加载）
        reloadConfig();

        // 初始化 ItemsAdder 管理器
        itemsAdderManager = new ItemsAdderManager(this);

        enhancedChestManager = new EnhancedChestManager(this);

        // 初始化日志管理器
        logManager = new LogManager(this);

        // 初始化调试模式
        debugMode = getConfig().getBoolean("debug", false);
        getLogger().info("Debug mode: " + (debugMode ? "enabled" : "disabled"));

        // 注册事件监听器
        BlockBreakListener blockBreakListener = new BlockBreakListener(this);
        getServer().getPluginManager().registerEvents(blockBreakListener, this);

        // 初始化管理器
        languageManager = new LanguageManager(this);
        languageManager.loadLanguageFiles();

        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // 初始化方块生成器
        blockGenerator = new BlockGenerator(this);


        // 初始化命令管理器
        CommandManager commandManager = new CommandManager(this);
        Objects.requireNonNull(this.getCommand("kaoneblock")).setExecutor(commandManager);
        Objects.requireNonNull(this.getCommand("kaoneblock")).setTabCompleter(commandManager);

        getLogger().info(languageManager.getMessage("plugin-enable"));

        // 加载方块列表
        loadBlockLists();
    }

    /**
     * 记录调试信息
     *
     * @param key          消息键
     * @param replacements 替换变量
     */
    public void debug(String key, Map<String, String> replacements) {
        if (!debugMode) return;

        // 获取原始消息
        String message = languageManager.getMessage(key);

        // 应用替换
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }

        // 去除颜色格式
        String cleanMessage = ChatColor.stripColor(message);
        getLogger().info("[DEBUG] " + cleanMessage);
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
            saveResource("chests/advanced_chest.yml", false);
            getLogger().info("Created chests directory and saved default files");
        }
    }

    /**
     * 加载方块列表配置
     */
    private void loadBlockLists() {
        worldBlockLists.clear();
        getLogger().info("Loading block lists...");

        FileConfiguration config = getConfig();
        ConfigurationSection blockListSection = config.getConfigurationSection("block-list");
        if (blockListSection == null) {
            getLogger().warning("No 'block-list' section found in config.yml");
            return;
        }

        getLogger().info("Found block-list section with keys: " + blockListSection.getKeys(false));

        for (String key : blockListSection.getKeys(false)) {
            // 添加 .yml 扩展名
            String fileName = key.endsWith(".yml") ? key : key + ".yml";
            getLogger().info("Processing block list file: " + fileName);

            // 获取文件配置节
            ConfigurationSection fileSection = blockListSection.getConfigurationSection(key);
            if (fileSection == null) {
                getLogger().warning("Invalid configuration for block list key: " + key);
                continue;
            }

            // 获取世界列表
            List<String> worlds = fileSection.getStringList("worlds");
            if (worlds.isEmpty()) {
                getLogger().warning("No worlds defined for block list key: " + key);
                continue;
            }

            getLogger().info("Worlds for " + key + ": " + worlds);

            // 构建完整文件路径（添加 blocks/ 目录）
            String fullFilePath = "blocks/" + fileName;
            File blockFile = new File(getDataFolder(), fullFilePath);
            getLogger().info("Block file path: " + blockFile.getAbsolutePath());

            if (!blockFile.exists()) {
                // 如果文件不存在，尝试从资源中保存默认文件
                getLogger().info("File does not exist, saving default: " + fullFilePath);
                saveResource(fullFilePath, false);
            }

            // 加载方块列表配置文件
            FileConfiguration blockConfig = YamlConfiguration.loadConfiguration(blockFile);
            ConfigurationSection blocksSection = blockConfig.getConfigurationSection("blocks");
            if (blocksSection == null) {
                getLogger().warning("No 'blocks' section in file: " + fullFilePath);
                continue;
            }

            // 创建权重随机选择器
            WeightedRandom<Object> weightedRandom = new WeightedRandom<>();
            for (String materialName : blocksSection.getKeys(false)) {
                // 统一转换为小写进行检查
                String normalizedName = materialName.toLowerCase();

                if (normalizedName.startsWith("ia:")) {
                    // ItemsAdder 方块 - 使用原始名称（包含命名空间）
                    int weight = blocksSection.getInt(materialName, 1);
                    weightedRandom.add(materialName, weight);
                    getLogger().info("Added ItemsAdder block to list: " + materialName + " (weight: " + weight + ")");
                } else {
                    try {
                        // 尝试匹配原版方块（不区分大小写）
                        Material material = Material.matchMaterial(materialName);
                        if (material != null) {
                            int weight = blocksSection.getInt(materialName, 1);
                            weightedRandom.add(material, weight);
                            getLogger().info("Added material to list: " + material.name() + " (weight: " + weight + ")");
                        } else {
                            getLogger().warning("Invalid material name: " + materialName + " in " + fullFilePath);
                        }
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid material name: " + materialName + " in " + fullFilePath);
                    }
                }
            }

            // 将方块列表与对应世界关联
            for (String worldName : worlds) {
                worldBlockLists.put(worldName, weightedRandom);
                getLogger().info("Mapped world '" + worldName + "' to block list: " + fileName);
            }
        }

        // 记录加载的世界列表
        getLogger().info("Loaded worlds: " + worldBlockLists.keySet());
    }

    @Override
    public void onDisable() {
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
        loadBlockLists();

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

    // 添加 getter 方法
    public EnhancedChestManager getEnhancedChestManager() {
        return enhancedChestManager;
    }

    // Getter 方法
    public LogManager getLogManager() {
        return logManager;
    }

    // Getter 方法
    public ItemsAdderManager getItemsAdderManager() {
        return itemsAdderManager;
    }

    public boolean isDebugEnabled() {
        return debugMode;
    }

    // Getter 方法
    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public BlockGenerator getBlockGenerator() {
        return blockGenerator;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    /**
     * 检查 ItemsAdder 是否可用
     *
     * @return 是否可用
     */
    public boolean isItemsAdderEnabled() {
        return itemsAdderManager != null && itemsAdderManager.isEnabled();
    }

    /**
     * 检查 ItemsAdder 数据是否已加载
     *
     * @return 是否已加载
     */
    public boolean isItemsAdderLoaded() {
        return itemsAdderManager != null && itemsAdderManager.isLoaded();
    }

    public String formatMaterialName(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }

    /**
     * 根据世界名称获取方块列表
     *
     * @param worldName 世界名称
     * @return 权重随机选择器
     */

    public WeightedRandom<Object> getBlockListForWorld(String worldName) {
        return worldBlockLists.get(worldName);
    }

    /**
     * 检查世界是否允许生成方块
     *
     * @param worldName 世界名称
     * @return 是否允许生成方块
     */
    public boolean isWorldAllowed(String worldName) {
        return worldBlockLists.containsKey(worldName);
    }

}