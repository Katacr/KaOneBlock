package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class BlockBreakListener implements Listener {
    private final KaOneBlock plugin;
    private final Set<Location> processedChests = new HashSet<>();

    public BlockBreakListener(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChestBreak(BlockBreakEvent event) {
        if (event.getBlock().getType() == Material.CHEST) {
            Chest chest = (Chest) event.getBlock().getState();
            plugin.debug("宝箱被破坏: " + chest.getLocation());
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    plugin.debug("掉落物品: " + item.getType() + " x" + item.getAmount());
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();
        String worldName = Objects.requireNonNull(location.getWorld()).getName();

        // 检查方块是否由本插件生成
        Map<String, Object> blockInfo = plugin.getDatabaseManager().findBlockByLocation(worldName, location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (blockInfo == null) {
            // 不是本插件生成的方块，不做处理
            return;
        }

        // 获取方块列表
        WeightedRandom<Object> blockList = plugin.getBlockListForWorld(worldName);
        Object newBlockObj = Material.STONE; // 默认使用石头
        boolean isChest = false;
        String chestConfigName = null;

        // 检查是否应该生成宝箱
        if (plugin.getEnhancedChestManager().shouldGenerateChest()) {
            // 生成宝箱而不是普通方块
            newBlockObj = Material.CHEST;
            isChest = true;
            chestConfigName = plugin.getEnhancedChestManager().getRandomChestConfig();
        } else {
            if (blockList != null) {
                // 随机选择新方块
                newBlockObj = blockList.getRandom();
                if (newBlockObj == null) {
                    // 无法选择方块，使用石头
                    plugin.debug("Failed to get random block for world: " + worldName);
                    newBlockObj = Material.STONE;
                }
            } else {
                // 没有配置方块列表，使用石头
                plugin.debug("No block list found for world: " + worldName);
            }
        }

        // 更新数据库记录
        final String[] newBlockType = {newBlockObj instanceof Material ? ((Material) newBlockObj).name() : (String) newBlockObj};

        // 如果是宝箱，在数据库中用特殊标记
        if (isChest && chestConfigName != null) {
            newBlockType[0] = "CHEST:" + chestConfigName;
        }

        plugin.getDatabaseManager().updateBlockType((Integer) blockInfo.get("id"), newBlockType[0]);

        // 在事件完成后重新放置方块（延迟1 tick）
        Object finalNewBlockObj = newBlockObj;
        boolean finalIsChest = isChest;
        String finalChestConfigName = chestConfigName;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 检查位置是否仍然是空气（确保没有其他方块被放置）
            if (location.getBlock().getType() == Material.AIR) {
                if (finalIsChest) {
                    // 检查是否已经处理过这个位置的宝箱
                    if (processedChests.contains(location)) {
                        plugin.debug("Chest already processed at " + location);
                        return;
                    }

                    // 标记这个位置的宝箱为已处理
                    processedChests.add(location);

                    // 1. 先放置宝箱方块
                    location.getBlock().setType(Material.CHEST);
                    plugin.debug("Chest block placed at " + location);

                    // 2. 延迟1tick填充宝箱
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        // 清理已处理的宝箱记录（避免内存泄漏）
                        processedChests.remove(location);

                        if (location.getBlock().getState() instanceof org.bukkit.block.Chest chest) {
                            // 3. 填充宝箱物品
                            plugin.getEnhancedChestManager().fillChest(chest, finalChestConfigName);

                            // 4. 记录调试信息
                            Map<String, String> debugReplacements = new HashMap<>();
                            debugReplacements.put("world", worldName);
                            debugReplacements.put("location", location.toString());
                            debugReplacements.put("chestConfig", finalChestConfigName);
                            plugin.debug("debug-generated-enhanced-chest", debugReplacements);

                            // 5. 记录日志
                            plugin.getLogManager().logChestGeneration(player.getName(), worldName, location, finalChestConfigName);
                        }
                    }, 1L); // 额外延迟1tick
                } else if (finalNewBlockObj instanceof Material material) {
                    // 原版方块
                    location.getBlock().setType(material);
                } else {
                    // ItemsAdder 方块
                    String iaBlockId = (String) finalNewBlockObj;
                    // 去掉 "ia:" 前缀
                    String realBlockId = iaBlockId.substring(3);
                    boolean success = plugin.getItemsAdderManager().placeBlock(location, realBlockId);
                    if (!success) {
                        // 放置失败，使用石头作为回退
                        location.getBlock().setType(Material.STONE);
                        newBlockType[0] = Material.STONE.name();
                        plugin.debug("Failed to place ItemsAdder block: " + realBlockId + ", using STONE instead");
                    }
                }

                // 记录调试信息（如果不是宝箱）
                if (!finalIsChest) {
                    Map<String, String> debugReplacements = new HashMap<>();
                    debugReplacements.put("world", worldName);
                    debugReplacements.put("location", location.toString());
                    debugReplacements.put("block", newBlockType[0]);
                    plugin.debug("debug-replaced-block", debugReplacements);

                    // 记录日志
                    plugin.getLogManager().logBlockReplacement(player.getName(), worldName, location, newBlockType[0]);
                }
            } else {
                // 记录调试信息
                Map<String, String> debugReplacements = new HashMap<>();
                debugReplacements.put("world", worldName);
                debugReplacements.put("location", location.toString());
                plugin.debug("debug-position-not-empty", debugReplacements);
            }

            // 只有在调试模式开启且玩家有权限时才向玩家发送消息
            if (plugin.isDebugEnabled() && player.hasPermission("kaoneblock.debug")) {
                // 格式化方块名称
                String formattedBlockName;
                if (finalIsChest) {
                    // 尝试获取语言文件中的宝箱名称，如果不存在则使用默认值
                    String chestNameMsg = plugin.getLanguageManager().getMessage("chest-name");
                    if (chestNameMsg.startsWith("Message not found")) {
                        formattedBlockName = "宝箱"; // 默认值
                    } else {
                        formattedBlockName = chestNameMsg;
                    }
                } else if (finalNewBlockObj instanceof Material material) {
                    formattedBlockName = plugin.formatMaterialName(material);
                } else {
                    // ItemsAdder 方块
                    String blockId = (String) finalNewBlockObj;
                    if (blockId.startsWith("ia:")) {
                        formattedBlockName = blockId.substring(3).replace("_", " ");
                    } else {
                        formattedBlockName = blockId.replace("_", " ");
                    }
                }

                String message = plugin.getLanguageManager().getMessage("block-transformed").replace("%block%", formattedBlockName);
                player.sendMessage(message);
            }
        }, 1L); // 初始延迟1tick
    }
}