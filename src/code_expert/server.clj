(ns code-expert.server
  (:require [babashka.fs :as fs]
            [clojure.string :as str]
            [code-expert.visualize :as viz]
            [org.httpkit.server :as http]
            [clojure.java.browse :as browse]))

(defn generate-graph [ns-filter]
  (with-out-str (viz/-main ns-filter)))

(defn get-query-param [query-string param]
  (when query-string
    (second (re-find (re-pattern (str param "=([^&]*)")) query-string))))

(defn handler [req]
  (let [uri (:uri req)
        query-string (:query-string req)]
    (cond
      (= uri "/")
      (let [ns-filter (get-query-param query-string "ns")
            graph (generate-graph ns-filter)
            template (slurp "www/index.html")
            html (str/replace template "LOADING_GRAPH" graph)]
        {:status 200
         :headers {"Content-Type" "text/html"}
         :body html})

      :else
      {:status 404
       :body "Not Found"})))

(defn -main [& _args]
  (let [port 9999]
    (println "Starting server on http://localhost:" port)
    (http/run-server handler {:port port})
    (browse/browse-url (str "http://localhost:" port))
    @(promise))) ;; Wait forever