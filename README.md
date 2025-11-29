# code-expert

A "Structure-Aware" indexing tool for Clojure codebases, designed to empower AI agents with deep semantic understanding of code.

## 🚀 What is this?

Standard RAG (Retrieval Augmented Generation) treats code like plain text, often splitting functions in half or missing context. **Codebase Expert** is different. It parses your code into an **Abstract Syntax Tree (AST)** and stores it in a **Graph Database (Datalevin)**.

This allows AI agents to answer questions like:
- *"Find all pure functions in the `math` module."*
- *"Show me the full source of `calculate-total` and its docstring."*
- *"List all side-effecting functions that use `jdbc/execute!`."*

## ✨ Features

- **AST-Based Indexing**: Uses `edamame` to parse code structure, ensuring we capture complete forms (functions, namespaces, macros).
- **Purity Analysis**: Heuristically tags functions as `:pure`, `:impure`, or `:pure-ish` based on their contents (e.g., `reset!` or `println` implies impurity).
- **Datalevin Storage**: Stores code as a graph in a fast, embedded Datalog database.
- **Babashka Powered**: Fast startup, zero-compile, runs anywhere.

## 🛠️ Usage

### Prerequisites
- [Babashka](https://github.com/babashka/babashka) (v1.3.181+)

### 1. Indexing a Project
Run the indexer pointing to your target project root. This will scan `src` and `modules` directories.

```bash
# From the code-expert directory
bb index ../your-clojure-project
```

*Example:*
```bash
bb index ../bb-mcp-server
```

This creates a persistent database at `.db/codebase`.

### 2. Querying the Data
You can query the database using Datalog. Here is a simple script to find all "pure" functions:

```bash
bb -e "
(require '[code-expert.db :as db])
(require '[pod.huahaiy.datalevin :as d])

(let [conn (db/get-conn ".db/codebase")]
  (def db (d/db conn))
  
  (println "Pure functions found:")
  (doseq [[name file] (d/q '[:find ?name ?file
                             :where [?e :code/purity :pure]
                                    [?e :code/name ?name]
                                    [?e :code/file ?file]]
                           db)]
    (println "-" name "(" file ")")))"
```

## 📊 Database Schema

The database uses the following schema to represent code entities:

| Attribute | Type | Description |
|-----------|------|-------------|
| `:code/id` | `:keyword` | Unique ID (e.g., `:my-ns/my-func`). |
| `:code/name` | `:symbol` | The name of the var (e.g., `my-func`). |
| `:code/type` | `:keyword` | Type of definition: `:ns`, `:defn`, `:def`, `:defmacro`. |
| `:code/purity` | `:keyword` | Analysis result: `:pure`, `:impure`, `:pure-ish`, `:unknown`. |
| `:code/source` | `:string` | The full, verbatim source code of the form. |
| `:code/doc` | `:string` | The docstring, if present. |
| `:code/file` | `:string` | Relative path to the file containing the code. |
| `:code/line` | `:long` | Line number where the definition starts. |

## 🧠 "Structure-Aware" RAG vs. Standard RAG

| Feature | Standard Text RAG | Codebase Expert |
|---------|-------------------|-----------------|
| **Chunking** | Arbitrary lines (e.g., 500 chars) | Logical units (Functions, Namespaces) |
| **Context** | Often cuts off code mid-function | Always captures full valid forms |
| **Metadata** | File path only | Purity, Type, Name, Docstring |
| **Retrieval** | Fuzzy text match | Precise Datalog queries |

## 🔮 Future Plans
- **Call Graphs**: Indexing `:code/calls` to track which functions call which.
- **MCP Server**: Exposing this database via the Model Context Protocol so Claude/Cursor can query it directly.