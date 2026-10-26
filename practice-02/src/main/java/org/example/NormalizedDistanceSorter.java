package org.example;

import java.util.*;

/**
 * Сортировка нормированных расстояний — {@code double} из интервала [0, 1).
 *
 * <p>Значения примерно равномерно распределены, поэтому подходит блочная
 * сортировка (bucket sort): массив разбивается на интервалы, элементы
 * раскладываются по корзинам, каждая корзина сортируется отдельно.
 * expected O(n) при равномерном распределении, worst O(n log n), память O(n).</p>
 */
public class NormalizedDistanceSorter {

    private final double MAX_VALUE = 1.0;

    /**
     * Сортирует значения по возрастанию на месте.
     *
     * @param values расстояния, каждое значение в интервале [0, 1)
     */
    public void sort(double[] values) {
        // edge case
        if (values.length < 1){
            return;
        }
        for (double value : values){
            if (value < 0 || value >= 1 || Double.isNaN(value)) {
                throw new IllegalArgumentException("values should be in [0; 1) range");
            }
        }
        // init buckets with pre-calculated length
        int bucketsNumber = Math.max(1, values.length / 8);

        int[] counts = new int[bucketsNumber];
        for (double value : values) {
            counts[bucketIndex(value, bucketsNumber)] ++;
        }
        double[][] buckets = new double[bucketsNumber][];
        for (int i = 0; i < bucketsNumber; i++) {
            buckets[i] = new double[counts[i]];
        }
        // place numbers
        int[] positions = new int[bucketsNumber]; // zeros
        for (double value : values) {
            int index = bucketIndex(value, bucketsNumber);
            buckets[index][positions[index]] = value;
            positions[index]++;
        }

        // sort and merge buckets
        int next = 0;
        for (double[] bucket : buckets) {
            Arrays.sort(bucket);
            for (double value : bucket) {
                values[next++] = value;
            }
        }
    }
    private int bucketIndex(double value, int bucketsNumber) {
        return (int) (value * bucketsNumber); // [0; 1] already
    }
}
