package org.katacr.kaOneBlock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies safe normalization of administrator- and configuration-provided stage names.
 */
class StageConfigManagerTest {
    /**
     * Confirms safe basenames receive exactly one YAML suffix.
     */
    @Test
    void normalizesSafeStageNames() {
        assertEquals("normal.yml", StageConfigManager.normalizeStageFile("normal"));
        assertEquals("custom-stage_2.yml", StageConfigManager.normalizeStageFile("custom-stage_2.yml"));
    }

    /**
     * Confirms traversal, absolute paths and blank values are rejected.
     */
    @Test
    void rejectsUnsafeStageNames() {
        assertThrows(IllegalArgumentException.class, () -> StageConfigManager.normalizeStageFile("../config"));
        assertThrows(IllegalArgumentException.class, () -> StageConfigManager.normalizeStageFile("/tmp/stage"));
        assertThrows(IllegalArgumentException.class, () -> StageConfigManager.normalizeStageFile(""));
    }
}

