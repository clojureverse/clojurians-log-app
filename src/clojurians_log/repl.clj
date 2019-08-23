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
            [clojure.tools.reader.edn :as edn]
            [clojure.string :as str]
            [clojure.core.async :as async :refer [>!! <! >! go-loop go <!!]]
            [clojure.data.json :as json]))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
	 (uncaughtException [_ thread throwable]
		 (println (.getMessage throwable)))))

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

(defn load-log-file!
  "Import a single log file. This assumes all channels and users referenced in the
  log file already exist, either by importing directly from Slack (production)
  or from EDN files (demo data)."
  [file]
  (println (str file))
  (let [msgs (filter #(= (:type %) "message") (data/event-seq file))
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
  (run! load-log-file! (log-files (java.io.File. directory "logs"))))

(defn load-from
  "Load log files starting from a certain date (a string like \"2019-05-20\")"
  [date]
  (->> (log-files)
       (drop-while #(not (clojure.string/starts-with? (.getName %) date)))
       (run! load-log-file!)))

(def file->tx
  "Transducer which consumes files and produces transaction data"
  (comp (mapcat #(import/lines-reducible (io/reader %)))
        (keep #(try
                 (json/read-json %)
                 (catch Throwable e
                   (println "Error decoding JSON: " %)
                   (println e)
                   nil)))
        (filter #(= (:type %) "message"))
        (keep import/event->tx)))

(def tx-thread-count
  "The number of threads to use for processing transactions"
  20)

(def tx-size
  "The maximum number of events to process in a single transaction."
  100)

(defn msg-topic
  "Make sure the same message key is always processed by the same thread, so
  retractions and edits are performed in order."
  [msg]
  (let [mod-hash #(mod (.hashCode %) tx-thread-count)]
    (mod-hash
     (if (vector? msg)
       (second (second msg))
       (:message/key msg)))))

(defn load-files!
  "Bulk import a set of files (e.g. from (log-files)), uses multiple threads to speed things up"
  [files]
  (let [tx-chs (into [] (repeatedly tx-thread-count
                                    #(async/chan 100 (import/partition-messages tx-size))))
        file-ch (async/chan 100)
        pubsub-ch (async/chan 100)
        pubsub (async/pub pubsub-ch msg-topic)
        conn  (conn)
        counter (volatile! 0)
        done? (promise)]

    (doseq [tx-ch tx-chs]
      (async/thread
        (loop [tx-data (<!! tx-ch)]
          (if (nil? tx-data)
            (deliver done? :done)
            (do
              (try
                @(d/transact conn tx-data)
                (vswap! counter inc)
                (catch Exception e
                  (println e)))
              (recur (<!! tx-ch)))))))

    (go-loop [[f & files] files]
      (>! file-ch f)
      (if (seq files)
        (recur files)
        (async/close! file-ch)))

    (doseq [i (range tx-thread-count)]
      (async/sub pubsub i (get tx-chs i)))

    (async/pipeline-blocking 10 pubsub-ch file->tx file-ch true)

    [counter done?]))



(comment
  ;; rlwrap nc localhost 50505
  (use 'clojurians-log.repl)
  (load-slack-data!)
  (def result (load-files! (log-files)))
  @(second result)
  result

  ;; old way (slower)
  (run! load-log-file! (log-files))

  ;; incremental
  (load-from "2016-08-04")



  (load-demo-data! "/home/arne/github/clojurians-log-demo-data")


  (do
    (write-edn "users.edn" (slack/users))
    (write-edn "channels.edn" (slack/channels)))

  )
