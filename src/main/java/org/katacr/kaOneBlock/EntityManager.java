package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

public class EntityManager {
    private final KaOneBlock plugin;
    private final Map<String, Map<String, EntityConfig>> entityPackCache = new HashMap<>();
    private final Random random = new Random();

    public EntityManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    public static class EntityConfig {
        public String name;
        public String nameTag;
        public EntityType type;
        public int weight;

        public EntityConfig(String name, String nameTag, EntityType type, int weight) {
            this.name = name;
            this.nameTag = nameTag;
            this.type = type;
            this.weight = weight;
        }
    }

    public Map<String, EntityConfig> loadEntityPack(String packName) {
        if (entityPackCache.containsKey(packName)) {
            return entityPackCache.get(packName);
        }

        File entityFile = new File(plugin.getDataFolder(), "entitys/" + packName + ".yml");
        plugin.debug("加载实体包: " + entityFile.getAbsolutePath());

        if (!entityFile.exists()) {
            plugin.getLogger().warning("实体包文件不存在: " + entityFile.getAbsolutePath());
            return new HashMap<>();
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(entityFile);
        Map<String, EntityConfig> entityConfigs = new HashMap<>();

        ConfigurationSection listSection = yaml.getConfigurationSection("list");
        if (listSection != null) {
            for (String entityKey : listSection.getKeys(false)) {
                ConfigurationSection entitySection = listSection.getConfigurationSection(entityKey);
                if (entitySection != null) {
                    try {
                        String name = entityKey;
                        String nameTag = entitySection.getString("name", "");
                        String typeName = entitySection.getString("type", "ZOMBIE");
                        EntityType type;
                        try {
                            type = EntityType.valueOf(typeName.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("未知的实体类型: " + typeName + ", 默认使用ZOMBIE");
                            type = EntityType.ZOMBIE;
                        }
                        int weight = entitySection.getInt("weight", 10);

                        EntityConfig config = new EntityConfig(name, nameTag, type, weight);
                        entityConfigs.put(name, config);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "加载实体配置失败: " + entityKey, e);
                    }
                }
            }
        }

        entityPackCache.put(packName, entityConfigs);
        plugin.debug("加载实体包: " + packName + ", 包含 " + entityConfigs.size() + " 个实体");
        return entityConfigs;
    }

    public EntityConfig getRandomEntity(String packName) {
        Map<String, EntityConfig> entityConfigs = loadEntityPack(packName);
        if (entityConfigs.isEmpty()) {
            return null;
        }

        int totalWeight = 0;
        for (EntityConfig config : entityConfigs.values()) {
            totalWeight += config.weight;
        }

        if (totalWeight <= 0) {
            return null;
        }

        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;

        for (EntityConfig config : entityConfigs.values()) {
            currentWeight += config.weight;
            if (randomWeight < currentWeight) {
                return config;
            }
        }

        return entityConfigs.values().iterator().next();
    }

    public LivingEntity spawnEntity(Location location, String packName) {
        EntityConfig config = getRandomEntity(packName);
        if (config == null) {
            plugin.getLogger().warning("无法获取实体配置: " + packName);
            return null;
        }

        try {
            World world = location.getWorld();
            if (world == null) {
                return null;
            }

            LivingEntity entity = (LivingEntity) world.spawnEntity(location, config.type);
            if (entity == null) {
                return null;
            }

            if (config.nameTag != null && !config.nameTag.isEmpty()) {
                entity.setCustomName(config.nameTag);
                entity.setCustomNameVisible(true);
            }

            plugin.debug("生成实体: " + config.name + " 在位置 " +
                    location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
            return entity;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "生成实体失败: " + packName, e);
            return null;
        }
    }

    public void clearCache() {
        entityPackCache.clear();
        plugin.debug("Cleared entity pack cache");
    }
}