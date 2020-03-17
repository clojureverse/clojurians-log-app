(ns user
  (:require [clojurians-log.application :as app]
            [clojurians-log.config :as config :refer [config]]
            [garden-watcher.core :refer [new-garden-watcher]]
            [reloaded.repl :refer [system]]
            [clojurians-log.datomic :as d]
            [sc.api]))

(defn dev-system []
  (let [config (config :dev)]
    (alter-var-root #'app/config (constantly config))
    (-> (app/prod-system config)
        (assoc :garden-watcher (new-garden-watcher ['clojurians-log.styles])))))

(reloaded.repl/set-init! #(dev-system))

;; Set up aliases so they don't accidentally
;; get scrubbed from the namespace declaration
(def start reloaded.repl/start)
(def stop reloaded.repl/stop)
(def go reloaded.repl/go)
(def reset reloaded.repl/reset)
(def reset-all reloaded.repl/reset-all)
(def clear reloaded.repl/clear)

(defn conn []
  (get-in reloaded.repl/system [:datomic :conn]))

(defn db []
  (d/db (conn)))

(defn update-cache-time! [new-cache-time]
  "Changes how long to ask http clients to cache each of the messages pages"
  (swap! (get-in system [:config :value])
         update-in
         [:message-page :cache-time]
         (constantly new-cache-time))
  true)

(comment
  (go)
  (reset)
  (reset-all)
  (use 'clojurians-log.repl)
  (load-demo-data! "../clojurians-log-demo-data")
  #_:end)
