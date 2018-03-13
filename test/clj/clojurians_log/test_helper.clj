(ns clojurians-log.test-helper
  (:require [clojure.java.io :as io]
            [clojurians-log.db.schema :as schema]
            [clojurians-log.xml2hiccup :as x2h]
            [datomic.api :as d]
            [net.cgrand.enlive-html :as enlive]))

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

(defn html-select [hiccup selector]
  (map x2h/xml2hiccup (enlive/select (enlive/html hiccup) selector)))

(defn html-select-1 [hiccup selector]
  (let [els (enlive/select (enlive/html hiccup) selector)]
    (assert (= 1 (count els)))
    (x2h/xml2hiccup (first els))))
