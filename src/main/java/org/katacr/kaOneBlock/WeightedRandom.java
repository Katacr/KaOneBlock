package org.katacr.kaOneBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 权重随机选择器
 * 用于根据权重随机选择元素
 */
public class WeightedRandom<T> {
    private final List<Entry<T>> entries = new ArrayList<>();
    private final Random random = new Random();
    private double totalWeight = 0;

    /**
     * 添加带权重的元素
     *
     * @param item   元素
     * @param weight 权重
     */
    public void add(T item, double weight) {
        if (weight <= 0) return;
        totalWeight += weight;
        entries.add(new Entry<>(item, totalWeight));
    }

    /**
     * 随机选择一个元素
     *
     * @return 随机选择的元素
     */
    public T getRandom() {
        if (entries.isEmpty()) return null;
        double value = random.nextDouble() * totalWeight;
        for (Entry<T> entry : entries) {
            if (value < entry.cumulativeWeight) {
                return entry.item;
            }
        }
        return entries.get(entries.size() - 1).item;
    }

    /**
     * 内部类：权重条目
     *
     * @param <T> 元素类型
     */
    private static class Entry<T> {
        final T item;
        final double cumulativeWeight;

        Entry(T item, double cumulativeWeight) {
            this.item = item;
            this.cumulativeWeight = cumulativeWeight;
        }
    }
}