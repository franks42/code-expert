(ns code-expert.db
  (:require [babashka.pods :as pods]))

(pods/load-pod 'huahaiy/datalevin "0.9.27")

(require '[pod.huahaiy.datalevin :as d])

(def schema
  {:code/id          {:db/valueType :db.type/keyword  :db/unique :db.unique/identity}
   :code/ns          {:db/valueType :db.type/keyword  :db/index true}
   :code/name        {:db/valueType :db.type/symbol   :db/index true}
   :code/type        {:db/valueType :db.type/keyword} ; :defn, :def, :defmacro, :ns
   :code/source      {:db/valueType :db.type/string}
   :code/doc         {:db/valueType :db.type/string   :db/fulltext true}
   :code/file        {:db/valueType :db.type/string}
   :code/line        {:db/valueType :db.type/long}
   :code/calls       {:db/valueType :db.type/keyword  :db/cardinality :db.cardinality/many}
   :code/purity      {:db/valueType :db.type/keyword} ; :pure, :pure-ish, :impure, :unknown
   :code/hash        {:db/valueType :db.type/string}})

(defn get-conn [db-path]
  (d/get-conn db-path schema))

(defn transact! [conn tx-data]
  (d/transact! conn tx-data))

(defn close! [conn]
  (d/close conn))