package org.katacr.kaOneBlock.chest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.katacr.kaOneBlock.KaOneBlock;
import java.util.concurrent.ThreadLocalRandom;

public class ContainerItem {
    private final ItemStack templateItem;
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

        // 确保物品有效
        if (itemStack.getType() == Material.AIR) {
            plugin.debug("Invalid item: AIR");
            return null;
        }

        return itemStack;
    }
}