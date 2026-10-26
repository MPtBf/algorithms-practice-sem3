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
 * Тесты корректности блочной сортировки нормированных расстояний
 * ({@link NormalizedDistanceSorter}).
 *
 * <p>Домен: {@code double} из интервала [0, 1). Проверяем примеры из задания,
 * границы интервала, дубликаты и случайные равномерно распределённые массивы
 * разных размеров (результат сверяется с {@link Arrays#sort}).</p>
 */
class NormalizedDistanceSorterTest {

    private final NormalizedDistanceSorter sorter = new NormalizedDistanceSorter();

    /** Первый пример из задания. */
    @Test
    void sortsExampleFromTask() {
        double[] values = {0.42, 0.11, 0.87, 0.35};

        sorter.sort(values);

        assertArrayEquals(new double[]{0.11, 0.35, 0.42, 0.87}, values);
    }

    /** Второй пример из задания. */
    @Test
    void sortsSecondExampleFromTask() {
        double[] values = {0.01, 0.99, 0.50};

        sorter.sort(values);

        assertArrayEquals(new double[]{0.01, 0.50, 0.99}, values);
    }

    @Test
    void sortsEmptyArray() {
        double[] values = {};

        sorter.sort(values);

        assertArrayEquals(new double[]{}, values);
    }

    @Test
    void sortsSingleElement() {
        double[] values = {0.5};

        sorter.sort(values);

        assertArrayEquals(new double[]{0.5}, values);
    }

    /** Левая граница интервала и значение, близкое к правой. */
    @Test
    void sortsBoundaryValues() {
        double[] values = {0.999999999999, 0.0, 0.5, 0.25};

        sorter.sort(values);

        assertArrayEquals(new double[]{0.0, 0.25, 0.5, 0.999999999999}, values);
    }

    @Test
    void sortsArrayWithDuplicates() {
        double[] values = {0.5, 0.1, 0.5, 0.1, 0.9};

        sorter.sort(values);

        assertArrayEquals(new double[]{0.1, 0.1, 0.5, 0.5, 0.9}, values);
    }

    @Test
    void sortsAlreadySortedArray() {
        double[] values = {0.01, 0.2, 0.35, 0.7, 0.99};

        sorter.sort(values);

        assertArrayEquals(new double[]{0.01, 0.2, 0.35, 0.7, 0.99}, values);
    }

    @Test
    void sortsReverseOrderedArray() {
        double[] values = {0.9, 0.7, 0.35, 0.2, 0.01};

        sorter.sort(values);

        assertArrayEquals(new double[]{0.01, 0.2, 0.35, 0.7, 0.9}, values);
    }

    @Test
    void sortsArrayOfEqualValues() {
        double[] values = {0.3, 0.3, 0.3, 0.3};

        sorter.sort(values);

        assertArrayEquals(new double[]{0.3, 0.3, 0.3, 0.3}, values);
    }

    /** Случайные равномерно распределённые значения: результат должен совпасть с эталоном JDK. */
    @ParameterizedTest
    @MethodSource("randomSizes")
    void sortsRandomArrayLikeJdk(int size) {
        Random random = new Random(4000 + size);
        double[] values = new double[size];
        for (int i = 0; i < values.length; i++) {
            values[i] = random.nextDouble();
        }
        double[] expected = values.clone();
        Arrays.sort(expected);

        sorter.sort(values);

        assertArrayEquals(expected, values);
    }

    /** {@code null} вместо массива — стандартный {@link NullPointerException}. */
    @Test
    void rejectsNullArray() {
        assertThrows(NullPointerException.class, () -> sorter.sort(null));
    }

    /** Всё, что не лежит в [0, 1), недопустимо: отрицательные, 1.0, NaN, бесконечности. */
    @ParameterizedTest
    @ValueSource(doubles = {
            -0.0001, -1.0, -Double.MAX_VALUE,
            1.0, 1.0000001, Double.MAX_VALUE,
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
    })
    void rejectsSingleValueOutsideDomain(double invalidValue) {
        assertThrows(IllegalArgumentException.class,
                () -> sorter.sort(new double[]{invalidValue}));
    }

    /** Недопустимое значение должно ловиться и в массиве вместе с валидными. */
    @ParameterizedTest
    @ValueSource(doubles = {
            -0.0001, -1.0, -Double.MAX_VALUE,
            1.0, 1.0000001, Double.MAX_VALUE,
            Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
    })
    void rejectsValueOutsideDomainAmongValidValues(double invalidValue) {
        assertThrows(IllegalArgumentException.class,
                () -> sorter.sort(new double[]{0.0, invalidValue, 0.5}));
    }

    /** При отказе массив не должен быть частично переставлен. */
    @Test
    void doesNotChangeArrayWhenValuesAreInvalid() {
        double[] values = {0.5, 1.0, 0.1};
        double[] original = values.clone();

        assertThrows(IllegalArgumentException.class, () -> sorter.sort(values));

        assertArrayEquals(original, values);
    }

    /** Левая граница и максимальное представимое значение меньше 1 — валидны. */
    @Test
    void acceptsIntervalBoundariesAndSmallestValue() {
        double[] values = {0.0, Math.nextDown(1.0), Double.MIN_VALUE, 0.5, 0.0};
        double[] expected = values.clone();
        Arrays.sort(expected);

        sorter.sort(values);

        assertArrayEquals(expected, values);
    }

    /** {@code -0.0} формально входит в [0, 1); порядок должен совпадать с {@link Arrays#sort}. */
    @Test
    void treatsNegativeZeroAsValidValue() {
        double[] values = {0.0, -0.0};
        double[] expected = values.clone();
        Arrays.sort(expected);

        sorter.sort(values);

        assertArrayEquals(expected, values);
    }

    static Stream<Integer> randomSizes() {
        return Stream.of(0, 1, 2, 10, 100, 1_000, 10_000, 100_000);
    }
}
