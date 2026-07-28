package org.katacr.kaOneBlock;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies cumulative mutually exclusive generation outcomes.
 */
class GenerationSelectorTest {
    /**
     * Confirms entity, each chest tier and normal blocks occupy adjacent configured ranges.
     */
    @Test
    void selectsCumulativeRangesInConfigurationOrder() {
        Map<String, Double> chests = new LinkedHashMap<>();
        chests.put("common", 0.03);
        chests.put("advanced", 0.01);

        assertEquals(GenerationSelector.OutcomeType.ENTITY, select(0.02, chests).type());
        assertEquals("common", select(0.06, chests).chestConfig());
        assertEquals("advanced", select(0.085, chests).chestConfig());
        assertEquals(GenerationSelector.OutcomeType.BLOCK, select(0.10, chests).type());
    }

    /**
     * Confirms invalid runtime probabilities cannot make cumulative selection exceed its valid range.
     */
    @Test
    void clampsInvalidProbabilities() {
        assertEquals(
                GenerationSelector.OutcomeType.ENTITY,
                GenerationSelector.select(new FixedDoubleRandom(0.5), 2.0, Map.of()).type()
        );
        assertEquals(
                GenerationSelector.OutcomeType.BLOCK,
                GenerationSelector.select(new FixedDoubleRandom(0.5), Double.NaN, Map.of("bad", -1.0)).type()
        );
    }

    /**
     * Runs one selection with a fixed reproducible random double.
     */
    private GenerationSelector.Selection select(double roll, Map<String, Double> chests) {
        return GenerationSelector.select(new FixedDoubleRandom(roll), 0.05, chests);
    }

    /**
     * Supplies an exact random double without coupling tests to a seed implementation.
     */
    private static final class FixedDoubleRandom extends Random {
        private final double value;

        private FixedDoubleRandom(double value) {
            this.value = value;
        }

        @Override
        public double nextDouble() {
            return value;
        }
    }
}

