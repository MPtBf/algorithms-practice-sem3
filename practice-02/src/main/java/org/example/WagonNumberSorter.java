package org.example;

/**
 * Сортировка номеров вагонов (целые числа от 0 до 9999) по возрастанию.
 *
 * <p>Ограничения задачи: значения лежат в узком фиксированном диапазоне,
 * количество элементов велико. Подходящий алгоритм — поразрядная сортировка (radix sort)
 * O(d \cdot (n + b))  дополнительная память O(n + b), где: n — количество вагонов;
 * d = 4 — число разрядов (0..9999); b = 10 — основание системы счисления, столько
 * счётчиков на одном проходе.
 */
public class WagonNumberSorter implements Sorter<Integer> {

    /** Максимальный номер вагона из условия задачи. */
    public static final int MAX_WAGON_NUMBER = 9999;

    /**
     * Сортирует массив номеров вагонов по возрастанию на месте.
     *
     * @param array номера вагонов, каждое значение в диапазоне 0..9999
     */
    @Override
    public void sort(Integer[] array) {
        // edge case
        if (array.length < 1){
            return;
        }
        for (int value : array) {
            if (value < 0 || value > MAX_WAGON_NUMBER) {
                throw new IllegalArgumentException("only allowing 0 <= value <= 9_999");
            }
        }
        // call sort for every digit
        Integer[] src = array;
        Integer[] dst = new Integer[array.length];
        Integer[] buffer;

        for (int digitPlace = 1; digitPlace <= MAX_WAGON_NUMBER; digitPlace *= 10) {
            sortByDigit(digitPlace, src, dst);
            // swap
            buffer = src;
            src = dst;
            dst = buffer;
            // int overflow safe
            if (Integer.MAX_VALUE / 10 < digitPlace) {
                break;
            }
        }
        if (dst != src) {
            System.arraycopy(src, 0, array, 0, array.length);
        }
    }

    private void sortByDigit(int digitPlace, Integer[] array, Integer[] buffer) {
        // init
        int[] counts = new int[10];
        // count digits
        for (int value : array) {
            int digit = (value / digitPlace) % 10;
            counts[digit]++;
        }
        // convert to places
        for (int i = 1; i < counts.length; i++) {
            counts[i] += counts[i - 1];
        }
        // rearrange
        for (int i = array.length - 1; i >= 0; i--) {
            int value = array[i];
            int digit = (value / digitPlace) % 10;
            int index = counts[digit] - 1;
            buffer[index] = value;
            counts[digit] --;
        }
    }
}
