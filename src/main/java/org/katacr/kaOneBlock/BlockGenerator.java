package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class BlockGenerator {
    private final KaOneBlock plugin;

    public BlockGenerator(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * 在玩家腿部位置生成石头方块（每个世界只能生成一次）
     *
     * @param player 目标玩家
     */
    public void generateBlockAtPlayerLocation(Player player) {
        String worldName = player.getWorld().getName();

        // 检查当前世界是否允许生成方块
        if (!plugin.isWorldAllowed(worldName)) {
            player.sendMessage(plugin.getLanguageManager().getMessage("world-type-not-allowed"));
            return;
        }

        // 检查玩家是否已经在当前世界生成过方块
        boolean hasBlock = plugin.getDatabaseManager().hasBlockInWorld(player.getUniqueId(), worldName);

        if (hasBlock) {
            player.sendMessage(plugin.getLanguageManager().getMessage("already-generated"));
            return;
        }

        // 获取玩家腿部位置（即玩家所在位置）
        Location playerLocation = player.getLocation();

        // 获取玩家脚下的方块位置
        Location blockLocation = playerLocation.clone();

        // 获取方块列表
        WeightedRandom<Object> blockList = plugin.getBlockListForWorld(worldName);
        Object blockObj = Material.STONE; // 默认使用石头

        if (blockList != null) {
            // 从方块列表中随机选择方块
            blockObj = blockList.getRandom();
            if (blockObj == null) {
                // 无法选择方块，使用石头
                plugin.debug("Failed to get random block for world: " + worldName);
                blockObj = Material.STONE;
            }
        } else {
            // 没有配置方块列表，使用石头
            plugin.debug("No block list found for world: " + worldName);
        }

        // 在玩家当前位置生成方块
        Block targetBlock = blockLocation.getBlock();
        if (blockObj instanceof Material material) {
            // 原版方块
            targetBlock.setType(material);
        } else {
            // ItemsAdder 方块
            String iaBlockId = (String) blockObj;
            // 去掉 "ia:" 前缀
            String realBlockId = iaBlockId.substring(3);
            boolean success = plugin.getItemsAdderManager().placeBlock(targetBlock.getLocation(), realBlockId);
            if (!success) {
                // 放置失败，使用石头作为回退
                targetBlock.setType(Material.STONE);
                blockObj = Material.STONE;
                plugin.debug("Failed to place ItemsAdder block: " + realBlockId + ", using STONE instead");
            }
        }

        // 记录生成事件到数据库
        String blockType = blockObj instanceof Material ?
                ((Material) blockObj).name() : (String) blockObj;
        plugin.getDatabaseManager().logBlockGeneration(
                player.getUniqueId(),
                player.getName(),
                worldName,
                blockLocation.getBlockX(),
                blockLocation.getBlockY(),
                blockLocation.getBlockZ(),
                blockType
        );

        // 记录日志
        plugin.getLogManager().logBlockGeneration(
                player.getName(),
                worldName,
                blockLocation,
                Material.valueOf(blockType)
        );

        // 生成可读的方块名称
        String blockDisplayName;
        if (blockObj instanceof Material material) {
            blockDisplayName = plugin.formatMaterialName(material);
        } else {
            // ItemsAdder 方块
            String blockId = (String) blockObj;
            if (blockId.startsWith("ia:")) {
                blockDisplayName = blockId.substring(3).replace("_", " ");
            } else {
                blockDisplayName = blockId.replace("_", " ");
            }
        }

        // 发送生成成功的消息（包含方块类型）
        String message = plugin.getLanguageManager().getMessage("block-generated")
                .replace("%block%", blockDisplayName);
        player.sendMessage(message);

        // 记录调试信息（控制台专用）
        Map<String, String> debugReplacements = new HashMap<>();
        debugReplacements.put("world", worldName);
        debugReplacements.put("location", blockLocation.toString());
        debugReplacements.put("block", blockType);
        plugin.debug("debug-generated-block", debugReplacements);
    }


    /**
     * 移除指定位置的方块（设置为空气）
     *
     * @param location 方块位置
     */
    public void removeBlockAtLocation(Location location) {
        Block targetBlock = location.getBlock();
        targetBlock.setType(Material.AIR);
    }

    /**
     * 移除玩家在当前世界生成的方块
     *
     * @param player 玩家
     */
    public void removePlayerBlockInCurrentWorld(@Nonnull Player player) {
        String worldName = player.getWorld().getName();

        // 从数据库获取玩家在当前世界生成的方块信息
        Map<String, Object> blockInfo = plugin.getDatabaseManager().findBlockByPlayerAndWorld(player.getUniqueId(), worldName);

        if (blockInfo == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no-blocks-in-world"));
            return;
        }

        // 获取坐标信息
        int x = (Integer) blockInfo.get("x");
        int y = (Integer) blockInfo.get("y");
        int z = (Integer) blockInfo.get("z");

        // 获取世界对象
        org.bukkit.World world = player.getWorld();

        // 创建位置对象并移除方块
        Location blockLocation = new Location(world, x, y, z);
        removeBlockAtLocation(blockLocation);

        // 从数据库删除记录
        boolean deleted = plugin.getDatabaseManager().deleteBlockByPlayerAndWorld(player.getUniqueId(), worldName);

        if (deleted) {
            player.sendMessage(plugin.getLanguageManager().getMessage("block-removed"));
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("remove-failed"));
        }
    }

}