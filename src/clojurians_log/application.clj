(ns clojurians-log.application
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [system.components.endpoint :refer [new-endpoint]]
            [clojurians-log.components.server-info :refer [server-info]]
            [clojurians-log.components.datomic-schema :refer [new-datomic-schema]]
            [clojurians-log.components.indexer :refer [new-indexer]]
            [clojurians-log.components.pohjavirta :refer [new-pohjavirta]]
            [system.components.handler :refer [new-handler]]
            [system.components.middleware :refer [new-middleware]]
            [system.components.datomic :refer [new-datomic-db]]
            [clojurians-log.config :as config]
            [clojurians-log.routes :as routes]
            [clojure.java.io :as io]
            [reloaded.repl]))

(def config nil)

(defn system []
  reloaded.repl/system)

(defrecord ValueComponent [value]
  component/Lifecycle
  (start [component] component)
  (stop [component] component))

(defn prod-system [{:keys [datomic http] :as config}]
  (component/system-map
   :config     (->ValueComponent (atom config))
   :routes     (-> (new-endpoint (fn [endpoint]
                                   (let [router (reitit.ring/router routes/routes)
                                         handler (reitit.ring/ring-handler router)]
                                     (fn [request]
                                       (handler (assoc request :endpoint endpoint))))))
                   (component/using [:datomic :config]))
   :middleware (new-middleware {:middleware clojurians-log.config/middleware-stack})
   :handler    (-> (new-handler)
                   (component/using [:routes :middleware]))
   :http       (-> (new-pohjavirta {:port (:port http)})
                   (component/using [:handler]))
   :server-info (server-info (:port http))
   :datomic (new-datomic-db (:uri datomic))
   :datomic-schema (-> (new-datomic-schema)
                       (component/using [:datomic]))
   :indexer (-> (new-indexer)
                (component/using [:datomic]))))

(defn -main [& [config-file]]
  (let [conf (if (and config-file (.exists (io/file config-file)))
               (config/config (io/file config-file) :prod)
               (config/config :prod))]
    (alter-var-root #'config (constantly conf))
    (reloaded.repl/set-init! #(prod-system conf))
    (reloaded.repl/go)))
