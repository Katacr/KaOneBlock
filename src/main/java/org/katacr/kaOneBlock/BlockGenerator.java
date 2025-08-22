package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

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
        // 检查当前世界类型是否允许生成方块
        if (!plugin.isWorldAllowed(player.getWorld())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("world-type-not-allowed"));
            return;
        }

        // 检查玩家是否已经在当前世界生成过方块
        String worldName = player.getWorld().getName();
        boolean hasBlock = plugin.getDatabaseManager().hasBlockInWorld(player.getUniqueId(), worldName);

        if (hasBlock) {
            player.sendMessage(plugin.getLanguageManager().getMessage("already-generated"));
            return;
        }

        // 获取玩家腿部位置（即玩家所在位置）
        Location playerLocation = player.getLocation();

        // 获取玩家脚下的方块位置
        Location blockLocation = playerLocation.clone();

        // 在玩家当前位置生成方块（使用配置的方块类型）
        Block targetBlock = blockLocation.getBlock();
        Material blockMaterial = plugin.getDefaultBlockMaterial();
        targetBlock.setType(blockMaterial);

        // 记录生成事件到数据库（传递7个参数）
        plugin.getDatabaseManager().logBlockGeneration(
                player.getUniqueId(),
                player.getName(),
                worldName,
                blockLocation.getBlockX(),
                blockLocation.getBlockY(),
                blockLocation.getBlockZ(),
                blockMaterial.name() // 第7个参数：方块类型
        );

        // 发送生成成功的消息（包含方块类型）
        String message = plugin.getLanguageManager().getMessage("block-generated")
                .replace("%block%", blockMaterial.name().toLowerCase().replace("_", " "));
        player.sendMessage(message);
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
    public void removePlayerBlockInCurrentWorld(Player player) {
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