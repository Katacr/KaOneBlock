package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

        // 检查方块是否由本插件生成
        Map<String, Object> blockInfo = plugin.getDatabaseManager().findBlockByLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        if (blockInfo == null) {
            // 不是本插件生成的方块，不做处理
            return;
        }

        // 初始化玩家进度（如果尚未初始化）
        plugin.getStageManager().initPlayerProgress(player);

        // 增加玩家破坏方块计数
        plugin.getStageManager().incrementBlocksBroken(player.getUniqueId());

        // 获取当前阶段的宝箱概率配置
        Map<String, Double> chestChances = plugin.getStageManager().getChestChances(player.getUniqueId());

        // 检查是否应该生成宝箱
        boolean isChest = false;
        boolean isEntity = false;
        String chestConfigName = null;

        // 获取实体生成概率
        StageConfig stageConfig = plugin.getStageConfigManager().loadStageConfig(
            plugin.getStageManager().getCurrentStageFile(player.getUniqueId()));
        double entityChance = stageConfig != null ? stageConfig.entityChance : 0.05;

        // 随机决定生成内容
        double random = Math.random();
        
        // 优先级：实体 > 宝箱 > 方块
        if (random <= entityChance) {
            // 生成实体（5%）
            isEntity = true;
        } else if (!chestChances.isEmpty()) {
            // 使用阶段特定的宝箱概率
            for (Map.Entry<String, Double> entry : chestChances.entrySet()) {
                if (random <= entry.getValue() + entityChance) {
                    isChest = true;
                    chestConfigName = entry.getKey();
                    break;
                }
            }
        } else {
            // 使用全局宝箱概率
            double chestChance = plugin.getConfig().getDouble("chest-chance", 0.05);
            if (random <= chestChance + entityChance) {
                isChest = true;
                chestConfigName = plugin.getEnhancedChestManager().getRandomChestConfig();
            }
        }

        // 获取当前阶段配置文件名
        String stageFile = plugin.getStageManager().getCurrentStageFile(player.getUniqueId());

        // 获取当前阶段的方块列表
        WeightedRandom<Object> blockList = plugin.getBlockListManager().getBlockList(stageFile);
        Object newBlockObj = Material.STONE; // 默认使用石头

        if (!isChest) {
            if (blockList != null) {
                // 随机选择新方块
                newBlockObj = blockList.getRandom();
                if (newBlockObj == null) {
                    plugin.debug("Failed to get random block from stage: " + stageFile);
                    newBlockObj = Material.STONE;
                }
            } else {
                plugin.debug("Block list for stage " + stageFile + " not loaded");
            }
        } else {
            // 生成宝箱
            newBlockObj = Material.CHEST;
        }

        // 更新数据库记录
        final String newBlockType = newBlockObj instanceof Material ? ((Material) newBlockObj).name() : (String) newBlockObj;

        // 获取当前进度
        StageManager.PlayerStageProgress progress =
                plugin.getStageManager().getPlayerProgress(player.getUniqueId());

        if (progress != null) {
            plugin.getDatabaseManager().updatePlayerStage(
                    player.getUniqueId(),
                    progress.stageFile,
                    progress.blocksBroken
            );
        }

        // 如果是宝箱，在数据库中用特殊标记
        if (isChest && chestConfigName != null) {
            plugin.getDatabaseManager().updateBlockType((Integer) blockInfo.get("id"), "CHEST:" + chestConfigName);
        } else {
            plugin.getDatabaseManager().updateBlockType((Integer) blockInfo.get("id"), newBlockType);
        }

        // 在事件完成后重新放置方块/生成实体（延迟1 tick）
        Object finalNewBlockObj = newBlockObj;
        boolean finalIsChest = isChest;
        boolean finalIsEntity = isEntity;
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

                        if (location.getBlock().getState() instanceof Chest chest) {
                            // 3. 填充宝箱物品
                            plugin.getEnhancedChestManager().fillChest(chest, finalChestConfigName);

                            // 4. 记录调试信息
                            World world = location.getWorld();
                            String worldName = world != null ? world.getName() : "未知世界";
                            Map<String, String> debugReplacements = new HashMap<>();
                            debugReplacements.put("world", worldName);
                            debugReplacements.put("x", String.valueOf(location.getBlockX()));
                            debugReplacements.put("y", String.valueOf(location.getBlockY()));
                            debugReplacements.put("z", String.valueOf(location.getBlockZ()));
                            debugReplacements.put("chestConfig", finalChestConfigName);
                            plugin.debug("debug-generated-enhanced-chest", debugReplacements);

                            // 5. 记录日志
                            plugin.getLogManager().logChestGeneration(player.getName(), location, finalChestConfigName);
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
                        plugin.debug("Failed to place ItemsAdder block: " + realBlockId + ", using STONE instead");
                    }
                }

                // 记录调试信息（如果不是宝箱）
                if (!finalIsChest) {
                    World world = location.getWorld();
                    String worldName = world != null ? world.getName() : "未知世界";
                    Map<String, String> debugReplacements = new HashMap<>();
                    debugReplacements.put("world", worldName);
                    debugReplacements.put("x", String.valueOf(location.getBlockX()));
                    debugReplacements.put("y", String.valueOf(location.getBlockY()));
                    debugReplacements.put("z", String.valueOf(location.getBlockZ()));
                    debugReplacements.put("block", newBlockType);
                    plugin.debug("debug-replaced-block", debugReplacements);

                    // 记录日志
                    plugin.getLogManager().logBlockReplacement(player.getName(), location, newBlockType);
                }
            } else {
                // 记录调试信息
                World world = location.getWorld();
                String worldName = world != null ? world.getName() : "未知世界";
                Map<String, String> debugReplacements = new HashMap<>();
                debugReplacements.put("world", worldName);
                debugReplacements.put("x", String.valueOf(location.getBlockX()));
                debugReplacements.put("y", String.valueOf(location.getBlockY()));
                debugReplacements.put("z", String.valueOf(location.getBlockZ()));
                plugin.debug("debug-position-not-empty", debugReplacements);
            }

            // 只有在调试模式开启且玩家有权限时才向玩家发送消息
            if (plugin.isDebugEnabled() && player.hasPermission("kaoneblock.debug")) {
                // 格式化方块名称
                String formattedBlockName;
                if (finalIsChest) {
                    // 尝试获取语言文件中的宝箱名称
                    String chestNameMsg = plugin.getLanguageManager().getMessage("chest-name");
                    formattedBlockName = chestNameMsg.startsWith("Message not found") ? "宝箱" : chestNameMsg;
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