(ns clojurians-log.repl
  "Helper namespace for manual operations like importing stuff. This namespace is
  not loaded by default, it's mainly useful for devs setting up a local
  instance, or sys admins seeding the production system with data."
  (:require [clojurians-log.application :as app]
            [clojurians-log.slack-api :as slack]
            [clojurians-log.db.queries :as q]
            [clojurians-log.db.import :as import]
            [clojurians-log.data :as data]
            [clojure.java.io :as io]
            [datomic.api :as d]
            [clojure.tools.reader.edn :as edn]))

(defn read-edn [filepath]
  (-> filepath
      slurp
      edn/read-string))

(defn write-edn [filepath data]
  (with-open [w (clojure.java.io/writer filepath)]
    (binding [*print-length* false
              *out* w]
      (pr data))))

(defn conn
  "Reach into the system for the datomic connection.

  Not 100% kosher but remember this is not production code as such."
  []
  (get-in (app/system) [:datomic :conn]))

(defn load-slack-data!
  "Import Slack users and Channels."
  []
  (slack/import-users! (conn))
  (let [channel->db-id (q/channel-id-map (d/db (conn)))
        channels       (mapv import/channel->tx (slack/channels))]
    (d/transact (conn) (map (fn [{slack-id :channel/slack-id :as ch}]
                              (if-let [db-id (channel->db-id slack-id)]
                                (assoc ch :db/id db-id)
                                ch))
                            channels))))

(defn log-files
  "List all files in the given log directory.

  Defaults to the directory supplied in config.edn."
  ([]
   (log-files (get-in app/config [:slack :log-dir])))
  ([directory]
   (->> directory
        io/file
        file-seq
        (filter #(re-find #"\.txt$" (str %)))
        (remove #(.isDirectory %))
        sort)))

(defn load-log-file! [file]
  (println (str file))
  (let [msgs (data/event-seq file)
        events (keep import/event->tx msgs)]
    (doseq [event events]
      @(d/transact-async (conn) [event]))))

(defn load-demo-data!
  "Load the demo data (users, channels, messages).

  First clone https://github.com/plexus/clojurians-log-demo-data, thne call this
  function, pointing at the repository."
  [directory]
  (if-not (conn)
    (println "Can't find Datomic connection. Make sure the system is up and running with (user/go).")
    (doseq [users (->> "/users.edn"
                       (str directory)
                       slurp
                       edn/read-string
                       (partition-all 1000))]
      @(d/transact (conn) users)))
  @(d/transact (conn) (edn/read-string (slurp (str directory "/channels.edn"))))
  (run! load-log-file! (log-files directory)))

(defn load-demo-data2!
  "Load the demo data (users, channels, messages)."
  [directory]
  (if-not (conn)
    (println "Can't find Datomic connection. Make sure the system is up and running with (user/go).")
    (do
      (println "Importing users.edn")
      (doseq [users (->> "/users.edn"
                         (str directory)
                         read-edn
                         (partition-all 1000))]
        (slack/import-users! (conn) users))

      (println "Importing channels.edn")
      (slack/import-channels! (conn) (->> "/channels.edn"
                                          (str directory)
                                          read-edn))

      (println "Import messages")
      (run! load-log-file! (log-files directory)))))

(comment
  ;; nc localhost 50505
  (use 'clojurians-log.repl)
  (load-slack-data!)
  (run! load-log-file! (log-files))

  (load-demo-data! "/home/arne/github/clojurians-log-demo-data")


  (do
    (write-edn "users.edn" (slack/users))
    (write-edn "channels.edn" (slack/channels)))

  )
