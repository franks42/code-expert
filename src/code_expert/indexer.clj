(ns code-expert.indexer
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [edamame.core :as e]
            [code-expert.db :as db]))

;; --- Purity Heuristics ---
(def hard-impure-symbols
  #{'reset! 'swap! 'vreset! 'vswap!
    'spit 'jdbc/execute! 'jdbc/execute-one!
    'future 'thread 'p/process 'p/shell})

(def soft-impure-symbols
  #{'println 'print 'prn 'pr-str 'log/info 'log/debug 'log/error 't/trace!})

(defn check-purity [form]
  (let [found-hard (atom false)
        found-soft (atom false)]
    (walk/prewalk
      (fn [x]
        (when (symbol? x)
          (when (contains? hard-impure-symbols x) (reset! found-hard true))
          (when (contains? soft-impure-symbols x) (reset! found-soft true)))
        x)
      form)
    (cond
      @found-hard :impure
      @found-soft :pure-ish
      :else       :pure)))

;; --- Source Extraction ---
(defn get-lines [s]
  (str/split s #"\r?\n" -1))

(defn extract-source [lines {:keys [row end-row]}]
  (when (and row end-row)
    (let [start-idx (dec row)
          end-idx   end-row
          start-idx (max 0 start-idx)
          end-idx   (min (count lines) end-idx)]
      (str/join "\n" (subvec (vec lines) start-idx end-idx)))))

;; --- Parsing ---
(defn extract-calls [form]
  (let [calls (atom #{})]
    (walk/prewalk
      (fn [x]
        (when (and (symbol? x) (not= (first form) x)) ;; Exclude the function name itself
          (swap! calls conj (keyword (str x))))
        x)
      form)
    @calls))

(defn analyze-file [file-path root-path]
  (let [content (slurp (str file-path))
        lines   (get-lines content)
        rel-path (str (fs/relativize root-path file-path))
        forms   (try
                  (e/parse-string-all content
                                      {:auto-resolve identity
                                       :features #{:clj}
                                       :regex true
                                       :deref true
                                       :quote true
                                       :fn true
                                       :var true
                                       :set hash-set
                                       :read-eval (fn [_] nil)})
                  (catch Exception e
                    (println "Error parsing" rel-path ":" (.getMessage e))
                    []))]
    (loop [forms forms
           current-ns nil
           entities []]
      (if (empty? forms)
        entities
        (let [form (first forms)
              op   (when (seq? form) (first form))
              m    (meta form)
              new-ns (if (= op 'ns) (second form) current-ns)]
          (if (and op (#{'defn 'def 'defmacro 'ns} op))
            (let [name (second form)
                  type op
                  id   (if current-ns
                         (keyword (str current-ns) (str name))
                         (keyword (str name)))
                  purity (if (#{'defn 'def 'defmacro} type) (check-purity form) :unknown)
                  source (extract-source lines m)
                  doc (when (string? (nth form 2 nil)) (nth form 2))
                  calls (if (#{'defn 'def 'defmacro} type) (extract-calls form) #{})
                  entity (into {} (filter val {:code/id id
                                               :code/name name
                                               :code/ns (keyword (str new-ns))
                                               :code/type type
                                               :code/file rel-path
                                               :code/line (:row m)
                                               :code/purity purity
                                               :code/source source
                                               :code/doc doc
                                               :code/calls calls}))]
              (recur (rest forms) new-ns (conj entities entity)))
            
            ;; Handle top-level expressions (impure by definition)
            (if (and current-ns (seq? form))
              (let [name (symbol (str "top-level-" (:row m)))
                    type :top-level
                    id   (keyword (str current-ns) (str name))
                    purity :impure
                    source (extract-source lines m)
                    doc "Top-level side-effect"
                    calls (extract-calls form)
                    entity (into {} (filter val {:code/id id
                                                 :code/name name
                                                 :code/ns (keyword (str new-ns))
                                                 :code/type type
                                                 :code/file rel-path
                                                 :code/line (:row m)
                                                 :code/purity purity
                                                 :code/source source
                                                 :code/doc doc
                                                 :code/calls calls}))]
                (recur (rest forms) new-ns (conj entities entity)))
              (recur (rest forms) new-ns entities))))))))

(defn index-project! [project-path db-path]
  (println "Indexing project:" project-path "to" db-path)
  (let [conn (db/get-conn db-path)
        files (concat (fs/glob (fs/path project-path "src") "**/*.clj")
                      (fs/glob (fs/path project-path "modules") "**/*.clj"))]
    (println "Found" (count files) "files.")
    (doseq [file files]
      (let [entities (analyze-file file project-path)]
        (when (seq entities)
          (db/transact! conn entities))))
    (println "Indexing complete.")
    (db/close! conn)))
