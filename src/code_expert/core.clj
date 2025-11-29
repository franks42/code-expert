(ns code-expert.core
  (:require [code-expert.indexer :as indexer]))

(defn -main [& args]
  (if (empty? args)
    (println "Usage: bb index <path-to-project>")
    (let [project-path (first args)
          db-path ".db/codebase"]
      (indexer/index-project! project-path db-path))))