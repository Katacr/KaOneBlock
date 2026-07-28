package org.katacr.kaOneBlock;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifies one block position without conflating equal coordinates in different worlds.
 */
public record BlockPosition(UUID worldId, int x, int y, int z) {
    /**
     * Creates a block position from a Bukkit location whose world must be available.
     */
    public static BlockPosition from(Location location) {
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        return new BlockPosition(world.getUID(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    /**
     * Converts this position to a location after verifying the supplied world.
     */
    public Location toLocation(World world) {
        if (!world.getUID().equals(worldId)) {
            throw new IllegalArgumentException("World does not match block position");
        }
        return new Location(world, x, y, z);
    }
}

