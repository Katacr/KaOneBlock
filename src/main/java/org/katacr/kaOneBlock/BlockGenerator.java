package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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

        // 获取世界类型
        String worldType = getWorldType(player.getWorld().getEnvironment());

        // 获取方块列表
        WeightedRandom<Material> blockList = plugin.getBlockList(worldType);
        Material blockMaterial = getMaterial(blockList, blockLocation);

        // 记录生成事件到数据库
        plugin.getDatabaseManager().logBlockGeneration(player.getUniqueId(), player.getName(), worldName, blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(), blockMaterial.name());

        // 记录调试信息
        plugin.debug("Generated block at " + blockLocation + ": " + blockMaterial);

        // 记录调试信息
        Map<String, String> replacements = new HashMap<>();
        replacements.put("location", blockLocation.toString());
        replacements.put("block", blockMaterial.name());
        plugin.debug("debug-generated-block", replacements);

    }

    private @NotNull Material getMaterial(WeightedRandom<Material> blockList, Location blockLocation) {
        Material blockMaterial;

        if (blockList != null) {
            // 从方块列表中随机选择方块
            blockMaterial = blockList.getRandom();
            if (blockMaterial == null) {
                // 无法选择方块，使用默认方块
                blockMaterial = plugin.getDefaultBlockMaterial();
            }
        } else {
            // 没有配置方块列表，使用默认方块
            blockMaterial = plugin.getDefaultBlockMaterial();
        }

        // 在玩家当前位置生成方块
        Block targetBlock = blockLocation.getBlock();
        targetBlock.setType(blockMaterial);
        return blockMaterial;
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

    /**
     * 将世界环境类型转换为配置键
     *
     * @param environment 世界环境
     * @return 配置键（normal, nether, the_end）
     */
    private String getWorldType(World.Environment environment) {
        return switch (environment) {
            case NETHER -> "nether";
            case THE_END -> "the_end";
            default -> "normal";
        };
    }
}