package org.katacr.kaOneBlock;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class BlockListManager {
    private final KaOneBlock plugin;
    private final Map<String, WeightedRandom<Object>> blockLists = new HashMap<>();

    public BlockListManager(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * 获取指定阶段的方块列表
     */
    public WeightedRandom<Object> getBlockList(String stageFile) {
        // 确保文件名有扩展名
        if (!stageFile.endsWith(".yml")) {
            stageFile += ".yml";
        }

        // 检查缓存
        if (blockLists.containsKey(stageFile)) {
            return blockLists.get(stageFile);
        }

        File blockFile = new File(plugin.getDataFolder(), "blocks/" + stageFile);
        plugin.debug("加载方块列表: " + blockFile.getAbsolutePath());

        if (!blockFile.exists()) {
            plugin.getLogger().warning("方块列表文件不存在: " + blockFile.getAbsolutePath());

            // 列出可用文件
            File blocksDir = new File(plugin.getDataFolder(), "blocks");
            if (blocksDir.exists() && blocksDir.isDirectory()) {
                File[] files = blocksDir.listFiles((dir, name) -> name.endsWith(".yml"));
                if (files != null) {
                    StringBuilder availableFiles = new StringBuilder("可用方块列表文件: ");
                    for (File file : files) {
                        availableFiles.append(file.getName()).append(", ");
                    }
                    plugin.debug(availableFiles.toString());
                }
            }
            return null;
        }
        // 加载方块列表
        WeightedRandom<Object> blockList = loadBlockList(stageFile);
        if (blockList != null) {
            blockLists.put(stageFile, blockList);
        }
        return blockList;
    }

    private WeightedRandom<Object> loadBlockList(String fileName) {
        File blockFile = new File(plugin.getDataFolder(), "blocks/" + fileName);
        if (!blockFile.exists()) {
            plugin.getLogger().warning("Block list not found: " + fileName);
            return null;
        }

        FileConfiguration blockConfig = YamlConfiguration.loadConfiguration(blockFile);
        ConfigurationSection blocksSection = blockConfig.getConfigurationSection("blocks");
        if (blocksSection == null) {
            plugin.getLogger().warning("No 'blocks' section in file: " + fileName);
            return null;
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
                plugin.getLogger().info("Added ItemsAdder block to list: " + materialName + " (weight: " + weight + ")");
            } else {
                try {
                    // 尝试匹配原版方块（不区分大小写）
                    Material material = Material.matchMaterial(materialName);
                    if (material != null) {
                        int weight = blocksSection.getInt(materialName, 1);
                        weightedRandom.add(material, weight);
                        plugin.getLogger().info("Added material to list: " + material.name() + " (weight: " + weight + ")");
                    } else {
                        plugin.getLogger().warning("Invalid material name: " + materialName + " in " + fileName);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid material name: " + materialName + " in " + fileName);
                }
            }
        }

        plugin.debug("Loaded block list for stage: " + fileName);
        return weightedRandom;
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        blockLists.clear();
        plugin.debug("Cleared block list cache");
    }
}