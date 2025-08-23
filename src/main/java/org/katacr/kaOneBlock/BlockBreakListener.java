package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 方块破坏监听器
 * 处理核心玩法逻辑
 */
public class BlockBreakListener implements Listener {
    private final KaOneBlock plugin;

    public BlockBreakListener(KaOneBlock plugin) {
        this.plugin = plugin;
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

        // 更新数据库记录
        final String[] newBlockType = {newBlockObj instanceof Material ? ((Material) newBlockObj).name() : (String) newBlockObj};
        plugin.getDatabaseManager().updateBlockType((Integer) blockInfo.get("id"), newBlockType[0]);

        // 在事件完成后重新放置方块（延迟1 tick）
        Object finalNewBlockObj = newBlockObj;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // 检查位置是否仍然是空气（确保没有其他方块被放置）
            if (location.getBlock().getType() == Material.AIR) {
                if (finalNewBlockObj instanceof Material material) {
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

                // 记录调试信息
                Map<String, String> debugReplacements = new HashMap<>();
                debugReplacements.put("world", worldName);
                debugReplacements.put("location", location.toString());
                debugReplacements.put("block", newBlockType[0]);
                plugin.debug("debug-replaced-block", debugReplacements);

                // 记录日志
                plugin.getLogManager().logBlockReplacement(player.getName(), worldName, location, newBlockType[0]);
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
                if (finalNewBlockObj instanceof Material material) {
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
        }, 1L); // 延迟1 tick执行，确保破坏事件完成
    }
}