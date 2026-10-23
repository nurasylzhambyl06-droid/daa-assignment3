# DAA Assignment 3 — Graphs: DSU, MST, SCC and Shortest Paths

One adjacency-list graph class (directed and undirected) and six algorithms written from scratch, each one counting
the work it really does *inside* the algorithm:

| Algorithm | Class | Graph kind |
|---|---|---|
| Disjoint Set Union (rank + path compression) | `algorithms.DSU` | — |
| Kruskal MST | `algorithms.Kruskal` | undirected |
| Prim MST (PriorityQueue, lazy) | `algorithms.Prim` | undirected |
| Kosaraju SCC (iterative DFS) | `algorithms.Kosaraju` | directed |
| Kahn topological order + cycle detection | `algorithms.Kahn` | directed |
| Shortest paths in a DAG (one relaxation pass) | `algorithms.DagShortestPath` | DAG |
| *Bonus A:* Tarjan SCC (iterative, one pass) | `algorithms.Tarjan` | directed |
| *Bonus B:* condensation + critical path | `algorithms.Condensation`, `algorithms.CriticalPath` | directed |

## Requirements

- Java 17+
- Maven 3.6+
- (optional, only for the plots) Python 3 with `pandas`, `matplotlib`, `numpy`, `tabulate`

## Build

```bash
mvn compile
```

## Run the tests

```bash
mvn test
```

## Run the benchmark (one command)

```bash
mvn compile exec:exec
```

Runs the five benchmarked algorithms on ten graph configurations (5 sizes x 2 densities; every configuration has an
undirected graph, a directed graph and a DAG) and writes **`results/results.csv`** (50 rows):

```
algorithm,graph,n,m,time_ms,dsu_ops,edge_scans,comparisons,max_depth,result
```

The benchmark checks itself and **fails loudly** (exception, no CSV written past that point) if

- Kruskal and Prim return different total weights,
- the order from Kahn is not valid (`pos[u] < pos[v]` for every edge),
- an MST weight, SCC count or sum of DAG distances differs from the reference table of the assignment,
- Kosaraju does not scan exactly `2m` edges, or Kahn / the DAG relaxation do not scan exactly `m`.

A quick partial run (only sizes up to 10 000):

```bash
java -Xmx2g -cp target/classes benchmark.Benchmark 10000
```

Everything uses `new Random(42)`; the counters are identical on every machine. Only `time_ms` depends on the machine.

### Bonus benchmark (Tarjan vs Kosaraju, condensation, critical path)

```bash
java -Xmx2g -cp target/classes benchmark.BonusBenchmark
```

Writes `results/bonus.csv`.

### Plots and the numbers for the report

```bash
python scripts/analyze.py
```

Reads `results/results.csv` and writes the three PNG charts into `results/plots/`, plus `results/ratios.csv`
(counter / formula) and `results/slopes.csv` (fitted log-log slopes).

## Reference values (reproduced exactly)

| Graph (n / m) | MST total weight | SCC count | Sum of DAG distances |
|---|---|---|---|
| 1 000 / 4 000 | 147 970 | 27 | 396 274 |
| 1 000 / 20 000 | 30 363 | 1 | 252 411 |
| 3 162 / 12 648 | 468 911 | 137 | 1 271 911 |
| 3 162 / 63 240 | 97 589 | 1 | 723 157 |
| 10 000 / 40 000 | 1 486 139 | 373 | 3 941 828 |
| 10 000 / 200 000 | 309 550 | 1 | 2 381 342 |
| 31 623 / 126 492 | 4 715 515 | 1 188 | 12 551 891 |
| 31 623 / 632 460 | 968 517 | 1 | 7 376 612 |
| 100 000 / 400 000 | 15 012 094 | 3 923 | 40 096 539 |
| 100 000 / 2 000 000 | 3 056 681 | 1 | 23 512 087 |

## Input format

```
n m directed|undirected
u v w          (m lines, vertices 0-based, weight may be negative)
```

Blank lines and lines starting with `#` are ignored. Sample graphs are in `data/`:
`small_connected.txt`, `disconnected.txt` (also has a self-loop, parallel edges and isolated vertices),
`cyclic.txt`, `dag.txt` (with negative weights).

## Project layout

```
src/main/java/
  graph/        Graph, Edge, GraphIO (file format), GraphGenerator (the exact generator of the assignment)
  algorithms/   DSU, Kruskal, Prim, Kosaraju, Kahn, DagShortestPath, Tarjan, Condensation, CriticalPath, result records
  metrics/      Metrics (dsuOps, edgeScans, comparisons, maxDepth)
  benchmark/    Benchmark (main, self-checking), BonusBenchmark, Reference (the table above)
src/test/java/  JUnit 5 tests (graph, DSU, MST, SCC, topological order, DAG shortest paths, condensation, reference values)
data/           small input graphs
results/        results.csv, bonus.csv, ratios.csv, slopes.csv, plots/
scripts/        analyze.py
REPORT.md       report
```

## Counters

| Counter | Meaning |
|---|---|
| `dsu_ops` | every `find` call (including the two made inside `union`) + every successful `union` |
| `edge_scans` | edges (arcs) looked at |
| `comparisons` | weights compared: Kruskal's merge sort, Prim's queue ordering, relaxation tests |
| `max_depth` | deepest the algorithm's own explicit DFS stack ever got |

Exact invariants (checked on every run): Kosaraju scans `2m` edges, Kahn and the DAG relaxation scan `m` each.

## Git workflow

Branches: `feature/dsu`, `feature/mst`, `feature/scc`, `feature/paths`, `feature/metrics`; each merged into `main`,
which carries the tag `v1.0`.

## AI use acknowledgement

I used an AI assistant (Claude) during this assignment for:

- analysing and debugging my code;
- writing and editing the text of REPORT.md and README.md;
- producing the plot images (the plotting script `scripts/analyze.py` that draws the three charts in `results/plots/`).

The benchmark was run on my own computer and the timing numbers in the report come from my own run. I checked that the
benchmark reproduces all reference values of the assignment, and I can explain the algorithms, the counters and
the report at the defense.