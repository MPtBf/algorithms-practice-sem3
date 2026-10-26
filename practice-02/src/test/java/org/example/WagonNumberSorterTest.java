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
 * Тесты корректности сортировки номеров вагонов ({@link WagonNumberSorter}).
 *
 * <p>Домен: целые числа 0..9999. Проверяем примеры из задания, граничные значения,
 * дубликаты и случайные массивы разных размеров (результат сверяется с {@link Arrays#sort}).</p>
 */
class WagonNumberSorterTest {

    private final WagonNumberSorter sorter = new WagonNumberSorter();

    /** Первый пример из задания. */
    @Test
    void sortsExampleFromTask() {
        Integer[] array = {9999, 12, 100, 12};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{12, 12, 100, 9999}, array);
    }

    /** Второй пример из задания: уже отсортированный вход. */
    @Test
    void keepsAlreadySortedArray() {
        Integer[] array = {7, 70, 700};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{7, 70, 700}, array);
    }

    @Test
    void sortsEmptyArray() {
        Integer[] array = {};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{}, array);
    }

    @Test
    void sortsSingleElement() {
        Integer[] array = {42};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{42}, array);
    }

    @Test
    void sortsArrayWithDuplicates() {
        Integer[] array = {5, 5, 0, 9999, 5, 0, 9999, 1};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{0, 0, 1, 5, 5, 5, 9999, 9999}, array);
    }

    /** Минимальный и максимальный допустимые номера. */
    @Test
    void sortsBoundaryValues() {
        Integer[] array = {9999, 0, 5000, 0, 9999, 1, 9998};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{0, 0, 1, 5000, 9998, 9999, 9999}, array);
    }

    @Test
    void sortsReverseOrderedArray() {
        Integer[] array = {9999, 5000, 100, 7, 0};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{0, 7, 100, 5000, 9999}, array);
    }

    /** Все элементы одинаковы — проверка, что не теряются и не добавляются значения. */
    @Test
    void sortsArrayOfEqualValues() {
        Integer[] array = {12, 12, 12, 12};

        sorter.sort(array);

        assertArrayEquals(new Integer[]{12, 12, 12, 12}, array);
    }

    /** Случайные массивы разных размеров: результат должен совпасть с эталоном JDK. */
    @ParameterizedTest
    @MethodSource("randomSizes")
    void sortsRandomArrayLikeJdk(int size) {
        Random random = new Random(1000 + size);
        Integer[] array = new Integer[size];
        for (int i = 0; i < array.length; i++) {
            array[i] = random.nextInt(WagonNumberSorter.MAX_WAGON_NUMBER + 1);
        }
        Integer[] expected = array.clone();
        Arrays.sort(expected);

        sorter.sort(array);

        assertArrayEquals(expected, array);
    }

    /** {@code null} вместо массива — стандартный {@link NullPointerException}. */
    @Test
    void rejectsNullArray() {
        assertThrows(NullPointerException.class, () -> sorter.sort(null));
    }

    /** {@code null}-элемент нельзя развернуть в {@code int}. */
    @Test
    void rejectsNullElementInLargeArray() {
        assertThrows(NullPointerException.class, () -> sorter.sort(new Integer[]{0, null, 9999}));
    }

    /** Одиночный {@code null} не должен «проскакивать» через early return. */
    @Test
    void rejectsNullElementInSingleElementArray() {
        assertThrows(NullPointerException.class, () -> sorter.sort(new Integer[]{null}));
    }

    /** Значение вне домена 0..9999 недопустимо даже в массиве из одного элемента. */
    @ParameterizedTest
    @ValueSource(ints = {-1, -9999, 10_000, 10_001, 99_999, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsSingleValueOutsideDomain(int invalidValue) {
        assertThrows(IllegalArgumentException.class,
                () -> sorter.sort(new Integer[]{invalidValue}));
    }

    /** Значение вне домена недопустимо и рядом с валидными элементами. */
    @ParameterizedTest
    @ValueSource(ints = {-1, -9999, 10_000, 10_001, 99_999, Integer.MIN_VALUE, Integer.MAX_VALUE})
    void rejectsValueOutsideDomainAmongValidValues(int invalidValue) {
        assertThrows(IllegalArgumentException.class,
                () -> sorter.sort(new Integer[]{0, invalidValue, 9999}));
    }

    /** При отказе массив не должен быть частично переставлен. */
    @Test
    void doesNotChangeArrayWhenValuesAreInvalid() {
        Integer[] array = {9999, 10_000, 0};
        Integer[] original = array.clone();

        assertThrows(IllegalArgumentException.class, () -> sorter.sort(array));

        assertArrayEquals(original, array);
    }

    static Stream<Integer> randomSizes() {
        return Stream.of(0, 1, 2, 10, 100, 1_000, 10_000, 100_000);
    }
}
