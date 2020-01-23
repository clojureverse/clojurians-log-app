(ns clojurians-log.components.datomic-schema
  (:require [com.stuartsierra.component :as component]
            [clojurians-log.datomic :as d]
            [clojurians-log.db.schema :as schema]))

(defrecord DatomicSchema [datomic]
  component/Lifecycle
  (start [this]
    (d/transact (:conn datomic)
                (if d/cloud?
                  (map #(dissoc % :db/index) schema/full-schema)
                  schema/full-schema)))
  (stop [this]))

(defn new-datomic-schema []
  (map->DatomicSchema {}))
