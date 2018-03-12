(ns clojurians-log.test-helper
  (:require [datomic.api :as d]
            [clojure.java.io :as io]
            [clojurians-log.db.schema :as schema]))

(defn test-db
  ([]
   (test-db "two-channels-two-days"))
  ([fixture]
   (let [fixture (str  "clojurians-log/test-data/" fixture ".edn")
         url  (str "datomic:mem:" (gensym "test_db"))
         _    (d/create-database url)
         conn (d/connect url)]
     @(d/transact conn schema/full-schema)
     (doseq [tx (-> (io/resource fixture) slurp read-string)]
       @(d/transact conn tx))
     [conn (d/db conn)])))
