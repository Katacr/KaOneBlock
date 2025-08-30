package org.katacr.kaOneBlock.chest;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.katacr.kaOneBlock.KaOneBlock;

import java.util.concurrent.ThreadLocalRandom;

public class ContainerItem {
    private ItemStack templateItem;
    private final int slot;
    private final int min;
    private final int max;
    private final KaOneBlock plugin;

    public ContainerItem(KaOneBlock plugin, ItemStack templateItem, int slot, int min, int max) {
        this.plugin = plugin;
        this.templateItem = templateItem.clone();
        this.slot = slot;
        this.min = min;
        this.max = max;
    }

    public int getSlot() {
        return slot;
    }

    public boolean hasSlot() {
        return slot >= 0;
    }

    public ItemStack buildItem() {
        int amount = min;
        if (max > min) {
            amount = ThreadLocalRandom.current().nextInt(min, max + 1);
        }

        // 确保数量有效
        if (amount <= 0) {
            amount = 1;
        }

        ItemStack itemStack = templateItem.clone();
        itemStack.setAmount(amount);
        // 检查是否为占位物品
        if (templateItem.getType() == Material.BARRIER) {
            ItemStack realItem = resolveDelayedItem();
            if (realItem != null) {
                templateItem = realItem; // 替换为真实物品
            }
        }
        // 确保物品有效
        if (itemStack.getType() == Material.AIR) {
            plugin.debug("Invalid item: AIR");
            return null;
        }

        return itemStack;
    }

    private ItemStack resolveDelayedItem() {
        ItemMeta meta = templateItem.getItemMeta();
        PersistentDataContainer pdc = null;
        if (meta != null) {
            pdc = meta.getPersistentDataContainer();
        }
        NamespacedKey key = new NamespacedKey(plugin, "ia-item-id");

        if (pdc != null && pdc.has(key, PersistentDataType.STRING)) {
            String itemId = pdc.get(key, PersistentDataType.STRING);
            ItemStack realItem = plugin.getItemsAdderManager().getCustomItem(itemId);

            if (realItem != null) {
                plugin.debug("成功加载延迟物品: " + itemId);
                return realItem;
            } else {
                plugin.debug("无法加载ItemsAdder物品: " + itemId);
            }
        }
        return null;
    }
}