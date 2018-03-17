(ns user
  (:require [clojurians-log.application :as app]
            [clojurians-log.config :as config :refer [config]]
            [clojurians-log.repl :as repl]
            [figwheel-sidecar.config :as fw-config]
            [figwheel-sidecar.system :as fw-sys]
            [garden-watcher.core :refer [new-garden-watcher]]
            [lambdaisland.repl-tools.browse-url :as browse-url]
            [lambdaisland.repl-tools.ring-history :as ring-history]
            [reloaded.repl :refer [system]]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.session.store :as session-store]
            [system.components.middleware :refer [new-middleware]]
            [datomic.api :as d]
            [sc.api]))

(defn dev-system []
  (let [config (config :dev)]
    (alter-var-root #'app/config (constantly config))
    (-> (app/prod-system config)
        (ring-history/inject-ring-history)
        (assoc :middleware (new-middleware {:middleware (clojurians-log.config/middleware-stack :dev)})
               :figwheel-system (fw-sys/figwheel-system (fw-config/fetch-config))
               :css-watcher (fw-sys/css-watcher {:watch-paths ["resources/public/css"]})
               :garden-watcher (new-garden-watcher ['clojurians-log.styles])
               #_#_:browse-url (browse-url/new-browse-url-component (str "http://localhost:" (get-in config [:http :port])))))))

(reloaded.repl/set-init! #(dev-system))

(defn cljs-repl []
  (fw-sys/cljs-repl (:figwheel-system system)))

;; Set up aliases so they don't accidentally
;; get scrubbed from the namespace declaration
(def start reloaded.repl/start)
(def stop reloaded.repl/stop)
(def go reloaded.repl/go)
(def reset reloaded.repl/reset)
(def reset-all reloaded.repl/reset-all)
(def clear reloaded.repl/clear)

(defn last-request
  ([]
   (last-request 0))
  ([n]
   (ring-history/last-request (:ring-history reloaded.repl/system) n)))

(defn last-response
  ([]
   (last-request 0))
  ([n]
   (ring-history/last-response (:ring-history reloaded.repl/system) n)))

(defn session-id []
  (-> (last-request)
      cookies/cookies-request
      (get-in [:cookies "ring-session" :value])))

(defn session []
  (session-store/read-session
   config/session-store
   (session-id)))

(defn conn []
  (get-in reloaded.repl/system [:datomic :conn]))

(defn db []
  (d/db (conn)))

(defmacro add-dependency
  "Add a dependency at runtime, e.g. (add-dependency [enlive \"1.1.6\"])"
  [dep-vec]
  `(do
     (require 'cemerick.pomegranate)
     (cemerick.pomegranate/add-dependencies
      :coordinates ['~dep-vec]
      :repositories (assoc cemerick.pomegranate.aether/maven-central
                           "clojars" "https://clojars.org/repo"))))

(defn update-cache-time! [new-cache-time]
  "Changes how long to ask http clients to cache each of the messages pages"
  (swap! (get-in system [:config :value])
         update-in
         [:message-page :cache-time]
         (constantly new-cache-time))
  true)
