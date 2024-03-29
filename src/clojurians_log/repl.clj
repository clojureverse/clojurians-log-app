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
            [clojurians-log.datomic :as d]
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

(defn db []
  (d/db (conn)))

(defn load-slack-data!
  "Import Slack users, Channels, and custom emojis."
  []
  (slack/import-users! (conn))
  (slack/import-emojis! (conn))
  (let [channel->db-id (q/channel-id-map (d/db (conn)))
        channels       (mapv import/channel->tx (slack/channels))]
    @(d/transact (conn) (map (fn [{slack-id :channel/slack-id :as ch}]
                               (if-let [db-id (channel->db-id slack-id)]
                                 (assoc ch :db/id db-id)
                                 ch))
                             channels))))

(defn build-indexes! []
  (q/build-indexes! (d/db (conn))))

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
    (doseq [users (->> (io/file directory "users.edn")
                       slurp
                       edn/read-string
                       (partition-all 1000))]

      @(d/transact (conn) users)))
  @(d/transact (conn) (edn/read-string (slurp (str directory "/channels.edn"))))
  (run! load-log-file! (log-files (java.io.File. directory "logs")))
  (build-indexes!))

(defn files-from
  "Get a sequence of log files starting ata given date"
  [date]
  (->> (log-files)
       (drop-while #(not (clojure.string/starts-with? (.getName %) date)))))

(defn load-from
  "Load log files starting from a certain date (a string like \"2019-05-20\")"
  [date]
  (->> (files-from date)
       (run! load-log-file!)))

(defn wrap-catch [f]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable t
        (let [ex-sym (gensym "ex-")]
          (intern *ns* ex-sym t)
          (println (str f " threw " (class t) " see " ex-sym)))))))

(def file->tx
  "Transducer which consumes files and produces transaction data"
  (comp (mapcat #(import/lines-reducible (io/reader %)))
        (keep #(try
                 (json/read-json %)
                 (catch Throwable e
                   (println "Error decoding JSON: " %)
                   (println e)
                   nil)))
        (keep (wrap-catch import/event->tx))))

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
       (or (:message/key msg)
           (:message/key (:reaction/message msg)))))))

(defn load-files!
  "Bulk import a set of files (e.g. from (log-files)), uses multiple threads to speed things up"
  [files]
  (let [make-tx-chan #(async/chan 100 (import/partition-messages tx-size))
        tx-chs       (into [] (repeatedly tx-thread-count make-tx-chan))
        file-ch      (async/chan 100)
        pubsub-ch    (async/chan 100)
        pubsub       (async/pub pubsub-ch (wrap-catch msg-topic))
        conn         (conn)
        counter      (volatile! 0)
        done?        (promise)]

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

    (async/onto-chan file-ch files)

    (doseq [[i tx-ch] (map-indexed vector tx-chs)]
      (async/sub pubsub i tx-ch))

    (async/pipeline-blocking 10 pubsub-ch file->tx file-ch true)

    [counter done?]))

(comment
  ;; Load https://github.com/clojureverse/clojurians-log-demo-data
  (do
    (user/reset)
    (load-demo-data! "/home/arne/github/clojurians-log-demo-data")
    (load-demo-data! "/tmp/demo_data")
    )
  )

(comment
  ;; rlwrap nc localhost 50505
  (use 'clojurians-log.repl)
  (in-ns 'clojurians-log.repl)
  (def result (load-files! (log-files)))
  (load-slack-data!)

  ;; or
  (def result (load-files! (files-from "2021-08-25")))

  ;; see progress
  (future
    (while (not (realized? (second result)))
      (println (java.util.Date.) "\t" @(first result))
      (Thread/sleep 5000)))

  ;; After importing, or you won't see data show up
  (build-indexes!)

  (def result
    (load-files!
     (filter #(and (.contains (str %) "2020-08")
                   (.contains (str %) "backfill"))
             (log-files))))



  ;; old way, this does not use multi-thread core.async magic, and does not
  ;; batch transactions, it literally transacts each slack event separately.
  ;; Very slow but always works.
  (run! load-log-file! (log-files))

  ;; Fetch and store slack data
  (do
    (write-edn "users.edn" (map import/user->tx (slack/users)))
    (write-edn "channels.edn" (map import/channel->tx (slack/channels)))
    (write-edn "emoji.edn" (map import/emoji->tx (slack/emoji))))

  ;; Micro-benchmark some queries, good to check if anything is unreasonably
  ;; slow
  (let [db (db)]
    (time
     (do
       (time (clojurians-log.db.queries/channel-day-messages db "clojurescript" "2018-02-04"))
       (time (clojurians-log.db.queries/thread-messages db '("1517722327.000023" "1517722363.000043" "1517722613.000012" "1517724278.000043" "1517724340.000044" "1517724770.000024" "1517724836.000023" "1517725105.000054")))
       (time (ffirst (clojurians-log.db.queries/channel db "clojurescript")))
       (time (clojurians-log.db.queries/channel-list db "2018-02-04"))
       (time (clojurians-log.db.queries/user-names db #{"U2TUBBPNU"}))
       (time (clojurians-log.db.queries/channel-days db "clojurescript"))

       nil)))

  ;; Original
  "Elapsed time: 18.166254 msecs"
  "Elapsed time: 631.458841 msecs" ; -> we optimized this
  "Elapsed time: 1.568807 msecs"
  "Elapsed time: 16.425878 msecs"
  "Elapsed time: 1.126005 msecs"
  "Elapsed time: 1535.355001 msecs" ; -> and this
  "Elapsed time: 2205.20762 msecs" ; Total

  ;; Latest
  "Elapsed time: 31.38712 msecs" ; -> seems fetching channel-day-messages is
                                 ; slower now
  "Elapsed time: 0.844338 msecs"
  "Elapsed time: 1.582986 msecs"
  "Elapsed time: 0.22545 msecs"
  "Elapsed time: 1.120628 msecs"
  "Elapsed time: 0.00676 msecs"
  "Elapsed time: 37.954533 msecs" ; Total

  )
