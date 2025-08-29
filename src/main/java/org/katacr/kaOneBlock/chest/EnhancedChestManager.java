package org.katacr.kaOneBlock.chest;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
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
        Material material = null;
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
            material = Material.matchMaterial(materialName);
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

        // 处理附魔书
        if (material == Material.ENCHANTED_BOOK && itemSection.isConfigurationSection("stored-enchantments")) {
            ItemMeta meta = itemStack.getItemMeta();
            if (meta instanceof EnchantmentStorageMeta enchantMeta) {
                ConfigurationSection enchantsSection = itemSection.getConfigurationSection("stored-enchantments");
                if (enchantsSection != null) {
                    for (String enchantName : enchantsSection.getKeys(false)) {
                        Enchantment enchant = getEnchantmentByName(enchantName);
                        if (enchant != null) {
                            int level = enchantsSection.getInt(enchantName, 1);
                            enchantMeta.addStoredEnchant(enchant, level, true);
                        }
                    }
                }
                itemStack.setItemMeta(enchantMeta);
            }
        }

        // 处理药水
        if (itemStack.getType().name().endsWith("POTION") || itemStack.getType() == Material.TIPPED_ARROW) {

            ItemMeta meta = itemStack.getItemMeta();
            if (meta instanceof PotionMeta potionMeta) {
                // 基础药水类型
                if (itemSection.isString("potion-type")) {
                    String potionTypeStr = itemSection.getString("potion-type");
                    try {
                        PotionType potionType = PotionType.valueOf(potionTypeStr.toUpperCase());
                        potionMeta.setBasePotionData(new PotionData(potionType));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid potion type: " + potionTypeStr);
                    }
                }

                // 自定义效果
                if (itemSection.isConfigurationSection("custom-effects")) {
                    ConfigurationSection effectsSection = itemSection.getConfigurationSection("custom-effects");
                    for (String effectKey : effectsSection.getKeys(false)) {
                        ConfigurationSection effectSection = effectsSection.getConfigurationSection(effectKey);
                        if (effectSection != null) {
                            String effectTypeName = effectSection.getString("type");
                            PotionEffectType type = PotionEffectType.getByName(effectTypeName.toUpperCase());
                            if (type != null) {
                                int duration = effectSection.getInt("duration", 200); // 默认10秒
                                int amplifier = effectSection.getInt("amplifier", 0);
                                boolean ambient = effectSection.getBoolean("ambient", false);
                                boolean particles = effectSection.getBoolean("particles", true);
                                boolean icon = effectSection.getBoolean("icon", true);

                                PotionEffect effect = new PotionEffect(type, duration, amplifier, ambient, particles, icon);
                                potionMeta.addCustomEffect(effect, true);
                            }
                        }
                    }
                }
                itemStack.setItemMeta(potionMeta);
            }
        }
        int slot = itemSection.getInt("slot", -1);
        int min = itemSection.getInt("min", 1);
        int max = itemSection.getInt("max", min);

        return new ContainerItem(plugin, itemStack, slot, min, max);
    }

    // 辅助方法：获取附魔
    private Enchantment getEnchantmentByName(String name) {
        try {
            NamespacedKey key = NamespacedKey.minecraft(name.toLowerCase());
            return Enchantment.getByKey(key);
        } catch (Exception e) {
            return Enchantment.getByName(name.toUpperCase());
        }
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

        // 安全更新宝箱
        safeUpdateChestWithoutLock(chest);
        plugin.debug("Enhanced chest filled with " + config.groups.size() + " groups");
    }

    private void safeUpdateChestWithoutLock(org.bukkit.block.Chest chest) {
        if (chest == null) {
            plugin.debug("Cannot update chest: chest is null");
            return;
        }

        try {
            // 获取宝箱的方块状态
            BlockState state = chest.getBlock().getState();
            if (state instanceof org.bukkit.block.Chest chestState) {
                // 复制库存
                chestState.getInventory().setContents(chest.getInventory().getContents());

                // 复制名称
                chestState.setCustomName(chest.getCustomName());

                // 关键修复：完全避免设置锁定状态
                // 不设置锁定状态，避免触发问题

                // 更新方块状态
                chestState.update(true, true);
                plugin.debug("Chest updated safely without lock");
            } else {
                plugin.debug("Block is no longer a chest, cannot update");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to safely update chest", e);
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

                // 确保锁定状态不为空字符串
                if (this.lock.isEmpty()) {
                    this.lock = null;
                }
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