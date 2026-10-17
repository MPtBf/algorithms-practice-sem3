# Практика по «Алгоритмам и анализу сложности» (Java)

Каждая практика — отдельная папка `practice-NN` и Maven-модуль:

| Практика | Тема | Итог |
|---|---|---|
| [practice-01](practice-01/) | QuickSort: O(n log n) vs O(n²) на замерах | ✅ теория подтверждена, графики в `practice-01/results/` |

## Требования

JDK 21 · Maven · Python 3.10+ (только для графиков)

## Быстрый старт

```bash
mvn test                          # тесты всех практик
mvn -pl practice-01 exec:exec     # замеры -> practice-01/results/benchmark_results.csv
cd practice-01
pip install -r requirements.txt   # однократно, только для графиков
python plot.py                    # 4 графика -> results/*.png
```

Подробности каждой практики — в её `README.md`.
