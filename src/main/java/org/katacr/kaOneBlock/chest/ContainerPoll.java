package org.katacr.kaOneBlock.chest;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.katacr.kaOneBlock.KaOneBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ContainerPoll {
    private final KaOneBlock plugin;
    private final ContainerItem[] items;
    private final int min; 
    private final int max; 

    public ContainerPoll(KaOneBlock plugin, int min, int max, ContainerItem[] items) {
        this.plugin = plugin;
        this.min = min;
        this.max = max;
        this.items = items;
    }

    public void run(Inventory inventory, ThreadLocalRandom random) {
        // 确定要放置的物品数量
        int itemsToPlace = min;
        if (max > min) {
            itemsToPlace = random.nextInt(min, max + 1);
        }

        plugin.debug("Attempting to place " + itemsToPlace + " items in chest");

        // 收集所有空槽位
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                emptySlots.add(i);
            }
        }

        plugin.debug("Found " + emptySlots.size() + " empty slots");

        // 随机打乱空槽位
        Collections.shuffle(emptySlots);

        // 放置物品
        int placedItems = 0;
        for (ContainerItem containerItem : items) {
            if (placedItems >= itemsToPlace || emptySlots.isEmpty()) {
                break;
            }

            ItemStack itemStack = containerItem.buildItem();

            // 跳过无效物品
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                plugin.debug("Skipping invalid item: " + (itemStack == null ? "null" : "AIR"));
                continue;
            }

            // 获取槽位
            int slot;
            if (containerItem.hasSlot()) {
                slot = containerItem.getSlot();
                plugin.debug("Using specified slot: " + slot);

                // 检查槽位是否可用
                if (slot < 0 || slot >= inventory.getSize()) {
                    plugin.debug("Specified slot is out of range: " + slot);
                    slot = emptySlots.isEmpty() ? -1 : emptySlots.remove(0);
                } else if (inventory.getItem(slot) != null) {
                    plugin.debug("Specified slot is occupied: " + slot);
                    slot = emptySlots.isEmpty() ? -1 : emptySlots.remove(0);
                }
            } else {
                slot = emptySlots.isEmpty() ? -1 : emptySlots.remove(0);
            }

            // 如果槽位有效，放置物品
            if (slot >= 0 && slot < inventory.getSize()) {
                plugin.debug("Placing " + itemStack.getType() + " at slot " + slot);
                inventory.setItem(slot, itemStack);
                placedItems++;

                // 立即验证物品是否被放置
                ItemStack placedItem = inventory.getItem(slot);
                if (placedItem == null || placedItem.getType() == Material.AIR) {
                    plugin.debug("ERROR: Item not placed at slot " + slot);
                } else {
                    plugin.debug("Confirmed: " + placedItem.getType() + " x" + placedItem.getAmount() + " at slot " + slot);

                    // 添加持久化检查
                    if (!isItemPersistent(placedItem, slot, inventory)) {
                        plugin.debug("WARNING: Item may not be persistent at slot " + slot);
                    }
                }
            }
        }

        plugin.debug("Successfully placed " + placedItems + " items in chest");
    }

    private boolean isItemPersistent(ItemStack item, int slot, Inventory inventory) {
        // 检查物品是否真的被保存到库存中
        ItemStack slotItem = inventory.getItem(slot);
        return slotItem != null && slotItem.getType() == item.getType() && slotItem.getAmount() == item.getAmount();
    }

}