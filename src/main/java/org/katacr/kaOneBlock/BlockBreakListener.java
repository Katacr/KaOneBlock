package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
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

        // 检查方块是否由本插件生成
        Map<String, Object> blockInfo = plugin.getDatabaseManager().findBlockByLocation(Objects.requireNonNull(location.getWorld()).getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (blockInfo == null) {
            // 不是本插件生成的方块，不做处理
            return;
        }

        // 获取世界类型
        String worldType = getWorldType(player.getWorld().getEnvironment());

        // 获取方块列表
        WeightedRandom<Material> blockList = plugin.getBlockList(worldType);
        Material newMaterial = null;

        if (blockList != null) {
            // 随机选择新方块
            newMaterial = blockList.getRandom();
        }

        if (newMaterial == null) {
            // 无法选择方块，使用默认方块
            newMaterial = plugin.getDefaultBlockMaterial();
            plugin.getLogger().warning("Failed to get random block for world type: " + worldType);
        }

        // 更新数据库记录
        plugin.getDatabaseManager().updateBlockType((Integer) blockInfo.get("id"), newMaterial.name());

        // 在事件完成后重新放置方块
        Material finalNewMaterial = newMaterial;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (location.getBlock().getType() == Material.AIR) {
                // 设置新方块
                location.getBlock().setType(finalNewMaterial);

                // 记录调试信息
                Map<String, String> replacements = new HashMap<>();
                replacements.put("location", location.toString());
                replacements.put("block", finalNewMaterial.name());
                plugin.debug("debug-replaced-block", replacements);
            } else {
                // 记录调试信息
                Map<String, String> replacements = new HashMap<>();
                replacements.put("location", location.toString());
                plugin.debug("debug-position-not-empty", replacements);
            }
        }, 1L);
    }

    // 将世界环境类型转换为配置键
    private String getWorldType(World.Environment environment) {
        return switch (environment) {
            case NETHER -> "nether";
            case THE_END -> "the_end";
            default -> "normal";
        };
    }
}