(ns clojurians-log.application
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [clojurians-log.components.server-info :refer [server-info]]
            [clojurians-log.components.datomic-schema :refer [new-datomic-schema]]
            [clojurians-log.components.indexer :refer [new-indexer]]
            [system.components.endpoint :refer [new-endpoint]]
            [system.components.middleware :refer [new-middleware]]
            [system.components.handler :refer [new-handler]]
            [system.components.jetty :refer [new-jetty]]
            [clojurians-log.datomic :refer [new-datomic-db]]
            [clojurians-log.config :as config]
            [clojurians-log.routes :as routes]
            [clojure.java.io :as io]
            [reloaded.repl]))

(require 'compojure.core)

(def config nil)

(defn system []
  reloaded.repl/system)

(defrecord ValueComponent [value]
  component/Lifecycle
  (start [component] component)
  (stop [component] component))

(defn prod-system [{:keys [datomic http] :as cfg}]
  (alter-var-root #'config (constantly cfg))
  (component/system-map
   :config     (->ValueComponent (atom cfg))
   :routes     (-> (new-endpoint (fn [endpoint]
                                   (let [router (reitit.ring/router routes/routes)
                                         handler (reitit.ring/ring-handler router (reitit.ring/redirect-trailing-slash-handler {:method :strip}))]
                                     (fn [request]
                                       (handler (assoc request
                                                       :endpoint endpoint
                                                       :config cfg
                                                       ::slack-instance (get-in cfg [:slack :instance])))))))
                   (component/using [:datomic :config]))
   :handler (-> (new-handler)
                (component/using [:routes :middleware]))
   :middleware (new-middleware {:middleware clojurians-log.config/middleware-stack})
   :http       (-> (new-jetty :port (:port http) :host (:host http))
                   (component/using [:handler]))
   :server-info (server-info http)
   :datomic (new-datomic-db datomic)
   :datomic-schema (-> (new-datomic-schema)
                       (component/using [:datomic]))
   :indexer (-> (new-indexer)
                (component/using [:datomic]))))

(defn dev-system [cfg]
  ;; Late resolve garden watcher, because in prod we won't have it on the filesystem
  (let [new-garden-watcher (requiring-resolve 'garden-watcher.core/new-garden-watcher)
        style-nss ['clojurians-log.styles]]
    (-> (prod-system cfg)
        #_(assoc :garden-watcher (new-garden-watcher style-nss)))))

(defn -main [& [config-file]]
  (let [conf (if (and config-file (.exists (io/file config-file)))
               (config/config (io/file config-file) :prod)
               (config/config :prod))]
    (reloaded.repl/set-init! #(prod-system conf))
    (reloaded.repl/go)))
