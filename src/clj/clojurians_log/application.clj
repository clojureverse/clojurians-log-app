(ns clojurians-log.application
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [clojurians-log.components.server-info :refer [server-info]]
            [system.components.endpoint :refer [new-endpoint]]
            [system.components.handler :refer [new-handler]]
            [system.components.middleware :refer [new-middleware]]
            [system.components.http-kit :refer [new-web-server]]
            [clojurians-log.config :refer [config]]
            [clojurians-log.routes :refer [home-routes]]))

(defn app-system [config]
  (component/system-map
   :routes     (new-endpoint (fn [endpoint]
                               (fn [request]
                                 ((home-routes endpoint) request))))
   :middleware (new-middleware {:middleware (:middleware config)})
   :handler    (-> (new-handler)
                   (component/using [:routes :middleware]))
   :http       (-> (new-web-server (:http-port config))
                   (component/using [:handler]))
   :server-info (server-info (:http-port config))))

(defn -main [& _]
  (let [config (config)]
    (-> config
        app-system
        component/start)))
