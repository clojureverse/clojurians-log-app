(ns clojurians-log.application
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [system.components.endpoint :refer [new-endpoint]]
            [clojurians-log.components.server-info :refer [server-info]]
            [clojurians-log.components.datomic-schema :refer [new-datomic-schema]]
            [system.components.handler :refer [new-handler]]
            [system.components.middleware :refer [new-middleware]]
            [system.components.http-kit :refer [new-web-server]]
            [system.components.datomic :refer [new-datomic-db]]
            [clojurians-log.config :as config]
            [clojurians-log.routes :refer [home-routes]]
            [clojure.java.io :as io]))

(def config nil)
(def system nil)

(defn app-system [{:keys [datomic http] :as config}]
  (component/system-map
   :routes     (-> (new-endpoint (fn [endpoint]
                                   (fn [request]
                                     ((home-routes endpoint) request))))
                   (component/using [:datomic]))
   :middleware (new-middleware {:middleware (:middleware http)})
   :handler    (-> (new-handler)
                   (component/using [:routes :middleware]))
   :http       (-> (new-web-server (:port http))
                   (component/using [:handler]))
   :server-info (server-info (:port http))
   :datomic (new-datomic-db (:uri datomic))
   :datomic-schema (-> (new-datomic-schema)
                       (component/using [:datomic]))))

(defn -main [& [config-file]]
  (let [conf (if (and config-file (.exists (io/file config-file)))
               (config/config (io/file config-file) :prod)
               (config/config :prod))]
    (alter-var-root #'config (constantly conf))
    (alter-var-root #'system (constantly
                              (-> conf
                                  app-system
                                  component/start)))))
