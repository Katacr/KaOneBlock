package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockGenerator {
    private final KaOneBlock plugin;

    public BlockGenerator(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * 在玩家腿部位置生成起始方块
     *
     * @param player 目标玩家
     */
    public void generateBlockAtPlayerLocation(Player player) {
        // 初始化玩家进度 - 这会发送初始阶段消息
        plugin.getStageManager().initPlayerProgress(player);

        // 检查玩家是否已经生成过方块
        boolean hasBlock = plugin.getDatabaseManager().hasBlock(player.getUniqueId());

        if (hasBlock) {
            player.sendMessage(plugin.getLanguageManager().getMessage("already-generated"));
            return;
        }

        // 获取玩家腿部位置（即玩家所在位置）
        Location playerLocation = player.getLocation();

        // 获取玩家脚下的方块位置
        Location blockLocation = playerLocation.clone();
        blockLocation.setY(blockLocation.getY());

        // 检查目标位置是否可替换
        Block targetBlock = blockLocation.getBlock();
        if (targetBlock.getType() != Material.AIR) {
            // 获取位置信息
            String locationInfo = String.format("(%d, %d, %d)", blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());

            // 获取方块名称
            String blockName = plugin.formatMaterialName(targetBlock.getType());

            // 发送错误消息
            String message = plugin.getLanguageManager().getMessage("position-not-air").replace("%location%", locationInfo).replace("%block%", blockName);
            player.sendMessage(message);

            // 记录调试信息
            Map<String, String> debugReplacements = new HashMap<>();
            debugReplacements.put("location", locationInfo);
            debugReplacements.put("block", targetBlock.getType().name());
            plugin.debug("debug-position-not-air", debugReplacements);

            return;
        }

        // 获取当前阶段配置文件名
        UUID playerId = player.getUniqueId();
        String stageFile = plugin.getStageManager().getCurrentStageFile(playerId);

        // 获取玩家进度
        int blocksBroken = 0;
        StageManager.PlayerStageProgress progress = plugin.getStageManager().getPlayerProgress(playerId);
        if (progress != null) {
            blocksBroken = progress.blocksBroken;
        }

        // 获取当前阶段的方块列表
        WeightedRandom<Object> blockList = plugin.getBlockListManager().getBlockList(stageFile);
        Object blockObj = Material.STONE; // 默认使用石头

        if (blockList != null) {
            // 从方块列表中随机选择方块
            blockObj = blockList.getRandom();
            if (blockObj == null) {
                plugin.debug("Failed to get random block from stage: " + stageFile);
                blockObj = Material.STONE;
            }
        } else {
            plugin.debug("Block list for stage " + stageFile + " not loaded");
        }

        // 在玩家当前位置生成方块
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
        String blockType = blockObj instanceof Material ? ((Material) blockObj).name() : (String) blockObj;

        plugin.getDatabaseManager().logBlockGeneration(player.getUniqueId(), player.getName(), blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(), blockType, stageFile, blocksBroken);

        // 记录日志
        if (blockObj instanceof Material material) {
            plugin.getLogManager().logBlockGeneration(player.getName(), blockLocation, material);
        } else {
            plugin.getLogManager().logBlockGeneration(player.getName(), blockLocation, (String) blockObj);
        }

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
        String message = plugin.getLanguageManager().getMessage("block-generated").replace("%block%", blockDisplayName);
        player.sendMessage(message);

        // 记录调试信息（控制台专用）
        Map<String, String> debugReplacements = KaOneBlock.createDebugReplacements(blockLocation, blockType);
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
     * 移除玩家生成的方块并重置阶段
     *
     * @param player 玩家
     */
    public void removePlayerBlock(@Nonnull Player player) {
        // 从数据库获取玩家生成的方块信息
        Map<String, Object> blockInfo = plugin.getDatabaseManager().findBlockByPlayer(player.getUniqueId());

        if (blockInfo == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no-blocks"));
            return;
        }

        // 获取坐标信息
        int x = (Integer) blockInfo.get("x");
        int y = (Integer) blockInfo.get("y");
        int z = (Integer) blockInfo.get("z");

        // 获取世界对象（使用玩家当前世界）
        org.bukkit.World world = player.getWorld();

        // 创建位置对象并移除方块
        Location blockLocation = new Location(world, x, y, z);
        removeBlockAtLocation(blockLocation);

        // 从数据库删除记录
        boolean deleted = plugin.getDatabaseManager().deleteBlock(player.getUniqueId());

        if (deleted) {
            // 重置玩家阶段为初始阶段
            plugin.getStageManager().setPlayerStage(player, "normal.yml");
            player.sendMessage(plugin.getLanguageManager().getMessage("block-removed"));
        } else {
            player.sendMessage(plugin.getLanguageManager().getMessage("remove-failed"));
        }
    }
}