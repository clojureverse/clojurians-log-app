(ns clojurians-log.test-helper
  (:require [clojure.java.io :as io]
            [clojurians-log.db.schema :as schema]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.xml2hiccup :as x2h]
            [datomic.api :as d]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn]
            [clojurians-log.application :refer [prod-system]]
            [com.stuartsierra.component :as component]
            [clojurians-log.config :refer [config]]
            [hickory.core :as hickory]))

(defn slurp-fixture [fixture-name]
  (-> (str  "clojurians-log/test-data/" fixture-name ".edn")
      io/resource
      slurp
      edn/read-string))

(defn transact-schema [conn]
  @(d/transact conn schema/full-schema))

(defn transact-txs [conn txs]
  (doseq [tx txs]
    @(d/transact conn tx)))

(defn test-conn []
  (let [url (str "datomic:mem:" (gensym "test_db"))]
    (d/create-database url)
    (doto (d/connect url)
      transact-schema)))

(defn test-db
  ([]
   (test-db "two-channels-two-days"))
  ([fixture-name]
   (let [conn (test-conn)]
     (transact-txs conn (slurp-fixture fixture-name))
     (let [db (d/db conn)]
       (queries/build-indexes! db)
       [conn (d/db conn)]))))

(defn html->hiccup [html]
  (-> html
      (hickory/parse)
      (hickory/as-hiccup)))

(defn html-select [hiccup selector]
  (map x2h/xml2hiccup (enlive/select (enlive/html hiccup) selector)))

(defn html-select-1 [hiccup selector]
  (let [els (enlive/select (enlive/html hiccup) selector)]
    (assert (= 1 (count els)))
    (x2h/xml2hiccup (first els))))

(defn system-db-conn [system]
  (get-in system [:datomic :conn]))

(defn system-db [system]
  (d/db (system-db-conn system)))

(defn system-ring-handler [system]
  (get-in system [:handler :handler]))

(defn test-system []
  (-> (prod-system (config :test)) ;; setup a full test system
      (assoc-in [:datomic :uri] (str "datomic:mem:" (gensym "test_db")))
      (dissoc :http)               ;; don't actually start a http server
      (dissoc :server-info)        ;; silence http server startup message
      component/start-system))

(defn system-load-fixture! [system fixture-name]
  (transact-txs (system-db-conn system)
                (slurp-fixture fixture-name))
  (queries/build-indexes! (system-db system))
  nil)
