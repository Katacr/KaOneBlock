package org.katacr.kaOneBlock;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates bundled user-editable YAML against the declared Spigot API baseline.
 */
class ResourceValidationTest {
    private static final Path RESOURCES = Path.of("src/main/resources");

    /**
     * Confirms every bundled chest entry has a supported material and valid range values.
     */
    @Test
    void validatesChestLootEntries() {
        File[] files = RESOURCES.resolve("chests").toFile().listFiles((directory, name) -> name.endsWith(".yml"));
        assertNotNull(files);
        int itemCount = 0;
        for (File file : files) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            validateItemRange(file, "amount", yaml.getInt("amount.min", 3), yaml.getInt("amount.max", 6));
            itemCount += validateItems(file, yaml.getConfigurationSection("items"));
            ConfigurationSection groups = yaml.getConfigurationSection("groups");
            if (groups != null) {
                for (String key : groups.getKeys(false)) {
                    validateItemRange(
                            file,
                            "groups." + key,
                            groups.getInt(key + ".min", 1),
                            groups.getInt(key + ".max", 1)
                    );
                    itemCount += validateItems(file, groups.getConfigurationSection(key + ".items"));
                }
            }
        }
        assertTrue(itemCount >= 797, "Expected the complete bundled loot catalog");
    }

    /**
     * Confirms stage block pools and entity packs reference supported enum values.
     */
    @Test
    void validatesStageAndEntityResources() {
        File[] blockFiles = RESOURCES.resolve("blocks").toFile().listFiles((directory, name) -> name.endsWith(".yml"));
        assertNotNull(blockFiles);
        for (File file : blockFiles) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            assertTrue(yaml.getInt("amount", 500) >= 1, file + " has invalid amount");
            validateChance(file, "entity_chance", yaml.getDouble("entity_chance", 0.05));
            String nextStage = yaml.getString("next", "");
            if (nextStage != null && !nextStage.isBlank()) {
                String normalized = StageConfigManager.normalizeStageFile(nextStage);
                assertTrue(RESOURCES.resolve("blocks").resolve(normalized).toFile().isFile(),
                        file + " references missing next stage " + nextStage);
            }
            String entityPack = yaml.getString("entity_pack", "");
            if (entityPack != null && !entityPack.isBlank()) {
                assertTrue(RESOURCES.resolve("entities").resolve(entityPack + ".yml").toFile().isFile(),
                        file + " references missing entity pack " + entityPack);
            }

            double totalChance = yaml.getDouble("entity_chance", 0.05);
            ConfigurationSection chests = yaml.getConfigurationSection("chests");
            if (chests != null) {
                for (String chestName : chests.getKeys(false)) {
                    double chance = chests.getDouble(chestName);
                    validateChance(file, "chests." + chestName, chance);
                    totalChance += chance;
                    assertTrue(RESOURCES.resolve("chests").resolve(chestName + ".yml").toFile().isFile(),
                            file + " references missing chest " + chestName);
                }
            }
            assertTrue(totalChance <= 1, file + " has event chances above 1");

            ConfigurationSection blocks = yaml.getConfigurationSection("blocks");
            assertNotNull(blocks, file + " has no blocks section");
            for (String materialName : blocks.getKeys(false)) {
                assertTrue(materialName.regionMatches(true, 0, "ia:", 0, 3) || Material.matchMaterial(materialName) != null,
                        file + " contains unsupported block " + materialName);
                double weight = blocks.getDouble(materialName);
                assertTrue(Double.isFinite(weight) && weight > 0, file + " block " + materialName + " has invalid weight");
            }
        }

        File[] entityFiles = RESOURCES.resolve("entities").toFile().listFiles((directory, name) -> name.endsWith(".yml"));
        assertNotNull(entityFiles);
        for (File file : entityFiles) {
            ConfigurationSection list = YamlConfiguration.loadConfiguration(file).getConfigurationSection("list");
            assertNotNull(list, file + " has no entity list");
            for (String key : list.getKeys(false)) {
                String type = list.getString(key + ".type");
                assertNotNull(type, file + " entity " + key + " has no type");
                assertTrue(EntityType.valueOf(type.toUpperCase()).isAlive(), file + " entity " + key + " is not living");
                assertTrue(list.getInt(key + ".weight", 10) > 0, file + " entity " + key + " has invalid weight");
                validateEquipment(file, key, list.getConfigurationSection(key + ".armors"));
            }
        }
    }

    /**
     * Confirms Chinese and English bundles expose the same message keys.
     */
    @Test
    void keepsLanguageKeysInSync() {
        YamlConfiguration chinese = YamlConfiguration.loadConfiguration(RESOURCES.resolve("lang/lang_zh_CN.yml").toFile());
        YamlConfiguration english = YamlConfiguration.loadConfiguration(RESOURCES.resolve("lang/lang_en_US.yml").toFile());
        assertEquals(chinese.getKeys(false), english.getKeys(false));
    }

    /**
     * Validates one chest item section and returns the number of entries checked.
     */
    private int validateItems(File file, ConfigurationSection items) {
        if (items == null) {
            return 0;
        }
        for (String key : items.getKeys(false)) {
            String path = key + ".";
            String materialName = items.getString(path + "material");
            assertNotNull(materialName, file + " item " + key + " has no material");
            assertTrue(materialName.regionMatches(true, 0, "ia:", 0, 3) || Material.matchMaterial(materialName) != null,
                    file + " item " + key + " has unsupported material " + materialName);
            int min = items.getInt(path + "min", 1);
            int max = items.getInt(path + "max", min);
            int slot = items.getInt(path + "slot", -1);
            double weight = items.getDouble(path + "weight", 1);
            assertTrue(min >= 1, file + " item " + key + " has invalid min");
            assertTrue(max >= min, file + " item " + key + " has invalid max");
            assertTrue(slot >= -1 && slot <= 26, file + " item " + key + " has invalid slot");
            assertTrue(Double.isFinite(weight) && weight > 0, file + " item " + key + " has invalid weight");
        }
        return items.getKeys(false).size();
    }

    /**
     * Confirms every configured entity equipment slot uses a supported material.
     */
    private void validateEquipment(File file, String entityKey, ConfigurationSection equipment) {
        if (equipment == null) {
            return;
        }
        Set<String> supportedSlots = Set.of("helmet", "chestplate", "leggings", "boots", "mainhand", "offhand");
        for (String slot : equipment.getKeys(false)) {
            assertTrue(supportedSlots.contains(slot), file + " entity " + entityKey + " has unsupported slot " + slot);
            String materialName = equipment.getString(slot);
            assertNotNull(materialName, file + " entity " + entityKey + " slot " + slot + " has no material");
            assertNotNull(Material.matchMaterial(materialName),
                    file + " entity " + entityKey + " has unsupported equipment " + materialName);
        }
    }

    /**
     * Confirms one configured chest item-count range fits a single chest inventory.
     */
    private void validateItemRange(File file, String path, int min, int max) {
        assertTrue(min >= 0 && min <= 27, file + " " + path + " has invalid min");
        assertTrue(max >= min && max <= 27, file + " " + path + " has invalid max");
    }

    /**
     * Confirms one bundled probability is finite and within the supported range.
     */
    private void validateChance(File file, String path, double chance) {
        assertTrue(Double.isFinite(chance) && chance >= 0 && chance <= 1,
                file + " " + path + " has invalid probability");
    }
}
