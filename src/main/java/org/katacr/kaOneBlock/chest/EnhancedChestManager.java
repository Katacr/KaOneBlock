package org.katacr.kaOneBlock.chest;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.katacr.kaOneBlock.KaOneBlock;
import org.katacr.kaOneBlock.WeightedRandom;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

public class EnhancedChestManager {
    private final KaOneBlock plugin;
    private final Map<String, ChestConfig> chestConfigs = new HashMap<>();
    private final Random random = new Random();

    public EnhancedChestManager(KaOneBlock plugin) {
        this.plugin = plugin;
        loadChestConfigs();
    }

    public void loadChestConfigs() {
        chestConfigs.clear();
        File chestsDir = new File(plugin.getDataFolder(), "chests");

        if (chestsDir.exists() && chestsDir.isDirectory()) {
            File[] chestFiles = chestsDir.listFiles((dir, name) -> name.endsWith(".yml"));

            if (chestFiles != null) {
                for (File chestFile : chestFiles) {
                    try {
                        String fileName = chestFile.getName().replace(".yml", "");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(chestFile);

                        ChestConfig chestConfig = new ChestConfig();
                        chestConfig.name = config.getString("name", "宝箱");

                        // 加载全局物品数量设置
                        if (config.isConfigurationSection("amount")) {
                            ConfigurationSection amountSection = config.getConfigurationSection("amount");
                            if (amountSection != null) {
                                chestConfig.globalMinItems = amountSection.getInt("min", 3);
                            }
                            if (amountSection != null) {
                                chestConfig.globalMaxItems = amountSection.getInt("max", 6);
                            }
                        } else {
                            // 默认值
                            chestConfig.globalMinItems = 3;
                            chestConfig.globalMaxItems = 6;
                        }

                        // 加载物品组
                        ConfigurationSection groupsSection = config.getConfigurationSection("groups");
                        if (groupsSection != null) {
                            for (String groupKey : groupsSection.getKeys(false)) {
                                ConfigurationSection groupSection = groupsSection.getConfigurationSection(groupKey);
                                if (groupSection != null) {
                                    ChestGroup group = new ChestGroup();
                                    group.minItems = groupSection.getInt("min", chestConfig.globalMinItems);
                                    group.maxItems = groupSection.getInt("max", chestConfig.globalMaxItems);

                                    // 加载组内物品
                                    ConfigurationSection itemsSection = groupSection.getConfigurationSection("items");
                                    if (itemsSection != null) {
                                        for (String itemKey : itemsSection.getKeys(false)) {
                                            ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                                            if (itemSection != null) {
                                                ContainerItem containerItem = parseContainerItem(itemSection);
                                                if (containerItem != null) {
                                                    double weight = itemSection.getDouble("weight", 1.0);
                                                    group.weightedItems.add(containerItem, weight);
                                                }
                                            }
                                        }
                                    }
                                    chestConfig.groups.put(groupKey, group);
                                }
                            }
                        }

                        // 加载全局物品
                        ConfigurationSection itemsSection = config.getConfigurationSection("items");
                        if (itemsSection != null) {
                            ChestGroup globalGroup = new ChestGroup();
                            globalGroup.minItems = chestConfig.globalMinItems;
                            globalGroup.maxItems = chestConfig.globalMaxItems;

                            for (String itemKey : itemsSection.getKeys(false)) {
                                ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);
                                if (itemSection != null) {
                                    ContainerItem containerItem = parseContainerItem(itemSection);
                                    if (containerItem != null) {
                                        double weight = itemSection.getDouble("weight", 1.0);
                                        globalGroup.weightedItems.add(containerItem, weight);
                                    }
                                }
                            }
                            chestConfig.groups.put("global", globalGroup);
                        }

                        chestConfigs.put(fileName, chestConfig);
                        plugin.getLogger().info("Loaded enhanced chest config: " + fileName + " with " + chestConfig.groups.size() + " groups");
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to load enhanced chest config: " + chestFile.getName(), e);
                    }
                }
            }
        }

        if (chestConfigs.isEmpty()) {
            plugin.getLogger().warning("No enhanced chest configs found! Using fallback config.");
            createFallbackConfig();
        }
    }

    private ContainerItem parseContainerItem(ConfigurationSection itemSection) {
        String materialName = itemSection.getString("material");
        if (materialName == null || materialName.isEmpty()) {
            String itemPath = itemSection.getCurrentPath();
            plugin.getLogger().warning("Missing material in item configuration at: " + itemPath);
            plugin.getLogger().warning("Item configuration: " + itemSection.getValues(false));
            return null;
        }

        // 处理ItemsAdder自定义物品
        ItemStack itemStack;
        if (materialName.startsWith("IA:")) {
            // ItemsAdder自定义物品
            String customBlockId = materialName.substring(3);
            if (plugin.getItemsAdderManager().isEnabled() && plugin.getItemsAdderManager().isLoaded()) {
                itemStack = plugin.getItemsAdderManager().getCustomBlockItem(customBlockId);
                if (itemStack == null) {
                    plugin.getLogger().warning("Invalid ItemsAdder block: " + customBlockId);
                    return null;
                }
            } else {
                plugin.getLogger().warning("ItemsAdder not available for block: " + customBlockId);
                return null;
            }
        } else {
            // 原版物品
            Material material = Material.matchMaterial(materialName);
            if (material == null || material == Material.AIR) {
                plugin.getLogger().warning("Invalid material: " + materialName);
                return null;
            }
            itemStack = new ItemStack(material);
        }

        // 应用自定义名称和描述
        if (itemSection.isString("name")) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', Objects.requireNonNull(itemSection.getString("name"))));
                itemStack.setItemMeta(meta);
            }
        }

        if (itemSection.isList("lore")) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                for (String line : itemSection.getStringList("lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', line));
                }
                meta.setLore(lore);
                itemStack.setItemMeta(meta);
            }
        }

        // 应用附魔
        if (itemSection.isConfigurationSection("enchantments")) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta != null) {
                ConfigurationSection enchantsSection = itemSection.getConfigurationSection("enchantments");
                if (enchantsSection != null) {
                    for (String enchantName : enchantsSection.getKeys(false)) {
                        // 使用推荐的方法获取附魔
                        Enchantment enchant;

                        // 首先尝试使用命名空间键获取
                        try {
                            NamespacedKey key = NamespacedKey.minecraft(enchantName.toLowerCase());
                            enchant = Enchantment.getByKey(key);
                        } catch (Exception e) {
                            // 回退到弃用方法
                            enchant = Enchantment.getByName(enchantName.toUpperCase());
                            if (enchant != null) {
                                plugin.getLogger().warning("Using deprecated method to get enchantment: " + enchantName);
                            }
                        }

                        if (enchant != null) {
                            int level = enchantsSection.getInt(enchantName, 1);
                            meta.addEnchant(enchant, level, true);
                        } else {
                            plugin.getLogger().warning("Unknown enchantment: " + enchantName);
                        }
                    }
                }
                itemStack.setItemMeta(meta);
            }
        }

        int slot = itemSection.getInt("slot", -1);
        int min = itemSection.getInt("min", 1);
        int max = itemSection.getInt("max", min);

        return new ContainerItem(plugin, itemStack, slot, min, max);
    }

    private void createFallbackConfig() {
        ChestConfig fallback = new ChestConfig();
        fallback.name = "&6默认宝箱";
        fallback.globalMinItems = 3;
        fallback.globalMaxItems = 6;

        // 添加全局组
        ChestGroup globalGroup = new ChestGroup();
        globalGroup.minItems = fallback.globalMinItems;
        globalGroup.maxItems = fallback.globalMaxItems;

        // 添加默认物品
        globalGroup.weightedItems.add(new ContainerItem(plugin, new ItemStack(Material.STONE), -1, 1, 5), 10);
        globalGroup.weightedItems.add(new ContainerItem(plugin, new ItemStack(Material.DIRT), -1, 3, 7), 8);
        globalGroup.weightedItems.add(new ContainerItem(plugin, new ItemStack(Material.COAL), -1, 1, 3), 5);
        globalGroup.weightedItems.add(new ContainerItem(plugin, new ItemStack(Material.IRON_INGOT), -1, 1, 1), 3);
        globalGroup.weightedItems.add(new ContainerItem(plugin, new ItemStack(Material.GOLD_INGOT), -1, 1, 1), 2);
        globalGroup.weightedItems.add(new ContainerItem(plugin, new ItemStack(Material.DIAMOND), -1, 1, 1), 1);

        fallback.groups.put("global", globalGroup);

        chestConfigs.put("fallback", fallback);
    }

    public void fillChest(org.bukkit.block.Chest chest, String chestConfigName) {
        ChestConfig config = chestConfigs.get(chestConfigName);


        if (chest == null) {
            plugin.debug("Cannot fill chest: chest is null");
            return;
        }
        if (config == null) {
            plugin.getLogger().warning("Enhanced chest config '" + chestConfigName + "' not found!");
            config = chestConfigs.get("fallback");
            if (config == null) {
                plugin.getLogger().severe("Fallback enhanced chest config also not found!");
                return;
            }
        }

        // 在处理所有组之前先获取宝箱的初始状态
        ChestState chestState = new ChestState(chest);
        // 设置宝箱名称
        if (config.name != null && !config.name.isEmpty()) {
            String displayName = ChatColor.translateAlternateColorCodes('&', config.name);
            chest.setCustomName(displayName);
            plugin.debug("Set enhanced chest custom name to: " + displayName);
        }

        // 确保箱子仍然存在
        if (chest.getBlock().getType() != Material.CHEST) {
            plugin.debug("Chest no longer exists at " + chest.getLocation());
            return;
        }

        // 确保箱子没有被锁定
        if (chest.isLocked()) {
            plugin.debug("Chest is locked, unlocking");
            chest.setLock(null); // 移除锁定
        }

        Inventory inventory = chest.getInventory();
        ThreadLocalRandom threadRandom = ThreadLocalRandom.current();

        // 处理所有组
        for (Map.Entry<String, ChestGroup> groupEntry : config.groups.entrySet()) {
            ChestGroup group = groupEntry.getValue();
            int itemsAmount = group.minItems >= group.maxItems ? group.minItems : threadRandom.nextInt(group.minItems, group.maxItems + 1);

            plugin.debug("Generating " + itemsAmount + " items for group: " + groupEntry.getKey());

            List<ContainerItem> selectedItems = new ArrayList<>();
            for (int i = 0; i < itemsAmount; i++) {
                ContainerItem item = group.weightedItems.getRandom();
                if (item != null) {
                    selectedItems.add(item);
                    plugin.debug("Selected item: " + item.buildItem().getType());
                }
            }

            // 创建容器轮询
            ContainerItem[] itemsArray = selectedItems.toArray(new ContainerItem[0]);
            ContainerPoll poll = new ContainerPoll(plugin, selectedItems.size(), selectedItems.size(), itemsArray);
            poll.run(inventory, threadRandom);
        }

        // 记录实际放置的物品数量
        int placedItems = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                placedItems++;
            }
        }
        plugin.debug("Total placed items: " + placedItems);


        // 记录最终库存状态
        if (plugin.isDebugEnabled()) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item != null && item.getType() != Material.AIR) {
                    plugin.debug("Final slot " + i + ": " + item.getType() + " x" + item.getAmount());
                }
            }
        }

        // 使用新的更新方法，避免重置宝箱状态
        updateChestWithoutReset(chest, chestState);
        plugin.debug("Enhanced chest filled with " + config.groups.size() + " groups");
        // 强制更新宝箱状态
        chest.update(true, true); // 强制更新并通知客户端
        plugin.debug("Force updated chest state");
    }

    private void updateChestWithoutReset(org.bukkit.block.Chest chest, ChestState originalState) {
        if (chest == null) {
            plugin.debug("Cannot update chest: chest is null");
            return;
        }

        if (originalState == null) {
            plugin.debug("Cannot update chest: originalState is null");
            return;
        }

        // 确保箱子仍然存在
        if (chest.getBlock().getType() != Material.CHEST) {
            plugin.debug("Cannot update chest: block is no longer a chest");
            return;
        }

        // 保存当前库存
        Inventory inventory = chest.getInventory();

        ItemStack[] contents = inventory.getContents();
        if (contents == null) {
            plugin.debug("Cannot update chest: contents is null");
            return;
        }

        // 恢复原始宝箱状态（除了库存）
        if (originalState.customName != null) {
            chest.setCustomName(originalState.customName);
        } else {
            chest.setCustomName(null);
        }

        if (originalState.lock != null) {
            chest.setLock(originalState.lock);
        } else {
            chest.setLock(null);
        }

        // 恢复库存
        inventory.setContents(contents);

        // 更新宝箱
        try {
            chest.update();
            plugin.debug("Chest updated without reset");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update chest", e);
        }
    }

    public String getRandomChestConfig() {
        if (chestConfigs.isEmpty()) {
            return "fallback";
        }

        List<String> configNames = new ArrayList<>(chestConfigs.keySet());
        return configNames.get(random.nextInt(configNames.size()));
    }

    public boolean shouldGenerateChest() {
        double chance = plugin.getConfig().getDouble("chest.chance", 0.05);
        return random.nextDouble() <= chance;
    }

    public void debugChestContents(org.bukkit.block.Chest chest) {
        if (chest == null) {
            plugin.debug("Chest is null");
            return;
        }

        // 确保箱子仍然存在
        if (chest.getBlock().getType() != Material.CHEST) {
            plugin.debug("Chest no longer exists at " + chest.getLocation());
            return;
        }

        Inventory inventory = chest.getInventory();
        plugin.debug("Debugging chest contents at " + chest.getLocation());

        int itemCount = 0;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                itemCount++;
                plugin.debug("Slot " + i + ": " + item.getType() + " x" + item.getAmount());
            }
        }

        plugin.debug("Chest contains " + itemCount + " items");
    }

    // 内部类用于保存宝箱状态
    private static class ChestState {
        public String customName;
        public String lock;

        public ChestState(org.bukkit.block.Chest chest) {
            if (chest != null) {
                this.customName = chest.getCustomName();
                this.lock = chest.getLock();
            }
        }
    }

    private static class ChestConfig {
        public String name = "宝箱";
        public int globalMinItems = 3;
        public int globalMaxItems = 6;
        public Map<String, ChestGroup> groups = new HashMap<>();
    }

    private static class ChestGroup {
        public int minItems = 1;
        public int maxItems = 1;
        public WeightedRandom<ContainerItem> weightedItems = new WeightedRandom<>();
    }
}