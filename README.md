# Analyzer — Static analysis, coupling & module identification

This project analyzes Java code to compute metrics, build call graphs and identify modules via hierarchical clustering.

Features
- Parse Java sources (Eclipse JDT AST or Spoon) and extract classes/methods
- Compute class coupling (normalized) and generate an interactive coupling graph (Vis.js)
- Build an interactive call graph (Vis.js)
- Perform agglomerative hierarchical clustering on class coupling to identify candidate modules
- Lightweight HTML reports: `callgraph.html`, `coupling_graph.html`, `modules.html`

Build

The project uses Maven. Two options:

- Using the wrapper (recommended):

  ./mvnw -DskipTests package

- Using a system-installed Maven:

  mvn -DskipTests package

If you don't have Maven installed on macOS, you can install it with Homebrew:

  brew install maven

Run (GUI)

1. Build the project (see above).
2. Run the GUI application (from your IDE or with `java -cp target/classes:target/dependency/* analyzer.Analyzer`).
3. In the GUI select a folder or a Java file to analyze.
4. Use the sidebar to view statistics, generate the call graph or the coupling analysis.

Spoon-based analysis

The project includes an alternative analysis runner that uses Spoon to build a model of the source tree and extract calls. This can be useful when the JDT-based parser doesn't find bindings or when you prefer a different AST engine.

- In the GUI: use the "Couplage" panel and check "Utiliser Spoon pour cette analyse" to run Spoon on the selected folder.
- Or run programmatically via `SpoonRunner.runSpoonAnalysis(Path inputPath, double cp)`.

Generated reports

- callgraph.html — interactive call graph (methods as nodes)
- coupling_graph.html — class coupling graph (classes as nodes, weighted edges)
- modules.html — identified modules (list) with an internal coupling score

Tuning

- The module identification threshold (cp) controls how strict the filter is when selecting subtrees as modules. Typical values: 0.01 — 0.05 depending on project size.

Notes

- The project includes a Maven wrapper (mvnw) to simplify builds on machines without Maven installed.
- Vis.js is used in the generated HTML files; the pages load the Vis.js CDN.

License

This repository contains code developed for coursework. Adjust licensing headers if you republish the code.