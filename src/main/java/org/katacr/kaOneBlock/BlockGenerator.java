package org.katacr.kaOneBlock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Creates, replaces and safely removes the physical block represented by a database record.
 */
public class BlockGenerator {
    private final KaOneBlock plugin;

    public BlockGenerator(KaOneBlock plugin) {
        this.plugin = plugin;
    }

    /**
     * Generates a player's initial block below their feet and persists its world-aware record.
     */
    public boolean generateBlockAtPlayerLocation(Player player) {
        if (plugin.getDatabaseManager().hasBlock(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("already-generated"));
            return false;
        }

        StageManager.PlayerStageProgress progress = plugin.getStageManager().initPlayerProgress(player);
        Location blockLocation = player.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation();
        Block targetBlock = blockLocation.getBlock();
        if (targetBlock.getType() != Material.AIR) {
            String locationInfo = String.format("(%d, %d, %d)", blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ());
            player.sendMessage(plugin.getLanguageManager().getMessage("position-not-air", Map.of(
                    "location", locationInfo,
                    "block", plugin.formatMaterialName(targetBlock.getType())
            )));
            return false;
        }

        Object selectedBlock = selectRandomBlock(progress.stageFile());
        String actualBlockType = placeGeneratedBlock(targetBlock, selectedBlock);
        World world = blockLocation.getWorld();
        if (world == null) {
            targetBlock.setType(Material.AIR);
            return false;
        }

        GeneratedBlockRecord record = new GeneratedBlockRecord(
                player.getUniqueId(),
                player.getName(),
                BlockPosition.from(blockLocation),
                world.getName(),
                actualBlockType,
                progress.stageFile(),
                progress.blocksBroken()
        );
        if (!plugin.getDatabaseManager().createBlock(record)) {
            targetBlock.setType(Material.AIR);
            player.sendMessage(plugin.getLanguageManager().getMessage("database-error"));
            return false;
        }

        plugin.getLogManager().logBlockGeneration(player.getName(), blockLocation, actualBlockType);
        player.sendMessage(plugin.getLanguageManager().getMessage("block-generated", Map.of(
                "block", formatBlockType(actualBlockType)
        )));
        plugin.debug("debug-generated-block", KaOneBlock.createDebugReplacements(blockLocation, actualBlockType));
        return true;
    }

    /**
     * Selects a configured stage block and falls back to stone for an empty or invalid pool.
     */
    public Object selectRandomBlock(String stageFile) {
        WeightedRandom<Object> blockList = plugin.getBlockListManager().getBlockList(stageFile);
        if (blockList == null) {
            return Material.STONE;
        }
        Object selected = blockList.getRandom();
        return selected == null ? Material.STONE : selected;
    }

    /**
     * Places a vanilla or ItemsAdder block and returns the actual persisted type.
     */
    public String placeGeneratedBlock(Block targetBlock, Object selectedBlock) {
        if (selectedBlock instanceof Material material) {
            targetBlock.setType(material);
            return material.name();
        }

        if (selectedBlock instanceof String blockId && blockId.regionMatches(true, 0, "ia:", 0, 3)) {
            String namespacedId = blockId.substring(3);
            if (plugin.getItemsAdderManager().placeBlock(targetBlock.getLocation(), namespacedId)) {
                return "ia:" + namespacedId;
            }
        }

        targetBlock.setType(Material.STONE);
        return Material.STONE.name();
    }

    /**
     * Removes the block from its recorded world only after its database record is deleted.
     */
    public void removePlayerBlock(Player player) {
        GeneratedBlockRecord record = plugin.getDatabaseManager().findBlockByPlayer(player.getUniqueId()).orElse(null);
        if (record == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("no-blocks"));
            return;
        }

        World world = Bukkit.getWorld(record.position().worldId());
        if (world == null) {
            player.sendMessage(plugin.getLanguageManager().getMessage("block-world-unavailable", Map.of(
                    "world", record.worldName()
            )));
            return;
        }

        if (!plugin.getDatabaseManager().deleteBlock(player.getUniqueId())) {
            player.sendMessage(plugin.getLanguageManager().getMessage("remove-failed"));
            return;
        }

        record.position().toLocation(world).getBlock().setType(Material.AIR);
        plugin.getStageManager().clearPlayerProgress(player.getUniqueId());
        player.sendMessage(plugin.getLanguageManager().getMessage("block-removed"));
    }

    /**
     * Formats a persisted vanilla or ItemsAdder identifier for player-facing messages.
     */
    private String formatBlockType(String blockType) {
        String normalized = blockType.regionMatches(true, 0, "ia:", 0, 3) ? blockType.substring(3) : blockType;
        return normalized.toLowerCase().replace('_', ' ');
    }
}
