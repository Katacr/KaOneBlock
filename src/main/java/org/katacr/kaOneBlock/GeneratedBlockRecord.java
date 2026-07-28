package org.katacr.kaOneBlock;

import java.util.UUID;

/**
 * Stores the authoritative owner, location, generated content and stage state for one OneBlock.
 */
public record GeneratedBlockRecord(
        UUID playerId,
        String playerName,
        BlockPosition position,
        String worldName,
        String blockType,
        String stageFile,
        int blocksBroken
) {
    /**
     * Reports whether the supplied player UUID is the authoritative owner.
     */
    public boolean isOwnedBy(UUID candidatePlayerId) {
        return playerId.equals(candidatePlayerId);
    }

    /**
     * Returns an updated immutable record while preserving its owner and location.
     */
    public GeneratedBlockRecord withState(String newBlockType, String newStageFile, int newBlocksBroken) {
        return new GeneratedBlockRecord(playerId, playerName, position, worldName, newBlockType, newStageFile, newBlocksBroken);
    }
}
