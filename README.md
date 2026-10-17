# Практика по «Алгоритмам и анализу сложности» (Java)

Каждая практика — отдельная папка `practice-NN` и Maven-модуль:

| Практика | Тема | Итог |
|---|---|---|
| [practice-01](practice-01/) | QuickSort: O(n log n) vs O(n²) на замерах | ✅ теория подтверждена, графики в `practice-01/results/` |

## Требования

JDK 21 · Maven · Python 3.10+ (только для графиков)

## Быстрый старт

Все команды выполняются **из корня репозитория**.

Тесты всех практик:

```bash
mvn test
```

Замеры quicksort (пишет CSV в `practice-01/results/`):

```bash
mvn -pl practice-01 compile exec:exec
```

Графики (нужен Python; зависимости ставятся один раз):

```bash
pip install -r practice-01/requirements.txt
python practice-01/plot.py
```

Подробности каждой практики — в её `README.md`.
