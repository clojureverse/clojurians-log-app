(ns clojurians-log.components.indexer
  (:require [com.stuartsierra.component :as component]
            [datomic.api :as d]
            [clojurians-log.db.queries :as queries]))

(defrecord Indexer [datomic]
  component/Lifecycle
  (start [this]
    (let [thread
          (Thread.
           (fn []
             (queries/build-indexes! (d/db (:conn datomic)))
             (Thread/sleep 3600)
             (recur)))]
      (.start thread)
      (assoc this :thread thread)))

  (stop [this]
    (when-let [thread (:thread this)]
      (.interrupt thread))
    (dissoc this :thread)))

(defn new-indexer []
  (map->Indexer {}))
