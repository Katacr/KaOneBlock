package org.katacr.kaOneBlock;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class KaOneBlock extends JavaPlugin {
    private LanguageManager languageManager;
    private BlockGenerator blockGenerator;
    private DatabaseManager databaseManager;
    private Material defaultBlockMaterial; // 声明默认方块类型字段

    @Override
    public void onEnable() {
        // 插件启动逻辑
        saveDefaultConfig();
        createLangDirectory();

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

        getLogger().info(languageManager.getMessage("plugin-enable"));
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
        languageManager.loadLanguageFiles();
        loadDefaultBlockMaterial(); // 重载时重新加载方块类型
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