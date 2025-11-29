# Issue: Missed `:shadowed-var` warning when destructuring namespaced keywords

**Description:**
`clj-kondo` correctly warns about shadowed vars (e.g., `clojure.core/name`) when using simple `let` bindings or standard `{:keys [name]}` destructuring. However, it fails to warn when the shadowing occurs via namespaced keyword destructuring `{:keys [:my.ns/name]}`.

**Reproduction:**
```clojure
(ns repro)

;; 1. Caught: Standard binding
(defn test-1 []
  (let [name "foo"] ;; Warning: Shadowed var: clojure.core/name
    (println name)))

;; 2. Caught: Simple keys destructuring
(defn test-2 [m]
  (let [{:keys [name]} m] ;; Warning: Shadowed var: clojure.core/name
    (println name)))

;; 3. MISSED: Namespaced keys destructuring
(defn test-3 [m]
  (let [{:keys [:my.ns/name]} m] ;; No warning!
    (println name)))
```

**Expected Behavior:**
Case 3 should trigger the same `:shadowed-var` warning as cases 1 and 2, as the local symbol `name` is bound and shadows `clojure.core/name`.
