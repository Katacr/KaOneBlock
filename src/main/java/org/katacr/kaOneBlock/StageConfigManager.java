package org.katacr.kaOneBlock;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class StageConfigManager {
    private static final Pattern SAFE_STAGE_NAME = Pattern.compile("[A-Za-z0-9_-]+(?:\\.yml)?");
    private final KaOneBlock plugin;
    private final Map<String, StageConfig> configCache = new HashMap<>();

    public StageConfigManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * 加载阶段配置
     */
    public StageConfig loadStageConfig(String fileName) {
        try {
            fileName = normalizeStageFile(fileName);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("非法阶段文件名: " + fileName);
            return null;
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
        config.amount = Math.max(1, yaml.getInt("amount", 500));

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
                if (chestsSection.isInt(chestKey) || chestsSection.isDouble(chestKey)) {
                    double chance = normalizeChance(chestsSection.getDouble(chestKey, 0.0), "chests." + chestKey, fileName);
                    if (chance > 0) {
                        config.chestChances.put(chestKey, chance);
                    }
                }
            }
        }
        
        // 加载实体包配置
        if (yaml.isString("entity_pack")) {
            config.entityPack = yaml.getString("entity_pack", "");
        }
        
        // 加载实体生成概率
        if (yaml.isInt("entity_chance") || yaml.isDouble("entity_chance")) {
            config.entityChance = normalizeChance(yaml.getDouble("entity_chance", 0.05), "entity_chance", fileName);
        }
        if (config.entityPack == null || config.entityPack.isBlank()) {
            config.entityChance = 0;
        }

        double totalChance = config.entityChance + config.chestChances.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalChance > 1.0) {
            plugin.getLogger().warning("阶段 " + fileName + " 的实体和宝箱概率总和超过 1.0，超出部分不会生效");
        }

        config.chestChances = Collections.unmodifiableMap(config.chestChances);

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

    /**
     * Normalizes a safe stage basename and rejects traversal or absolute paths.
     */
    public static String normalizeStageFile(String fileName) {
        if (fileName == null || !SAFE_STAGE_NAME.matcher(fileName).matches()) {
            throw new IllegalArgumentException("Invalid stage filename");
        }
        return fileName.endsWith(".yml") ? fileName : fileName + ".yml";
    }

    /**
     * Clamps a configured probability and reports values outside the supported range.
     */
    private double normalizeChance(double chance, String path, String fileName) {
        if (!Double.isFinite(chance) || chance < 0 || chance > 1) {
            plugin.getLogger().warning("阶段 " + fileName + " 的概率 " + path + " 超出 0..1，已截断");
        }
        if (!Double.isFinite(chance)) {
            return 0;
        }
        return Math.max(0, Math.min(1, chance));
    }
}
