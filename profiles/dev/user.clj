(ns user
  (:require [reloaded.repl :as reloaded]))

(defmacro jit [sym]
  `(or (resolve '~sym)
       (do
         (-> '~sym namespace symbol require)
         (resolve '~sym))))

(reloaded/set-init! #((jit clojurians-log.application/dev-system)
                      ((jit clojurians-log.config/config) :dev)))

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
  ((jit clojurians-log.datomic/db) (conn)))

(defn update-cache-time! [new-cache-time]
  "Changes how long to ask http clients to cache each of the messages pages"
  (swap! (get-in reloaded/system [:config :value])
         update-in
         [:message-page :cache-time]
         (constantly new-cache-time))
  true)

(defn browse []
  ((jit clojure.java.browse/browse-url) (str "http://localhost:" (get-in @(jit clojurians-log.application/config) [:http :port]))))

(comment

  (go)
  (reset)
  (reset-all)
  (use 'clojurians-log.repl)
  (load-demo-data! "../clojurians-log-demo-data")

  )
