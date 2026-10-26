package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты корректности сортировки результатов броска кубика ({@link DiceResultSorter})
 * и подсчёта повторов.
 *
 * <p>Домен: целые числа 1..6. Проверяем примеры из задания, границы диапазона,
 * дубликаты, случайные массивы и корректность частот.</p>
 */
class DiceResultSorterTest {

    private final DiceResultSorter sorter = new DiceResultSorter();

    /** Первый пример из задания: сортировка. */
    @Test
    void sortsExampleFromTask() {
        Integer[] array = {6, 1, 6, 3, 2};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{1, 2, 3, 6, 6}, array);
    }

    /** Второй пример из задания: сортировка. */
    @Test
    void sortsSecondExampleFromTask() {
        Integer[] array = {4, 4, 2, 5};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{2, 4, 4, 5}, array);
    }

    /** Первый пример из задания: частоты. */
    @Test
    void countsExampleFromTask() {
        Integer[] array = {6, 1, 6, 3, 2};

        int[] counts = sorter.countOccurrences(array);

        // counts[0] не используется; counts[v] — сколько раз выпало v
        assertArrayEquals(new int[]{0, 1, 1, 1, 0, 0, 2}, counts);
    }

    /** Второй пример из задания: частоты. */
    @Test
    void countsSecondExampleFromTask() {
        Integer[] array = {4, 4, 2, 5};

        int[] counts = sorter.countOccurrences(array);

        assertArrayEquals(new int[]{0, 0, 1, 0, 2, 1, 0}, counts);
    }

    /** Подсчёт частот не должен менять исходный массив. */
    @Test
    void countOccurrencesDoesNotModifyInput() {
        Integer[] array = {6, 1, 6, 3, 2};
        Integer[] original = array.clone();

        sorter.countOccurrences(array);

        assertArrayEquals(original, array);
    }

    @Test
    void countsEmptyArray() {
        int[] counts = sorter.countOccurrences(new Integer[]{});

        assertArrayEquals(new int[]{0, 0, 0, 0, 0, 0, 0}, counts);
    }

    @Test
    void sortsEmptyArray() {
        Integer[] array = {};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{}, array);
    }

    @Test
    void sortsSingleElement() {
        Integer[] array = {5};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{5}, array);
    }

    @Test
    void sortsAllOnes() {
        Integer[] array = {1, 1, 1, 1};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{1, 1, 1, 1}, array);
    }

    @Test
    void sortsAllSixes() {
        Integer[] array = {6, 6, 6};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{6, 6, 6}, array);
    }

    @Test
    void sortsArrayWithEveryFace() {
        Integer[] array = {6, 5, 4, 3, 2, 1};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{1, 2, 3, 4, 5, 6}, array);
    }

    /** Случайные массивы разных размеров: результат должен совпасть с эталоном JDK. */
    @ParameterizedTest
    @MethodSource("randomSizes")
    void sortsRandomArrayLikeJdk(int size) {
        Random random = new Random(2000 + size);
        Integer[] array = new Integer[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = DiceResultSorter.MIN_VALUE
                    + random.nextInt(DiceResultSorter.MAX_VALUE - DiceResultSorter.MIN_VALUE + 1);
        }
        Integer[] expected = array.clone();
        Arrays.sort(expected);

        sorter.sort(array);

        assertArrayEquals(expected, array);
    }

    /** Сумма частот должна равняться длине массива, а частоты — совпадать с эталоном. */
    @ParameterizedTest
    @MethodSource("randomSizes")
    void countsRandomArrayLikeJdk(int size) {
        Random random = new Random(3000 + size);
        Integer[] array = new Integer[size];
        int[] expected = new int[7];
        for (int i = 0; i < array.length; i++) {
            array[i] = DiceResultSorter.MIN_VALUE
                    + random.nextInt(DiceResultSorter.MAX_VALUE - DiceResultSorter.MIN_VALUE + 1);
            expected[array[i]]++;
        }

        int[] counts = sorter.countOccurrences(array);

        assertArrayEquals(expected, counts);
    }

    /** {@code null} вместо массива — стандартный {@link NullPointerException}. */
    @Test
    void rejectsNullArray() {
        assertThrows(NullPointerException.class, () -> sorter.sort(null));
        assertThrows(NullPointerException.class, () -> sorter.countOccurrences(null));
    }

    /** {@code null}-элемент нельзя развернуть в {@code int}. */
    @Test
    void rejectsNullElementInLargeArray() {
        assertThrows(NullPointerException.class, () -> sorter.sort(new Integer[]{1, null, 6}));
    }

    /** Одиночный {@code null} не должен «проскакивать» через early return. */
    @Test
    void rejectsNullElementInSingleElementArray() {
        assertThrows(NullPointerException.class, () -> sorter.sort(new Integer[]{null}));
    }

    @Test
    void rejectsNullElementInCount() {
        assertThrows(NullPointerException.class, () -> sorter.countOccurrences(new Integer[]{null}));
    }

    /** Значение вне домена 1..6 недопустимо даже в массиве из одного элемента. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 7, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsSingleValueOutsideDomainOnSort(int invalidValue) {
        assertThrows(IllegalArgumentException.class,
                () -> sorter.sort(new Integer[]{invalidValue}));
    }

    /** Значение вне домена недопустимо и рядом с валидными элементами. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 7, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsValueOutsideDomainAmongValidValuesOnSort(int invalidValue) {
        assertThrows(IllegalArgumentException.class,
                () -> sorter.sort(new Integer[]{1, invalidValue, 6}));
    }

    /** Подсчёт частот обязан проверять домен так же строго, как сортировка. */
    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 7, 100, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsValueOutsideDomainOnCount(int invalidValue) {
        assertThrows(IllegalArgumentException.class,
                () -> sorter.countOccurrences(new Integer[]{invalidValue}));
        assertThrows(IllegalArgumentException.class,
                () -> sorter.countOccurrences(new Integer[]{1, invalidValue, 6}));
    }

    /** При отказе массив не должен быть частично переставлен. */
    @Test
    void doesNotChangeArrayWhenValuesAreInvalid() {
        Integer[] array = {6, 7, 1};
        Integer[] original = array.clone();

        assertThrows(IllegalArgumentException.class, () -> sorter.sort(array));

        assertArrayEquals(original, array);
    }

    static Stream<Integer> randomSizes() {
        return Stream.of(0, 1, 2, 10, 100, 1_000, 10_000, 100_000);
    }
}
