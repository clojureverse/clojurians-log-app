(ns user
  (:require [reloaded.repl :as reloaded]))

(reloaded/set-init! #((requiring-resolve 'clojurians-log.application/dev-system)
                      ((requiring-resolve 'clojurians-log.config/config) :dev)))

;; Set up aliases so they don't accidentally
;; get scrubbed from the namespace declaration
(def start reloaded/start)
(def stop reloaded/stop)
(def go reloaded/go)
(def reset reloaded/reset)
(def reset-all reloaded/reset-all)
(def clear reloaded/clear)

(defn conn []
  (get-in reloaded/system [:datomic :conn]))

(defn db []
  ((requiring-resolve 'clojurians-log.datomic/db) (conn)))

(defn update-cache-time! [new-cache-time]
  "Changes how long to ask http clients to cache each of the messages pages"
  (swap! (get-in reloaded/system [:config :value])
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

  )
