(ns clojurians-log.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]))

(defrecord Datomic [uri conn]
  component/Lifecycle
  (start [component]
    (let [db (d/create-database uri)
          conn (d/connect uri)]
      (assoc component :conn conn)))
  (stop [component]
    (when conn (d/release conn))
    (assoc component :conn nil)))

(defn new-datomic-db [config]
  (map->Datomic {:uri (get-in config [:on-prem :uri])}))

(def create-database d/create-database)
(def connect d/connect)
(def db d/db)
(def q d/q)
(def transact d/transact)
(def transact-async d/transact-async)
