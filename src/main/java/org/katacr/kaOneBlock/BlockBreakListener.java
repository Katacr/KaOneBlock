package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Protects registered OneBlocks and executes one deterministic generation pipeline per valid break.
 */
public class BlockBreakListener implements Listener {
    private final KaOneBlock plugin;
    private final Set<BlockPosition> pendingChests = new HashSet<>();

    public BlockBreakListener(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Logs chest contents only after other plugins have accepted the break.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChestBreak(BlockBreakEvent event) {
        if (!plugin.isDebugEnabled() || event.getBlock().getType() != Material.CHEST) {
            return;
        }
        Chest chest = (Chest) event.getBlock().getState();
        plugin.debug("宝箱被破坏: " + chest.getLocation());
        for (ItemStack item : chest.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                plugin.debug("掉落物品: " + item.getType() + " x" + item.getAmount());
            }
        }
    }

    /**
     * Handles only an uncancelled block owned by the breaking player in the exact recorded world.
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block brokenBlock = event.getBlock();
        Location location = brokenBlock.getLocation();
        GeneratedBlockRecord record = plugin.getDatabaseManager().findBlockByLocation(location).orElse(null);
        if (record == null) {
            return;
        }

        if (!record.isOwnedBy(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLanguageManager().getMessage("not-block-owner"));
            return;
        }

        StageManager.PlayerStageProgress progress = plugin.getStageManager().incrementBlocksBroken(player);
        StageConfig stage = plugin.getStageManager().getCurrentStageConfig(player.getUniqueId());
        if (stage == null) {
            event.setCancelled(true);
            plugin.getStageManager().restoreProgress(player.getUniqueId(), record.stageFile(), record.blocksBroken());
            player.sendMessage(plugin.getLanguageManager().getMessage("stage-config-error"));
            return;
        }

        GenerationSelector.Selection selection = GenerationSelector.select(
                ThreadLocalRandom.current(),
                stage.entityChance,
                stage.chestChances
        );
        Object selectedBlock = plugin.getBlockGenerator().selectRandomBlock(progress.stageFile());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> replaceAfterBreak(
                player,
                location,
                record,
                progress,
                stage,
                selection,
                selectedBlock
        ), 1L);
    }

    /**
     * Applies the selected outcome after Bukkit has removed the original block.
     */
    private void replaceAfterBreak(
            Player player,
            Location location,
            GeneratedBlockRecord previousRecord,
            StageManager.PlayerStageProgress progress,
            StageConfig stage,
            GenerationSelector.Selection selection,
            Object selectedBlock
    ) {
        if (location.getBlock().getType() != Material.AIR) {
            plugin.getStageManager().restoreProgress(player.getUniqueId(), previousRecord.stageFile(), previousRecord.blocksBroken());
            plugin.debug("debug-position-not-empty", KaOneBlock.createDebugReplacements(location));
            return;
        }

        if (selection.type() == GenerationSelector.OutcomeType.CHEST) {
            placeChest(player, location, selection.chestConfig(), progress);
            return;
        }

        String actualBlockType = plugin.getBlockGenerator().placeGeneratedBlock(location.getBlock(), selectedBlock);
        plugin.getDatabaseManager().updateState(
                player.getUniqueId(),
                actualBlockType,
                progress.stageFile(),
                progress.blocksBroken()
        );
        plugin.getLogManager().logBlockReplacement(player.getName(), location, actualBlockType);
        plugin.debug("debug-replaced-block", KaOneBlock.createDebugReplacements(location, actualBlockType));

        if (selection.type() == GenerationSelector.OutcomeType.ENTITY && !stage.entityPack.isBlank()) {
            LivingEntity entity = plugin.getEntityManager().spawnEntity(location.clone().add(0.5, 1, 0.5), stage.entityPack);
            if (entity != null) {
                plugin.getLogManager().logEntityGeneration(player.getName(), location, entity.getType().name(), stage.entityPack);
                plugin.debug("debug-generated-entity", Map.of(
                        "world", location.getWorld() == null ? "unknown" : location.getWorld().getName(),
                        "x", String.valueOf(location.getBlockX()),
                        "y", String.valueOf(location.getBlockY()),
                        "z", String.valueOf(location.getBlockZ()),
                        "entityName", entity.getType().name(),
                        "entityPack", stage.entityPack
                ));
            }
        }

        sendDebugTransformation(player, actualBlockType);
    }

    /**
     * Places, records and fills a generated chest while preventing duplicate fill tasks.
     */
    private void placeChest(Player player, Location location, String chestConfig, StageManager.PlayerStageProgress progress) {
        BlockPosition position = BlockPosition.from(location);
        if (!pendingChests.add(position)) {
            return;
        }

        location.getBlock().setType(Material.CHEST);
        plugin.getDatabaseManager().updateState(
                player.getUniqueId(),
                "CHEST:" + chestConfig,
                progress.stageFile(),
                progress.blocksBroken()
        );
        plugin.getLogManager().logChestGeneration(player.getName(), location, chestConfig);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                if (location.getBlock().getState() instanceof Chest chest) {
                    plugin.getEnhancedChestManager().fillChest(chest, chestConfig);
                    plugin.debug("debug-generated-enhanced-chest", KaOneBlock.createDebugReplacements(location, null, chestConfig));
                }
            } finally {
                pendingChests.remove(position);
            }
        }, 1L);
        sendDebugTransformation(player, "CHEST");
    }

    /**
     * Sends the optional per-break debug result without doing work when debug mode is disabled.
     */
    private void sendDebugTransformation(Player player, String blockType) {
        if (!plugin.isDebugEnabled() || !player.hasPermission("kaoneblock.debug")) {
            return;
        }
        String formatted = blockType.regionMatches(true, 0, "ia:", 0, 3) ? blockType.substring(3) : blockType;
        player.sendMessage(plugin.getLanguageManager().getMessage("block-transformed", Map.of(
                "block", formatted.toLowerCase().replace('_', ' ')
        )));
    }
}
