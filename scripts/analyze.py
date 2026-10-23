"""
Reads results/results.csv and produces everything the report needs:
  results/plots/operations_vs_n.png      operations vs n, one line per algorithm and density, log-log
  results/plots/scans_per_edge.png       edgeScans / m vs n (flat line = linear work)
  results/plots/kruskal_edge_share.png   share of edges Kruskal examines, sparse vs dense
  results/ratios.csv                     counter / formula for every algorithm, density and n
  results/slopes.csv                     fitted log-log slopes
and prints the same numbers as markdown tables.

Usage (from the project root):   pip install pandas matplotlib numpy   then   python scripts/analyze.py
"""
import math
import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

CSV = os.path.join("results", "results.csv")
PLOTS = os.path.join("results", "plots")
os.makedirs(PLOTS, exist_ok=True)

df = pd.read_csv(CSV)
df["density"] = df["graph"].str.split("_").str[1]
df["operations"] = df["dsu_ops"] + df["edge_scans"] + df["comparisons"]
df["scans_per_edge"] = df["edge_scans"] / df["m"]

ALGOS = ["Kruskal", "Prim", "Kosaraju", "Kahn", "DagShortestPath"]
COLORS = dict(zip(ALGOS, ["tab:blue", "tab:orange", "tab:green", "tab:red", "tab:purple"]))
STYLES = {"sparse": "-", "dense": "--"}
# Different marker shapes AND sizes: Kosaraju (edgeScans 2m) and DagShortestPath (m scans + m comparisons) have exactly
# the same operation count, so their lines coincide - the big green triangle sits under the small purple diamond.
MARKERS = dict(zip(ALGOS, ["o", "s", "^", "v", "D"]))
MSIZE = dict(zip(ALGOS, [6, 6, 12, 6, 5]))


def lines(ax, column, ylabel, title, loglog=True, legend_loc="best"):
    for algo in ALGOS:
        for density in ("sparse", "dense"):
            sub = df[(df.algorithm == algo) & (df.density == density)].sort_values("n")
            ax.plot(sub["n"], sub[column], marker=MARKERS[algo], markersize=MSIZE[algo], color=COLORS[algo],
                    linestyle=STYLES[density], label=f"{algo} ({density})")
    if loglog:
        ax.set_xscale("log")
        ax.set_yscale("log")
    else:
        ax.set_xscale("log")
    ax.set_xlabel("n (number of vertices)")
    ax.set_ylabel(ylabel)
    ax.set_title(title)
    ax.grid(True, which="both", alpha=0.3)
    ax.legend(fontsize=7, ncol=2, loc=legend_loc)


# ---- plot 1: operations vs n ----
fig, ax = plt.subplots(figsize=(8, 6))
lines(ax, "operations", "counted operations (dsu_ops + edge_scans + comparisons)", "Operations vs n (log-log)", legend_loc="upper left")
fig.tight_layout()
fig.savefig(os.path.join(PLOTS, "operations_vs_n.png"), dpi=150)
plt.close(fig)

# ---- plot 2: scans per edge ----
fig, ax = plt.subplots(figsize=(8, 6))
lines(ax, "scans_per_edge", "edgeScans / m", "Scans per edge (flat line = linear work)", loglog=False, legend_loc="center right")
ax.set_ylim(0, 2.3)
fig.tight_layout()
fig.savefig(os.path.join(PLOTS, "scans_per_edge.png"), dpi=150)
plt.close(fig)

# ---- plot 3: share of edges Kruskal examines ----
fig, ax = plt.subplots(figsize=(7, 5))
for density, color in (("sparse", "tab:blue"), ("dense", "tab:red")):
    sub = df[(df.algorithm == "Kruskal") & (df.density == density)].sort_values("n")
    ax.plot(sub["n"], sub["scans_per_edge"], marker="o", color=color, label=f"Kruskal, {density} (m = {'4n' if density == 'sparse' else '20n'})")
ax.set_xscale("log")
ax.set_ylim(0, 1.05)
ax.set_xlabel("n (number of vertices)")
ax.set_ylabel("edgeScans / m  (share of edges examined)")
ax.set_title("Share of edges Kruskal examines before the tree is complete")
ax.grid(True, which="both", alpha=0.3)
ax.legend()
fig.tight_layout()
fig.savefig(os.path.join(PLOTS, "kruskal_edge_share.png"), dpi=150)
plt.close(fig)

# ---- counter / formula ratios ----
rows = []
for density in ("sparse", "dense"):
    for n in sorted(df.n.unique()):
        def get(algo):
            return df[(df.algorithm == algo) & (df.density == density) & (df.n == n)].iloc[0]
        k, p, ko, ka, ds = get("Kruskal"), get("Prim"), get("Kosaraju"), get("Kahn"), get("DagShortestPath")
        m = int(k["m"])
        rows.append({"density": density, "n": n, "m": m,
                     "Kruskal dsuOps/E": k.dsu_ops / m,
                     "Kruskal dsuOps/edgeScans": k.dsu_ops / k.edge_scans,
                     "Kruskal comparisons/(E log2 E)": k.comparisons / (m * math.log2(m)),
                     "Prim edgeScans/E": p.edge_scans / m,
                     "Prim comparisons/(E log2 V)": p.comparisons / (m * math.log2(n)),
                     "Kosaraju edgeScans/(V+E)": ko.edge_scans / (n + m),
                     "Kahn edgeScans/(V+E)": ka.edge_scans / (n + m),
                     "DagSP edgeScans/(V+E)": ds.edge_scans / (n + m)})
ratios = pd.DataFrame(rows)
ratios.to_csv(os.path.join("results", "ratios.csv"), index=False, float_format="%.4f")

# ---- slopes ----
slope_rows = []
for algo in ALGOS:
    for density in ("sparse", "dense"):
        sub = df[(df.algorithm == algo) & (df.density == density)].sort_values("n")
        s_scans = np.polyfit(np.log10(sub["n"]), np.log10(sub["edge_scans"]), 1)[0]
        s_ops = np.polyfit(np.log10(sub["n"]), np.log10(sub["operations"]), 1)[0]
        slope_rows.append({"algorithm": algo, "density": density,
                           "slope of edgeScans": s_scans, "slope of all operations": s_ops})
slopes = pd.DataFrame(slope_rows)
slopes.to_csv(os.path.join("results", "slopes.csv"), index=False, float_format="%.3f")

print("\n### Counter / formula ratios (constant = confirmed complexity class)\n")
print(ratios.to_markdown(index=False, floatfmt=".3f"))
print("\n### Fitted log-log slopes (1.00 = linear)\n")
print(slopes.to_markdown(index=False, floatfmt=".3f"))
print("\n### Share of edges Kruskal examines (edgeScans / m)\n")
share = df[df.algorithm == "Kruskal"].pivot(index="n", columns="density", values="scans_per_edge")
print(share.to_markdown(floatfmt=".3f"))
print("\nPlots written to", PLOTS)
