package org.katacr.kaOneBlock;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class KaOneBlock extends JavaPlugin {
    private final Map<String, WeightedRandom<Material>> blockLists = new HashMap<>();
    private LanguageManager languageManager;
    private BlockGenerator blockGenerator;
    private DatabaseManager databaseManager;
    private Material defaultBlockMaterial; // 声明默认方块类型字段
    private boolean debugMode;

    @Override
    public void onEnable() {
        // 插件启动逻辑
        saveDefaultConfig();
        createLangDirectory();
        createBlockDirectory();

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

        // 加载默认方块类型
        loadDefaultBlockMaterial();

        // 加载方块列表
        loadBlockLists();


        // 加载配置
        reloadConfig();

        // 初始化调试模式
        debugMode = getConfig().getBoolean("debug", false);
        getLogger().info("Debug mode: " + (debugMode ? "enabled" : "disabled"));

        getLogger().info(languageManager.getMessage("plugin-enable"));
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

        getLogger().info(message);
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
        }
    }

    /**
     * 根据世界类型获取方块列表
     *
     * @param worldType 世界类型（normal, nether, the_end）
     * @return 权重随机选择器
     */
    public WeightedRandom<Material> getBlockList(String worldType) {
        return blockLists.get(worldType);
    }

    /**
     * 加载方块列表配置
     */
    private void loadBlockLists() {
        blockLists.clear();

        FileConfiguration config = getConfig();
        ConfigurationSection blockListSection = config.getConfigurationSection("block-list");
        if (blockListSection == null) return;

        for (String worldType : blockListSection.getKeys(false)) {
            String fileName = blockListSection.getString(worldType);
            if (fileName == null || fileName.isEmpty()) continue;

            File blockFile = new File(getDataFolder(), fileName);
            if (!blockFile.exists()) {
                // 如果文件不存在，尝试从资源中保存默认文件
                saveResource(fileName, false);
            }

            // 加载方块列表配置文件
            FileConfiguration blockConfig = YamlConfiguration.loadConfiguration(blockFile);
            ConfigurationSection blocksSection = blockConfig.getConfigurationSection("blocks");
            if (blocksSection == null) continue;

            // 创建权重随机选择器
            WeightedRandom<Material> weightedRandom = new WeightedRandom<>();
            for (String materialName : blocksSection.getKeys(false)) {
                try {
                    Material material = Material.valueOf(materialName.toUpperCase());
                    int weight = blocksSection.getInt(materialName, 1);
                    weightedRandom.add(material, weight);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid material name: " + materialName + " in " + fileName);
                }
            }

            blockLists.put(worldType, weightedRandom);
            getLogger().info("Loaded block list for " + worldType + ": " + fileName);
        }
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

    private void loadDefaultBlockMaterial() {
        String blockName = getConfig().getString("default-block", "OAK_LOG");
        try {
            this.defaultBlockMaterial = Material.valueOf(blockName.toUpperCase());
            getLogger().info("Default block set to: " + this.defaultBlockMaterial.name());
        } catch (IllegalArgumentException e) {
            this.defaultBlockMaterial = Material.OAK_LOG;
            getLogger().warning("Invalid block type '" + blockName + "' in config. Using default OAK_LOG.");
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        debugMode = getConfig().getBoolean("debug", false);
        getLogger().info("Debug mode: " + (debugMode ? "enabled" : "disabled"));
        languageManager.loadLanguageFiles();
        loadDefaultBlockMaterial();
        loadBlockLists();
    }

    // 检查是否启用调试模式
    public boolean isDebugEnabled() {
        return debugMode;
    }

    // 记录调试信息
    public void debug(String message) {
        if (debugMode) {
            getLogger().info("[DEBUG] " + message);
        }
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

    public Material getDefaultBlockMaterial() {
        return this.defaultBlockMaterial;
    }

    /**
     * 检查指定世界类型是否允许生成方块
     *
     * @param world 世界对象
     * @return 是否允许生成方块
     */
    public boolean isWorldAllowed(World world) {
        FileConfiguration config = getConfig();

        // 根据世界环境类型检查配置
        return switch (world.getEnvironment()) {
            case NORMAL -> config.getBoolean("world.normal", true);
            case NETHER -> config.getBoolean("world.nether", true);
            case THE_END -> config.getBoolean("world.the_end", true);
            default ->
                // 对于未知的世界类型，默认不允许
                    false;
        };
    }
}