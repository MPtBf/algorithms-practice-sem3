"""
Построение графиков для practice-01 (QuickSort) из results/benchmark_results.csv.

ЗАПУСК (можно из любого каталога, например из корня репозитория):
    python practice-01/plot.py [путь/к/benchmark_results.csv]
По умолчанию читает results/benchmark_results.csv рядом со скриптом;
PNG всегда сохраняются рядом с CSV.

-------------------------------------------------------------------------------
ПОЧЕМУ НА ГРАФИКАХ ПРЯМЫЕ, А НЕ КРИВЫЕ
-------------------------------------------------------------------------------
Мы сравниваем измеренное время t(N) с теоретическими законами сложности:
    t = c·N·log2(N)   (средний/лучший случай)
    t = c·N^2         (худший случай)
Оси на первых двух панелях — ЛОГАРИФМИЧЕСКИЕ (log-log). Возьмём логарифм
степенной функции t = c·N^k:
    log(t) = log(c) + k·log(N)
Это уравнение ПРЯМОЙ (y = b + k·x) в координатах (log N, log t):
    наклон прямой  = показатель степени k;
    смещение       = log(c).
Поэтому:
    • квадратичная зависимость N^2 на log-log графике — прямая с наклоном 2;
    • линейная N^1 — прямая с наклоном 1;
    • N·log2(N) — «почти прямая»: log(N·log N) = log N + log log N, и добавка
      log log N растёт очень медленно, поэтому на нескольких порядках N
      кривая выглядит почти прямой с наклоном чуть больше 1;
    • если бы оси были обычными (не логарифмическими), все эти функции были
      бы настоящими кривыми и визуально почти неотличимы друг от друга.
Панель «наклон аппроксимации» показывает это явно: мы подгоняем прямую
t = 2^b · N^k в log-log осях и печатаем k (теория ожидает ~1 или ~2).

-------------------------------------------------------------------------------
КАК ЛИНИИ ВЫРАВНИВАНЫ ПО МАСШТАБУ (fit_scale)
-------------------------------------------------------------------------------
Линии теории подгоняются к точкам замеров методом наименьших квадратов —
но НЕ свободная прямая, а прямая через НАЧАЛО КООРДИНАТ: ищем только один
коэффициент масштаба c при известной форме функции f(x) (x = N·log N или N^2):
    t ≈ c·f(x),  min_c Σ(t_i − c·f_i)^2
Решение (производная по c обращается в нуль):
    c = Σ(t_i·f_i) / Σ(f_i²)
Это и есть «выравнивание по масштабу»: форма кривой берётся из теории,
величина c подбирается так, чтобы кривая прошла «посередине» точек.
Качество совпадения оценивается R²:
    R² = 1 − Σ(t − c·f)² / Σ(t − mean(t)²)   (1.0 — идеальное совпадение).
Поэтому в легендах мы сравним R² для N·log N и N²: чья R² больше — тот закон
лучше объясняет данные.

Панель нормировки — независимая проверка того же тезиса: если t ≈ c·N·log N,
то отношение t/(N·log N) должно быть около константы, а t/N² — наоборот,
«уходить в ноль» (для среднего случая) или быть константой (для худшего).
"""

import csv
import math
import os
import sys

import matplotlib
matplotlib.use("Agg")  # безэкранный бэкенд: файлы-картинки вместо окон
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.ticker import FormatStrFormatter

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

# Путь к CSV: первый аргумент командной строки или значение по умолчанию.
# По умолчанию путь отсчитывается от КАТАЛОГА СКРИПТА, а не от текущего каталога —
# тогда `python practice-01/plot.py` работает из корня репозитория и откуда угодно.
_BASE = os.path.dirname(os.path.abspath(__file__))
CSV_PATH = sys.argv[1] if len(sys.argv) > 1 else os.path.join(_BASE, "results", "benchmark_results.csv")
# PNG сохраняются рядом с CSV (поэтому при ручном указании пути картинки попадут в его каталог)
OUT_DIR = os.path.dirname(CSV_PATH) or "."

# Человекочитаемые подписи сценариев для заголовков и легенд
SCENARIO_TITLES = {
    "sorted": "лучший случай (отсортированный вход)",
    "random": "средний случай (случайные данные)",
    "worst_case": "худший случай (вырожденный вход)",
}


def load(path):
    """
    Читает CSV бенчмарка и группирует точки по сценариям.

    Возвращает dict: scenario -> {"N": np.array размеров, "t": np.array времени в мс}.
    Время хранится в миллисекундах (из наносекунд CSV делим на 1e6) — так удобнее на графиках.
    """
    data = {}
    with open(path, encoding="utf-8") as f:
        for row in csv.DictReader(f):
            sc = row["scenario"]
            d = data.setdefault(sc, {"N": [], "t": []})
            d["N"].append(int(row["N"]))
            d["t"].append(float(row["mean_ns"]) / 1e6)  # нс -> мс
    for sc, d in data.items():
        # numpy-массивы, чтобы работали векторные формулы МНК/логарифмов
        d["N"] = np.array(d["N"], dtype=float)
        d["t"] = np.array(d["t"], dtype=float)
    return data


def fit_scale(x, y):
    """
    «Выравнивание по масштабу»: подбор единственного коэффициента c при известной
    функции x (x — это уже N·log2(N) или N^2):  y ≈ c·x,  min_c Σ(y − c·x)².

    МНК через начало координат даёт замкнутое решение:  c = Σ(y·x) / Σ(x·x).

    Возвращает (c, R²): R² = 1 − Σ(y − c·x)² / Σ(y − mean(y))²,
    1.0 означает идеальное совпадение кривой с данными.
    """
    c = float(np.sum(y * x) / np.sum(x * x))          # оценка масштаба
    pred = c * x                                        # теоретическая кривая «в данных»
    ss_res = float(np.sum((y - pred) ** 2))             # остаточная сумма квадратов
    ss_tot = float(np.sum((y - np.mean(y)) ** 2))       # разброс данных около среднего
    r2 = 1.0 - ss_res / ss_tot if ss_tot > 0 else 1.0
    return c, r2


def slope_fit(N, t):
    """
    Наклон k аппроксимации t ~ N^k в log-log осях.

    Берём логарифм по основанию 2 от обеих сторон:  log2(t) = k·log2(N) + b —
    линейная регрессия (polyfit степени 1): k = наклон, b = log2(масштаба).
    k ≈ 1 подтверждает O(N·log N), k ≈ 2 — O(N²).
    """
    xs = np.log2(N)
    ys = np.log2(t)
    k, b = np.polyfit(xs, ys, 1)  # наименьшие квадраты для y = k·x + b
    return k, b


def add_explanation(fig):
    """
    Нижняя сноска на графике: объясняет читателю, почему линии прямые
    и как они были выровнены по данным (см. также модульный docstring).
    Строки намеренно короткие — помещаются и в узкий сводный график.
    """
    fig.text(
        0.5, 0.012,
        "Почему прямые: оси log-log, поэтому степенной закон t = c·N^k — это прямая: log t = log c + k·log N\n"
        "(наклон = показатель k: у N^1 наклон 1, у N^2 — 2; N·log N — почти прямая, т.к. log log N растёт медленно)\n"
        "Выравнивание линий: масштаб c подобран МНК через начало координат c = Σ(t·x)/Σ(x²), качество — R² в легенде",
        ha="center", va="bottom", fontsize=8, color="dimgray",
    )


def plot_scenario(sc, N, t):
    """Строит и сохраняет 3-панельный график для одного сценария -> results/benchmark_{sc}.png."""
    # --- Подгонки теории к данным ------------------------------------------
    x = N * np.log2(N)          # теория среднего/лучшего случая
    y = N * N                   # теория худшего случая
    c1, r1 = fit_scale(x, t)    # масштаб и R² для N·log2(N)
    c2, r2 = fit_scale(y, t)    # масштаб и R² для N²
    k, b = slope_fit(N, t)      # наклон log-log аппроксимации (теория: ~1 или ~2)

    norm1 = t / x               # нормировка «время / (N·log N)»: около константы => O(N log N)
    norm2 = t / y               # нормировка «время / N²»: около константы => O(N²)

    fig, axes = plt.subplots(1, 3, figsize=(17, 5))
    fig.suptitle("QuickSort: " + SCENARIO_TITLES.get(sc, sc), fontsize=14)

    # --- Панель 1: замеры и две теоретические кривые на log-log -------------
    ax = axes[0]
    ax.loglog(N, t, "o", label="измерено (среднее)")               # сами точки замеров
    ax.loglog(N, c1 * x, "-", linewidth=1.5,
              label=f"{c1:.3g}·N·log2(N),  R²={r1:.4f}")          # кривая теории, выровненная по данным
    ax.loglog(N, c2 * y, "-", linewidth=1.5,
              label=f"{c2:.3g}·N²,  R²={r2:.4f}")
    ax.set_xlabel("N")
    ax.set_ylabel("время, мс")
    ax.set_title("замеры vs теоретические кривые (log-log)")
    ax.grid(True, which="both", alpha=0.3)  # сетка по обоим основаниям ( decade-сетка )
    ax.legend(fontsize=8)

    # --- Панель 2: наклон аппроксимации -------------------------------------
    # Показываем сам показатель степени k: на log-log это просто наклон прямой.
    ax = axes[1]
    ax.loglog(N, t, "o", label="измерено")
    ax.loglog(N, 2 ** b * N ** k, "-", linewidth=1.5,
              label=f"аппроксимация N^{k:.2f}")                     # fitted прямая в log-log => степень k
    # Справочные прямые НЕ подогнаны: их масштаб взят «от первой точки данных»,
    # чтобы визуально сравнить наклоны (k=1 и k=2) с реальным ростом замеров.
    ax.loglog(N, (t[0] / N[0]) * N, "--", color="gray", linewidth=1,
              label="прямая N^1 (масштаб от первой точки)")
    ax.loglog(N, (t[0] / N[0] ** 2) * N ** 2, "--", color="black", linewidth=1,
              label="прямая N^2 (масштаб от первой точки)")
    ax.set_xlabel("N")
    ax.set_ylabel("время, мс")
    ax.set_title(f"наклон аппроксимации: t ~ N^{k:.2f}")
    ax.grid(True, which="both", alpha=0.3)
    ax.legend(fontsize=8)

    # --- Панель 3: нормировка -----------------------------------------------
    # Если t ≈ c·N·log N, то t/(N·log N) ~ const: линия должна быть «горизонтальной».
    ax = axes[2]
    ax.plot(N, norm1, "o-", label="время / (N·log2(N))")
    ax.set_xscale("log", base=2)                                  # тут обычные оси Y — видна «плоскость» нормировки
    ax.set_xlabel("N")
    ax.set_ylabel("мс / (N·log2 N)")
    ax.set_title("нормировка: около константы = N·log2(N)")
    ax.grid(True, alpha=0.3)
    ax.yaxis.set_major_formatter(FormatStrFormatter("%.1e"))
    ax2 = ax.twinx()                                               # вторая (правая) ось для второй нормировки
    ax2.plot(N, norm2, "s--", color="tab:red", label="время / N²")
    ax2.set_ylabel("мс / N²", color="tab:red")
    ax2.yaxis.set_major_formatter(FormatStrFormatter("%.1e"))
    ax.legend(fontsize=8, loc="upper left")
    ax2.legend(fontsize=8, loc="upper right")

    # Нижние поля расширены под сноской с объяснением (add_explanation)
    fig.tight_layout(rect=(0, 0.10, 1, 0.94))
    add_explanation(fig)
    name = os.path.join(OUT_DIR, f"benchmark_{sc}.png")
    fig.savefig(name, dpi=150)  # 150 dpi — чётко для отчёта/просмотра
    plt.close(fig)              # освобождаем память (фигур может быть много)
    print(f"{sc}: slope={k:.3f}  c_nlogn={c1:.4g} (R2={r1:.4f})  "
          f"c_n2={c2:.4g} (R2={r2:.4f})  -> {name}")


def plot_all(data):
    """Сводный log-log график всех сценариев + две эталонные теоретические кривые -> results/benchmark_all.png."""
    fig, ax = plt.subplots(figsize=(9, 7))
    colors = {"sorted": "tab:green", "random": "tab:blue", "worst_case": "tab:red"}

    # Точки замеров каждого сценария (одни оси — видно, насколько худший случай «быстрее» растёт)
    for sc, d in sorted(data.items()):
        ax.loglog(d["N"], d["t"], "o-", color=colors.get(sc),
                  label=SCENARIO_TITLES.get(sc, sc))

    # Эталонные кривые: коэффициенты подбираются по «своим» сценариям (fit_scale, см. docstring),
    # форма берётся из теории — на log-log это прямые/почти прямые.
    anchor1 = data.get("random") or data.get("sorted")   # кривая N·log N привязана к среднему случаю
    anchor2 = data.get("worst_case") or anchor1          # кривая N² — к худшему
    if anchor1 is not None:
        N1 = anchor1["N"]
        c1, _ = fit_scale(N1 * np.log2(N1), anchor1["t"])
        ax.loglog(N1, c1 * N1 * np.log2(N1), ":", color="black", linewidth=1.2,
                  label=f"{c1:.3g}·N·log2(N)")
    if anchor2 is not None:
        N2 = anchor2["N"]
        c2, _ = fit_scale(N2 * N2, anchor2["t"])
        ax.loglog(N2, c2 * N2 * N2, "--", color="black", linewidth=1.2,
                  label=f"{c2:.3g}·N²")

    ax.set_xlabel("N")
    ax.set_ylabel("время, мс")
    ax.set_title("QuickSort: время сортировки от размера входа")
    ax.grid(True, which="both", alpha=0.3)
    ax.legend(fontsize=9)
    fig.tight_layout(rect=(0, 0.09, 1, 1))  # место под сноску
    add_explanation(fig)
    name = os.path.join(OUT_DIR, "benchmark_all.png")
    fig.savefig(name, dpi=150)
    plt.close(fig)
    print("-> " + name)


def main():
    """Точка входа: прочитать CSV и построить по графику на каждый сценарий + сводный."""
    data = load(CSV_PATH)
    for sc, d in sorted(data.items()):
        plot_scenario(sc, d["N"], d["t"])
    if data:
        plot_all(data)


if __name__ == "__main__":
    main()
