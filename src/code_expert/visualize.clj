(ns code-expert.visualize
  (:require [code-expert.db :as db]
            [pod.huahaiy.datalevin :as d]
            [clojure.string :as str]))

(defn safe-id [k]
  (-> (str k)
      (subs 1) ;; remove :
      (str/replace #"[^a-zA-Z0-9]" "_")))

(defn -main [& args]
  (let [db-path ".db/codebase"
        conn (db/get-conn db-path)
        db (d/db conn)
        project-root "/Users/franksiebenlist/Development/bb-mcp-server"
        ;; Get all definitions
        entities (d/q '[:find [(pull ?e [:code/id :code/name :code/purity :code/calls :code/type :code/file :code/line]) ...]
                        :in $ [?type ...]
                        :where [?e :code/type ?type]]
                      db
                      [:defn :top-level :def :defmacro])
        ;; Create a set of known IDs for filtering calls
        known-ids (set (map :code/id entities))
        
        arg (first args)
        limit (when (and arg (re-matches #"\d+" arg)) (Integer/parseInt arg))
        ns-filter (when (and arg (not limit)) arg)

        entities (cond->> entities
                   ns-filter (filter (fn [e] 
                                       (let [ns (namespace (:code/id e))]
                                         (and ns (str/starts-with? ns ns-filter)))))
                   limit     (take limit))]
    
    (println "graph TD")
    (println "  classDef pure fill:#e1f5fe,stroke:#01579b,stroke-width:2px;")
    (println "  classDef pure-ish fill:#fff9c4,stroke:#fbc02d,stroke-width:2px;")
    (println "  classDef impure fill:#ffebee,stroke:#b71c1c,stroke-width:2px;")
    (println "  classDef unknown fill:#f5f5f5,stroke:#9e9e9e,stroke-width:1px;")

    (doseq [{:keys [:code/id :code/name :code/purity :code/calls :code/type :code/file :code/line]} entities]
      (let [node-id (safe-id id)
            node-ns (namespace id)
            purity-class (cond
                           (keyword? purity) (clojure.core/name purity)
                           (and (string? purity) (not-empty purity)) purity
                           :else "unknown")
            display-name (if (= type :top-level) "do..." name)]
        (println (str "  " node-id "[\"" display-name "\"]:::" purity-class))

        (when (and file line)
          (let [abs-path (if (str/starts-with? file "/") 
                             file 
                             (str project-root "/" file))
                url (str "vscode://file" abs-path ":" line)]
             (println (str "  click " node-id " \"" url "\" \"Open in VS Code\""))))

        (doseq [call calls]
          ;; Smart Link Resolution
          (try
            (let [call-name (name call)
                  call-ns (namespace call)
                  target (or 
                          ;; 1. Exact match (already FQN)
                          (when (contains? known-ids call) call)
                          ;; 2. Local match (same NS)
                          (when (and node-ns (not call-ns))
                            (let [local-fqn (keyword node-ns (str call-name))]
                              (when (contains? known-ids local-fqn)
                                local-fqn))))]
              (when target
                (println (str "  " node-id " --> " (safe-id target)))))
            (catch Exception e
              (binding [*out* *err*]
                (println "Error processing call:" call "Type:" (type call) "Error:" (.getMessage e))))))))
    
    (db/close! conn)))
