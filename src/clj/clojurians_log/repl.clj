(ns clojurians-log.repl
  (:require [clojurians-log.application :as app]
            [clojurians-log.slack-api :as slack]
            [clojurians-log.db.import :as import]
            [clojurians-log.data :as data]
            [clojure.java.io :as io]
            [datomic.api :as d]))

(defn conn []
  (get-in app/system [:datomic :conn]))

(defn load-slack-data! []
  (slack/import-users! (conn))
  (slack/import-channels! (conn)))

(defn log-files []
  (->> (get-in app/config [:slack :log-dir])
       io/file
       file-seq
       (remove #(.isDirectory %))))

(defn load-log-file! [file]
  (println (str file))
  (let [msgs (data/event-seq file)]
    (d/transact (conn) (keep import/event->tx msgs))))


(comment
  ;; nc localhost 50505
  (use 'clojurians-log.repl)
  (load-slack-data!)
  (run! load-log-file! (log-files))
  )
