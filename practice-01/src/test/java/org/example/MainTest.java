package org.example;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Random;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты корректности QuickSort ({@link Main#quickSort(int[])}).
 *
 * <p>Сложность в practice-01 измеряется отдельно ({@link Benchmark}), здесь мы проверяем
 * только то, что алгоритм действительно сортирует — на всех видах входа, включая
 * вырожденный из {@link WorstCaseBuilder}.</p>
 *
 * <p>Параметризованные тесты гоняются для N = 2^3 … 2^12 (см. {@link #testQuickSortData()}).</p>
 */
class MainTest {
    /** Базовый смоук: точное ожидаемое содержимое массива после сортировки (включая дубликаты). */
    @Test
    void testQuickSortArrayFromMain() {
        int[] numbers = {1, 2, 10, 1, 9, 7, 8};

        Main.quickSort(numbers);

        assertArrayEquals(new int[]{1, 1, 2, 7, 8, 9, 10}, numbers);
    }

    /** Случайный вход: после сортировки каждая пара соседей должна удовлетворять a[i] <= a[i+1]. */
    @ParameterizedTest
    @MethodSource("testQuickSortData")
    void testQuickSortUnsorted(int N) {
        Random random = new Random();
        int[] arr = new int[N];
        for (int i = 0; i < N; i++) arr[i] = random.nextInt(10000);

        Main.quickSort(arr);

        for (int i = 0; i < arr.length - 1; i++) {
            Assertions.assertTrue(arr[i] <= arr[i + 1]);
        }
    }

    /** Уже отсортированный вход — не должен ломаться (и по времени это «лучший случай» бенчмарка). */
    @ParameterizedTest
    @MethodSource("testQuickSortData")
    void testQuickSortSorted(int N) {
        int[] arr = new int[N];
        for (int i = 0; i < N; i++) arr[i] = i;

        Main.quickSort(arr);

        for (int i = 0; i < arr.length - 1; i++) {
            Assertions.assertTrue(arr[i] <= arr[i + 1]);
        }
    }

    /** Обратный порядок (N, N-1, ..., 1) — классический стресс-тест для рекурсии и обменов. */
    @ParameterizedTest
    @MethodSource("testQuickSortData")
    void testQuickSortReverse(int N) {
        int[] arr = new int[N];
        for (int i = 0; i < N; i++) arr[i] = N-i;

        Main.quickSort(arr);

        for (int i = 0; i < arr.length - 1; i++) {
            Assertions.assertTrue(arr[i] <= arr[i + 1]);
        }
    }

    /** Эталонная сортировка JDK: смоук-тест окружения (контроль, что сами проверки выше имеют смысл). */
    @ParameterizedTest
    @MethodSource("testQuickSortData")
    void testBuiltinUnsorted(int N) {
        Random random = new Random();
        int[] arr = new int[N];
        for (int i = 0; i < N; i++) arr[i] = random.nextInt(10000);

        Arrays.sort(arr);

        for (int i = 0; i < arr.length - 1; i++) {
            Assertions.assertTrue(arr[i] <= arr[i + 1]);
        }
    }

    /** Вырожденный вход из {@link WorstCaseBuilder}: корректность на худшем по сложности сценарии. */
    @ParameterizedTest
    @MethodSource("testQuickSortData")
    void testQuickSortWorstCaseInput(int N) {
        int[] arr = WorstCaseBuilder.build(N);

        Main.quickSort(arr);

        for (int i = 0; i < arr.length - 1; i++) {
            Assertions.assertTrue(arr[i] <= arr[i + 1]);
        }
    }

    /** Набор размеров N для параметризованных тестов: степени двойки 2^3 … 2^12. */
    static Stream<Arguments> testQuickSortData() {
        return Stream.of(
                Arguments.of((int) Math.pow(2, 3)),
                Arguments.of((int) Math.pow(2, 4)),
                Arguments.of((int) Math.pow(2, 5)),
                Arguments.of((int) Math.pow(2, 6)),
                Arguments.of((int) Math.pow(2, 7)),
                Arguments.of((int) Math.pow(2, 8)),
                Arguments.of((int) Math.pow(2, 9)),
                Arguments.of((int) Math.pow(2, 10)),
                Arguments.of((int) Math.pow(2, 11)),
                Arguments.of((int) Math.pow(2, 12))
        );
    }
}
