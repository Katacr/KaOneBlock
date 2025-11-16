package org.katacr.kaOneBlock;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class StageConfigManager {
    private final KaOneBlock plugin;
    private final Map<String, StageConfig> configCache = new HashMap<>();

    public StageConfigManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载阶段配置
     */
    public StageConfig loadStageConfig(String fileName) {
        // 确保文件名有扩展名
        if (!fileName.endsWith(".yml")) {
            fileName += ".yml";
        }

        // 检查缓存
        if (configCache.containsKey(fileName)) {
            return configCache.get(fileName);
        }

        File configFile = new File(plugin.getDataFolder(), "blocks/" + fileName);
        plugin.debug("加载阶段配置: " + configFile.getAbsolutePath());

        if (!configFile.exists()) {
            plugin.getLogger().warning("阶段配置文件不存在: " + configFile.getAbsolutePath());
            return null;
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(configFile);
        StageConfig config = new StageConfig();

        // 加载基本配置
        config.amount = yaml.getInt("amount", 500);

        // 安全获取 next 字段
        if (yaml.isString("next")) {
            config.nextStage = yaml.getString("next", "");
        } else {
            config.nextStage = "";
        }

        // 安全获取 message 字段
        if (yaml.isString("message")) {
            config.message = yaml.getString("message", "");
        } else {
            config.message = "";
        }

        // 加载宝箱概率
        ConfigurationSection chestsSection = yaml.getConfigurationSection("chests");
        if (chestsSection != null) {
            for (String chestKey : chestsSection.getKeys(false)) {
                if (chestsSection.isDouble(chestKey)) {
                    double chance = chestsSection.getDouble(chestKey, 0.0);
                    config.chestChances.put(chestKey, chance);
                }
            }
        }
        
        // 加载实体包配置
        if (yaml.isString("entity_pack")) {
            config.entityPack = yaml.getString("entity_pack", "");
        }
        
        // 加载实体生成概率
        if (yaml.isDouble("entity_chance")) {
            config.entityChance = yaml.getDouble("entity_chance", 0.05);
        }

        // 缓存配置
        configCache.put(fileName, config);
        plugin.debug("加载阶段配置: " + fileName);
        return config;
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        configCache.clear();
        plugin.debug("Cleared stage config cache");
    }
}