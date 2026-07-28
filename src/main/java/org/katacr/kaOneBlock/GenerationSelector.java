package org.katacr.kaOneBlock;

import java.util.Map;
import java.util.random.RandomGenerator;

/**
 * Selects mutually exclusive entity, chest and normal-block outcomes from one cumulative roll.
 */
public final class GenerationSelector {
    private GenerationSelector() {
    }

    /**
     * Selects an outcome while preserving the configured iteration order of chest probabilities.
     */
    public static Selection select(RandomGenerator random, double entityChance, Map<String, Double> chestChances) {
        double roll = random.nextDouble();
        double cumulative = clamp(entityChance);
        if (roll < cumulative) {
            return new Selection(OutcomeType.ENTITY, null);
        }

        for (Map.Entry<String, Double> entry : chestChances.entrySet()) {
            cumulative = Math.min(1, cumulative + clamp(entry.getValue()));
            if (roll < cumulative) {
                return new Selection(OutcomeType.CHEST, entry.getKey());
            }
        }
        return new Selection(OutcomeType.BLOCK, null);
    }

    /**
     * Restricts a runtime probability to the valid finite range.
     */
    private static double clamp(double chance) {
        if (!Double.isFinite(chance)) {
            return 0;
        }
        return Math.max(0, Math.min(1, chance));
    }

    /**
     * Describes the mutually exclusive event selected for one block break.
     */
    public enum OutcomeType {
        ENTITY,
        CHEST,
        BLOCK
    }

    /**
     * Carries an outcome type and the selected chest configuration when applicable.
     */
    public record Selection(OutcomeType type, String chestConfig) {
    }
}
