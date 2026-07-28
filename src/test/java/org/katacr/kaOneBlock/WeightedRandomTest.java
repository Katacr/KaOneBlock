package org.katacr.kaOneBlock;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies the weighted selector's boundary behavior independently of Bukkit.
 */
class WeightedRandomTest {
    /**
     * Confirms non-positive entries are ignored and a valid sole entry is always returned.
     */
    @Test
    void ignoresNonPositiveWeights() {
        WeightedRandom<String> random = new WeightedRandom<>();
        random.add("negative", -1);
        random.add("zero", 0);
        random.add("nan", Double.NaN);
        random.add("infinite", Double.POSITIVE_INFINITY);
        random.add("valid", 1);
        for (int iteration = 0; iteration < 100; iteration++) {
            assertEquals("valid", random.getRandom());
        }
    }
}
