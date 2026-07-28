package org.katacr.kaOneBlock;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Loads weighted entity packs, applies configured equipment and spawns safe living entities.
 */
public class EntityManager {
    private final KaOneBlock plugin;
    private final Map<String, Map<String, EntityConfig>> entityPackCache = new LinkedHashMap<>();

    public EntityManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads and caches one entity pack from the canonical or legacy data directory.
     */
    public Map<String, EntityConfig> loadEntityPack(String packName) {
        if (packName == null || !packName.matches("[A-Za-z0-9_-]+")) {
            plugin.getLogger().warning("非法实体包名称: " + packName);
            return Map.of();
        }
        if (entityPackCache.containsKey(packName)) {
            return entityPackCache.get(packName);
        }

        File entityFile = new File(plugin.getDataFolder(), "entities/" + packName + ".yml");
        if (!entityFile.exists()) {
            File legacyFile = new File(plugin.getDataFolder(), "entitys/" + packName + ".yml");
            if (legacyFile.exists()) {
                entityFile = legacyFile;
                plugin.getLogger().warning("正在使用旧 entitys 目录，请迁移到 entities: " + legacyFile.getName());
            }
        }
        if (!entityFile.exists()) {
            plugin.getLogger().warning("实体包文件不存在: " + entityFile.getAbsolutePath());
            return Map.of();
        }

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(entityFile);
        Map<String, EntityConfig> configurations = new LinkedHashMap<>();
        ConfigurationSection list = yaml.getConfigurationSection("list");
        if (list != null) {
            for (String entityKey : list.getKeys(false)) {
                ConfigurationSection section = list.getConfigurationSection(entityKey);
                EntityConfig config = section == null ? null : parseEntity(entityKey, section);
                if (config != null) {
                    configurations.put(entityKey, config);
                }
            }
        }

        Map<String, EntityConfig> immutable = Map.copyOf(configurations);
        entityPackCache.put(packName, immutable);
        return immutable;
    }

    /**
     * Selects one entity from a pack according to positive configured weights.
     */
    public EntityConfig getRandomEntity(String packName) {
        Map<String, EntityConfig> configurations = loadEntityPack(packName);
        long totalWeight = configurations.values().stream().mapToLong(EntityConfig::weight).sum();
        if (totalWeight <= 0) {
            return null;
        }

        long roll = ThreadLocalRandom.current().nextLong(totalWeight);
        long cumulative = 0;
        for (EntityConfig config : configurations.values()) {
            cumulative += config.weight();
            if (roll < cumulative) {
                return config;
            }
        }
        return null;
    }

    /**
     * Spawns one configured living entity and applies its name and equipment.
     */
    public LivingEntity spawnEntity(Location location, String packName) {
        EntityConfig config = getRandomEntity(packName);
        World world = location.getWorld();
        if (config == null || world == null) {
            return null;
        }

        try {
            if (!(world.spawnEntity(location, config.type()) instanceof LivingEntity entity)) {
                return null;
            }
            if (!config.nameTag().isBlank()) {
                entity.setCustomName(ChatColor.translateAlternateColorCodes('&', config.nameTag()));
                entity.setCustomNameVisible(true);
            }
            applyEquipment(entity.getEquipment(), config.equipment());
            return entity;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "生成实体失败: " + packName, exception);
            return null;
        }
    }

    /**
     * Clears cached entity packs so edited files are read on the next generation.
     */
    public void clearCache() {
        entityPackCache.clear();
    }

    /**
     * Parses and validates one entity entry from YAML.
     */
    private EntityConfig parseEntity(String key, ConfigurationSection section) {
        String typeName = section.getString("type", "ZOMBIE");
        try {
            EntityType type = EntityType.valueOf(typeName == null ? "ZOMBIE" : typeName.toUpperCase());
            if (!type.isAlive()) {
                plugin.getLogger().warning("实体类型不是生物: " + typeName);
                return null;
            }
            String nameTag = section.getString("name", "");
            int weight = Math.max(0, section.getInt("weight", 10));
            return new EntityConfig(key, nameTag == null ? "" : nameTag, type, weight, parseEquipment(section));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("未知实体类型: " + typeName);
            return null;
        }
    }

    /**
     * Parses supported armor and hand slots while ignoring invalid materials safely.
     */
    private Map<EquipmentSlot, Material> parseEquipment(ConfigurationSection entitySection) {
        ConfigurationSection armor = entitySection.getConfigurationSection("armors");
        if (armor == null) {
            return Map.of();
        }

        Map<EquipmentSlot, Material> equipment = new EnumMap<>(EquipmentSlot.class);
        Map<String, EquipmentSlot> slots = Map.of(
                "helmet", EquipmentSlot.HEAD,
                "chestplate", EquipmentSlot.CHEST,
                "leggings", EquipmentSlot.LEGS,
                "boots", EquipmentSlot.FEET,
                "mainhand", EquipmentSlot.HAND,
                "offhand", EquipmentSlot.OFF_HAND
        );
        slots.forEach((path, slot) -> {
            String materialName = armor.getString(path);
            Material material = materialName == null ? null : Material.matchMaterial(materialName);
            if (material != null && material != Material.AIR) {
                equipment.put(slot, material);
            } else if (materialName != null) {
                plugin.getLogger().warning("无效实体装备: " + materialName);
            }
        });
        return Map.copyOf(equipment);
    }

    /**
     * Applies parsed equipment to a living entity when its type supports equipment.
     */
    private void applyEquipment(EntityEquipment target, Map<EquipmentSlot, Material> equipment) {
        if (target == null) {
            return;
        }
        equipment.forEach((slot, material) -> target.setItem(slot, new ItemStack(material)));
    }

    /**
     * Immutable validated entity configuration used by weighted selection.
     */
    public record EntityConfig(
            String name,
            String nameTag,
            EntityType type,
            int weight,
            Map<EquipmentSlot, Material> equipment
    ) {
    }
}
