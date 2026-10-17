package org.example;

import java.io.File;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Экспериментальное измерение сложности QuickSort (см. {@link Main#quickSort(int[])}).
 *
 * <p>Идея: для каждого размера входа N многократно сортируем массив и усредняем время,
 * затем кладём результат в CSV. По этим данным строится график (plot.py) и сравниваются
 * теоретические кривые {@code N·log2(N)} (средний/лучший случай) и {@code N²} (худший случай).</p>
 *
 * <p>Три сценария входа:</p>
 * <ul>
 *   <li><b>sorted</b> — уже отсортированный массив [0..N-1]; опорный элемент берётся из середины,
 *       поэтому разбиения сбалансированы и сложность остаётся O(N·log N);</li>
 *   <li><b>random</b> — случайные числа с фиксированным seed: типичный, «средний» вход;</li>
 *   <li><b>worst_case</b> — специально сконструированная перестановка из {@link WorstCaseBuilder},
 *       провоцирующая вырожденные разбиения => O(N²) и глубину рекурсии ~N.</li>
 * </ul>
 *
 * <p>Корректность методики:</p>
 * <ul>
 *   <li>JIT-прогрев (WARMUP подходов на каждую точку + общий прогрев до всех замеров);</li>
 *   <li>усреднение по RUNS независимым прогонам;</li>
 *   <li>фиксированный seed => результаты воспроизводимы от запуска к запуску;</li>
 *   <li>проверка отсортированности после прогрева — защита от «слишком быстрого» результата,
 *       если бы алгоритм вдруг не сортировал;</li>
 *   <li>худший случай гоняется в отдельном потоке с большим стеком: глубина рекурсии ~N
 *       иначе вызывает StackOverflowError.</li>
 * </ul>
 *
 * <p>Запуск: {@code mvn -pl practice-01 exec:exec} (из корня репозитория).
 * Выход: {@code practice-01/results/benchmark_results.csv}.</p>
 */
public class Benchmark {
    /** Сколько замеров усредняется для каждой точки N (больше => стабильнее, но дольше). */
    private static final int RUNS = 30;
    /** Сколько «тренировочных» проходов делается перед замером: даёт JIT остыть и оптимизировать код. */
    private static final int WARMUP = 3;
    /** Фиксированный seed генератора случайных чисел — воспроизводимость «случайного» сценария. */
    private static final long SEED = 20261003L;
    /** Минимальный экспонент: стартуем с N = 2^10 = 1024 (меньше — шум замеров не отличить от сигнала). */
    private static final int MIN_EXP = 10;
    /** Максимальный экспонент для sorted/random: до N = 2^23 (~8.4 млн элементов). */
    private static final int DEFAULT_MAX_EXP = 23;
    /** Максимальный экспонент для worst_case: до N = 2^16 — квадратичная сложность, большие N слишком долгие. */
    private static final int DEFAULT_WORST_MAX_EXP = 16;
    /** Размер стека потока для худший-случая: глубина рекурсии ~N (65536 кадров) требует больше стека. */
    private static final long WORST_CASE_STACK = 512L * 1024 * 1024;

    /**
     * Точка входа.
     *
     * @param args необязательно: [0]=число замеров RUNS, [1]=макс. экспонента (sorted/random),
     *             [2]=макс. экспонента (worst_case); по умолчанию 30, 23, 16
     * @throws Exception при ошибке записи CSV
     */
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT); // десятичный разделитель — точка: иначе printf может писать запятую в CSV

        // Аргументы командной строки позволяют ускорить прогон (например, для отладки: mvn exec:exec -Dexec.args="3 12 10")
        int runs = args.length > 0 ? Integer.parseInt(args[0]) : RUNS;
        int maxExp = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_MAX_EXP;
        int worstMaxExp = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_WORST_MAX_EXP;

        globalWarmup(); // общий прогрев JVM: прогоняем крупные массивы, чтобы JIT «увидел» горячий код quickSort

        // Результат пишем в results/ внутри каталога модуля (workingDirectory задана в pom.xml)
        File resultsDir = new File("results");
        if (!resultsDir.exists() && !resultsDir.mkdirs()) {
            throw new IllegalStateException("cannot create results directory");
        }
        File csv = new File(resultsDir, "benchmark_results.csv");
        try (PrintWriter out = new PrintWriter(csv, "UTF-8")) {
            // Заголовок CSV: сценарий, размер массива, среднее время в наносекундах
            // и два «эталонных» столбца с теоретическими функциями (для справки/плоттера)
            out.println("scenario,N,mean_ns,n_log_n,n_sq");
            runScenario(out, "sorted", maxExp, runs);    // лучший/типичный вход: уже отсортирован
            runScenario(out, "random", maxExp, runs);    // средний вход: случайные данные
            runScenario(out, "worst_case", worstMaxExp, runs); // худший вход: вырожденные разбиения
        }
        System.out.println(csv.getPath() + " ready"); // сообщает путь к готовому CSV (плоттер читает его)
    }

    /**
     * Прогоняет один сценарий для всех N = 2^MIN_EXP … 2^maxExp и дописывает строки в CSV.
     *
     * @param out      куда писать результат (файл открыт в {@link #main})
     * @param scenario имя сценария: sorted | random | worst_case
     * @param maxExp   максимальный экспонент размера массива
     * @param runs     число усредняемых замеров на каждую точку
     */
    private static void runScenario(PrintWriter out, String scenario, int maxExp, int runs) throws Exception {
        for (int e = MIN_EXP; e <= maxExp; e++) {
            int n = 1 << e; // n = 2^e: степени двойки дают «ровную» логарифмическую шкалу на графике

            // Диспетчеризация по имени сценария (см. measureScenario): неизвестное имя — ошибка,
            // а не молчаливый переход на худший случай — опечатка не должна испортить эксперимент незаметно
            double mean = measureScenario(scenario, n, runs);

            // Теоретические функции сложности в тех же единицах, что и mean (наносекунды):
            // n·log2(n) — ожидаемая кривая для sorted/random, n² — для worst_case
            double nLogN = n * (Math.log(n) / Math.log(2));
            out.printf("%s,%d,%.0f,%.4f,%.4f%n", scenario, n, mean, nLogN, (double) n * n);
            out.flush(); // пишем каждую строку сразу: при падении процесса накопленные данные не потеряются
            System.out.printf("%-10s N=%-9d mean=%.3f ms%n", scenario, n, mean / 1e6); // нс -> мс для человека
        }
    }

    /**
     * Диспетчер сценариев: по имени выбирает, как получать входные массивы для одного размера N,
     * и запускает единый шаблон замера {@link #measure}. Это ЕДИНСТВЕННОЕ место со switch —
     * новый сценарий добавляется только здесь, а цикл «прогрев + замеры» нигде не дублируется.
     *
     * <p>Неизвестное имя сценария — явная {@link IllegalArgumentException}, а не молчаливый
     * переход на худший случай: опечатка в имени должна падать сразу, а не портить данные.</p>
     *
     * @param scenario имя сценария: sorted | random | worst_case
     * @param n        размер массива (степень двойки)
     * @param runs     число усредняемых замеров на каждую точку
     * @return среднее время сортировки в наносекундах
     * @throws InterruptedException если прерван измерительный поток (worst_case)
     */
    private static double measureScenario(String scenario, int n, int runs) throws InterruptedException {
        return switch (scenario) {
            // --- sorted: свежий массив 0..N-1 на каждый прогрев/замер ---
            case "sorted" -> measure(scenario, () -> sequentialArray(n), runs);

            // --- random: один Random(SEED) на весь замер => воспроизводимо между запусками ---
            case "random" -> {
                Random random = new Random(SEED);
                yield measure(scenario, () -> randomArray(random, n), runs);
            }

            // --- worst_case: вырожденная перестановка; глубина рекурсии ~N, поэтому
            //     замер идёт в отдельном потоке с увеличенным стеком (WORST_CASE_STACK),
            //     а ошибки переносятся в главный поток вручную ---
            case "worst_case" -> {
                int[] template = WorstCaseBuilder.build(n); // один раз конструируем вход; каждый прогон клонируем
                double[] result = new double[1];     // «выход» потока-измерителя (лямбда не может вернуть значение)
                Throwable[] error = new Throwable[1]; // «исключения» потока-измерителя: храним и пробрасываем дальше

                Thread thread = new Thread(null, () -> {
                    try {
                        result[0] = measure(scenario, template::clone, runs);
                    } catch (Throwable t) {
                        error[0] = t; // любую ошибку (в т.ч. StackOverflowError) сохраняем, чтобы не потерять
                    }
                }, "worst-case", WORST_CASE_STACK);

                thread.start();
                thread.join(); // ждём завершения измерительного потока
                if (error[0] != null) {
                    throw new RuntimeException("worst case failed", error[0]); // роняем main с исходной причиной
                }
                yield result[0];
            }

            default -> throw new IllegalArgumentException("unknown scenario: " + scenario);
        };
    }

    /**
     * Единый шаблон замера для всех сценариев: прогрев (JIT) → контрольная проверка → runs прогонов.
     *
     * <p>Фабрика {@code arrayFactory} возвращает НОВЫЙ массив на каждый вызов, поэтому каждый
     * прогон стартует с одинакового несортированного входа (сортировка меняет массив на месте).</p>
     *
     * @param scenario     имя сценария — только для сообщений об ошибке в {@link #checkSorted}
     * @param arrayFactory фабрика свежего входного массива (прогрев и каждый замер)
     * @param runs         число усредняемых замеров
     * @return среднее время сортировки в наносекундах
     */
    private static double measure(String scenario, Supplier<int[]> arrayFactory, int runs) {
        // --- Прогрев: WARMUP проходов, чтобы JIT оптимизировал код; первый проход валидируем
        for (int i = 0; i < WARMUP; i++) {
            int[] a = arrayFactory.get();
            Main.quickSort(a);
            if (i == 0) {
                checkSorted(a, scenario + " N=" + a.length); // убеждаемся, что алгоритм действительно отсортировал
            }
        }

        // --- Измерение: runs независимых проходов, засекаем ТОЛЬКО сортировку (не создание массива) ---
        long sum = 0;
        for (int i = 0; i < runs; i++) {
            int[] a = arrayFactory.get();
            long start = System.nanoTime(); // момент старта сортировки
            Main.quickSort(a);
            sum += System.nanoTime() - start; // длительность одного прохода в наносекундах
        }
        return (double) sum / runs; // среднее арифметическое по runs проходам
    }

    /** Свежий массив [0..N-1] — вход сценария sorted (каждый прогон строится заново). */
    private static int[] sequentialArray(int n) {
        int[] a = new int[n];
        for (int j = 0; j < n; j++) {
            a[j] = j; // последовательность 0,1,2,...,n-1
        }
        return a;
    }

    /** Заполняет массив случайными int из переданного генератора (общий помощник для random-сценария). */
    private static int[] randomArray(Random random, int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt();
        }
        return a;
    }

    /**
     * Проверка, что массив отсортирован по возрастанию (a[i] <= a[i+1]).
     * Если нарушение найдено — значит алгоритм «сэкономил» время, не выполнив сортировку, и это ошибка.
     *
     * @param context описание места проверки для сообщения об ошибке
     */
    private static void checkSorted(int[] a, String context) {
        for (int i = 0; i < a.length - 1; i++) {
            if (a[i] > a[i + 1]) {
                throw new IllegalStateException("sort failed: " + context);
            }
        }
    }

    /**
     * Общий прогрев JVM до начала всех замеров: 10 сортировок массива на 2^17 элементов.
     * Прогрев «запускает» JIT-компиляцию quickSort заранее, чтобы первые (малые) замеры
     * не несли на себе стоимость интерпретации/компиляции.
     */
    private static void globalWarmup() {
        Random random = new Random(SEED);
        for (int i = 0; i < 10; i++) {
            int[] a = randomArray(random, 1 << 17);
            Main.quickSort(a);
        }
    }
}
