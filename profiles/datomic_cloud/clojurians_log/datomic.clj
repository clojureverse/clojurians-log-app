(ns clojurians-log.datomic
  (:require [com.stuartsierra.component :as component]
            [datomic.client.api :as d]))

(defrecord Datomic [config client conn]
  component/Lifecycle
  (start [component]
    (let [client (d/client config)
          db     (d/create-database client {:db-name (:db-name config)})
          conn   (d/connect client {:db-name (:db-name config)})]
      (assoc component :conn conn)))
  (stop [component]
    (assoc component :conn nil :client nil)))

(defn new-datomic-db [config]
  (map->Datomic {:config (:cloud config)}))
 
(def create-database d/create-database)
(def connect d/connect)
(def db d/db)
(def q d/q)
(def transact (fn [conn data]
                (d/transact conn {:tx-data data})))
(def transact-async transact)
