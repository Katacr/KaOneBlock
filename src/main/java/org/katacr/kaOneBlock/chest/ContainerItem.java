package org.katacr.kaOneBlock.chest;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.katacr.kaOneBlock.KaOneBlock;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds one validated vanilla or lazily resolved ItemsAdder loot entry.
 */
public class ContainerItem {
    private final KaOneBlock plugin;
    private final ItemStack templateItem;
    private final String customItemId;
    private final int slot;
    private final int min;
    private final int max;

    public ContainerItem(KaOneBlock plugin, ItemStack templateItem, int slot, int min, int max) {
        this(plugin, templateItem.clone(), null, slot, min, max);
    }

    /**
     * Creates an ItemsAdder loot entry that is resolved only when a chest is generated.
     */
    public static ContainerItem custom(KaOneBlock plugin, String customItemId, int slot, int min, int max) {
        return new ContainerItem(plugin, null, customItemId, slot, min, max);
    }

    private ContainerItem(KaOneBlock plugin, ItemStack templateItem, String customItemId, int slot, int min, int max) {
        this.plugin = plugin;
        this.templateItem = templateItem;
        this.customItemId = customItemId;
        this.slot = slot;
        this.min = Math.max(1, Math.min(64, min));
        this.max = Math.max(this.min, Math.min(64, max));
    }

    /**
     * Returns the preferred fixed inventory slot, or a negative value for random placement.
     */
    public int getSlot() {
        return slot;
    }

    /**
     * Reports whether this entry requests a fixed inventory slot.
     */
    public boolean hasSlot() {
        return slot >= 0;
    }

    /**
     * Resolves and clones the item with a valid random stack amount.
     */
    public ItemStack buildItem() {
        ItemStack itemStack = customItemId == null
                ? templateItem.clone()
                : plugin.getItemsAdderManager().getCustomItemSilently(customItemId);
        if (itemStack == null || itemStack.getType() == Material.AIR) {
            return null;
        }

        int amount = max > min ? ThreadLocalRandom.current().nextInt(min, max + 1) : min;
        itemStack.setAmount(Math.min(amount, itemStack.getMaxStackSize()));
        return itemStack;
    }

    /**
     * Describes the configured type without resolving or mutating the loot entry.
     */
    public String describeType() {
        return customItemId == null ? templateItem.getType().name() : "IA:" + customItemId;
    }
}
