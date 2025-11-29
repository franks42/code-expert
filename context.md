# Code Expert - Project Context

## Overview
`code-expert` is a standalone, Babashka-based CLI tool designed to index, analyze, and visualize Clojure codebases. It was originally extracted from a larger MCP server project to provide focused static analysis capabilities.

The core philosophy is to treat "code as data" by parsing the AST, storing it in a graph database (Datalevin), and then querying that graph to generate insights—specifically focusing on **call graphs** and **function purity**.

## Tech Stack
- **Runtime**: [Babashka](https://github.com/babashka/babashka) (fast startup, scripting friendly).
- **Database**: [Datalevin](https://github.com/juji-io/datalevin) (Datalog graph DB).
  - *Version Note*: We are strictly using version `0.9.27` to ensure compatibility with the pod.
- **Parser**: [Edamame](https://github.com/borkdude/edamame) (Clojure parser).
- **Visualization**: [Mermaid.js](https://mermaid.js.org/) (Graph syntax generation).

## Current Capabilities

### 1. Indexing (`indexer.clj`)
The tool scans a project (defaulting to `src` and `modules`), parses every `.clj` file, and extracts:
- **Definitions**: `defn`, `def`, `defmacro`, `ns`.
- **Top-Level Forms**: Arbitrary code at the root of a file (e.g., `(do ...)` or side-effecting calls) is captured as `:code/type :top-level`.
- **Call Graph**: It walks the function bodies to find outgoing calls to other symbols.
- **Purity Analysis**: It applies heuristics to classify functions as:
  - `:impure` (uses `reset!`, `spit`, `future`, or is a top-level form).
  - `:pure-ish` (uses logging/printing).
  - `:pure` (none of the above).

### 2. Visualization (`visualize.clj`)
Generates a Mermaid flowchart (`graph TD`) of the codebase.
- **Web UI**: A browser-based viewer is available via `bb serve-ui`.
- **Filtering**: Supports filtering by namespace prefix via CLI arg (e.g., `bb visualize http-core`) or query param in the UI (`?ns=http-core`).
- **Nodes**: Functions and top-level forms.
  - Top-level forms are labeled as `do...` to reduce noise.
- **Edges**: Calls between functions.
  - **Smart Linking**: It attempts to resolve local symbol calls (e.g., `(my-func)`) to their Fully Qualified Names (FQNs) (e.g., `:my.ns/my-func`) by checking the current namespace and the known entity IDs in the DB.
- **Styling**: Nodes are color-coded by purity:
  - 🟦 Blue: Pure
  - 🟨 Yellow: Pure-ish (logging)
  - 🟥 Red: Impure (side effects)

### 3. Web UI (`server.clj` & `www/index.html`)
- **Server**: Runs an http-kit server on port 9999.
- **Interactive Graph**: Renders the Mermaid graph using the Mermaid.js CDN.
- **Filtering**: 
    - Users can filter by namespace prefix using the input box or URL parameter `?ns=...`.
    - Example: `http://localhost:9999/?ns=http-core`
- **Navigation**: Clicking a node opens the file in VS Code (`vscode://file/...`).

### 4. Testing
- **Playwright**: End-to-end tests in `tests/` verify the UI renders and interactions work.
    - `tests/visualize.spec.js`: Checks basic rendering.
    - `tests/interaction.spec.js`: Checks filtering and UI controls.

## Recent History & "Gotchas"

### The "Shadowing" Bug
We recently fixed a bug in `visualize.clj` where the local variable `name` (from destructuring) shadowed `clojure.core/name`. This caused the purity class generation to fail (returning `nil` instead of "pure"/"impure").
*   **Fix**: Explicitly used `clojure.core/name` or renamed the binding.
*   **Lesson**: Always run `clj-kondo` after changes.

### Top-Level Forms
We recently added support for indexing "top-level forms". These are crucial for understanding module initialization side-effects. They are treated as inherently `:impure`.

## Roadmap & Next Steps

### 1. Visualization Refinement
The current graph includes *everything*. For large projects, this produces a "spaghetti" diagram that is hard to read.
*   **Idea**: Add CLI arguments to filter by namespace (e.g., `bb visualize --ns my.module.*`).
*   **Idea**: Add depth limits or "neighborhood" views (show only X hops from function Y).

### 2. Advanced Analysis
*   **Arity Checks**: We currently don't check if the number of arguments matches.
*   **Var Usage**: We track function calls, but not variable usage (e.g., `def` constants).

### 3. User Interface
*   ✅ **Implemented**: Serve a simple HTML page that renders the Mermaid graph interactively.
*   ✅ **Implemented**: Click on nodes to see the source code (via `vscode://` protocol).
*   **Idea**: Add more advanced filtering (e.g., by purity, by directory).

### 4. Integration
*   This tool could be wrapped back into an MCP server to allow LLMs to query the codebase structure dynamically ("Find all impure functions in module X").

## How to Run
```bash
# 1. Index the codebase (scans ../bb-mcp-server by default, configurable in bb.edn)
bb index

# 2. Generate Visualization (CLI)
bb visualize > graph.mmd

# 3. Run Web UI (Interactive)
bb serve-ui
# Open http://localhost:9999

# 4. Lint (Important!)
clj-kondo --lint src
```
