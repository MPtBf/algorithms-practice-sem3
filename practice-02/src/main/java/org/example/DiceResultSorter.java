package org.example;

/**
 * Сортировка результатов броска кубика (целые числа от 1 до 6) и подсчёт повторов.
 *
 * <p>Диапазон значений фиксирован и очень узкий, поэтому подходит сортировка
 * подсчётом (counting sort): время O(n + k), дополнительная память O(k), где k = 6.</p>
 */
public class DiceResultSorter implements Sorter<Integer> {

    /** Минимальное значение на кубике. */
    public static final int MIN_VALUE = 1;

    /** Максимальное значение на кубике. */
    public static final int MAX_VALUE = 6;

    /**
     * Сортирует результаты бросков по возрастанию на месте.
     *
     * @param array результаты бросков, каждое значение в диапазоне 1..6
     */
    @Override
    public void sort(Integer[] array) {
        // edge case
        validateValuesRange(array);
        // frequencies
        int[] frequencies = countOccurrencesUnchecked(array);
        // backmap
        int next = 0;
        for (int i = 0; i < MAX_VALUE + 1; i++) {
            int count = frequencies[i];
            for (int j = 0; j < count; j++) {
                array[next] = i;
                next++;
            }
        }
    }

    private void validateValuesRange(Integer[] array) {
        if (array.length < 1) {
            return;
        }
        for (int value : array) {
            if (value < MIN_VALUE || value > MAX_VALUE) {
                throw new IllegalArgumentException("value should be 1 <= value <= 6");
            }
        }
    }

    /**
     * Считает, сколько раз встречается каждое значение.
     *
     * <p>Исходный массив не изменяется.</p>
     *
     * @param array результаты бросков, каждое значение в диапазоне 1..6
     * @return массив длины 7, где {@code result[v]} — количество значений {@code v};
     *         элемент с индексом 0 не используется и всегда равен 0
     */
    public int[] countOccurrences(Integer[] array) {
        // edge case
        validateValuesRange(array);

        return countOccurrencesUnchecked(array);
    }
    private int[] countOccurrencesUnchecked(Integer[] array) {
        // count
        int[] frequencies = new int[MAX_VALUE + 1];
        for (int value : array) {
            frequencies[value]++;
        }
        return frequencies;
    }
}
