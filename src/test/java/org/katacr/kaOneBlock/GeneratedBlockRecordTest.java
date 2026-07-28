package org.katacr.kaOneBlock;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies owner and world identity remain part of the authoritative block record.
 */
class GeneratedBlockRecordTest {
    /**
     * Confirms equal coordinates in different worlds remain distinct positions.
     */
    @Test
    void distinguishesWorldsAtEqualCoordinates() {
        UUID firstWorld = UUID.randomUUID();
        UUID secondWorld = UUID.randomUUID();
        assertFalse(new BlockPosition(firstWorld, 1, 64, 1).equals(new BlockPosition(secondWorld, 1, 64, 1)));
    }

    /**
     * Confirms ownership checks reject another player and state updates preserve identity.
     */
    @Test
    void preservesOwnerAndLocationAcrossStateUpdates() {
        UUID owner = UUID.randomUUID();
        BlockPosition position = new BlockPosition(UUID.randomUUID(), 1, 64, 1);
        GeneratedBlockRecord record = new GeneratedBlockRecord(owner, "Owner", position, "world", "STONE", "normal.yml", 3);
        GeneratedBlockRecord updated = record.withState("CHEST:common", "nether.yml", 0);

        assertTrue(updated.isOwnedBy(owner));
        assertFalse(updated.isOwnedBy(UUID.randomUUID()));
        assertEquals(position, updated.position());
        assertEquals(owner, updated.playerId());
    }
}
