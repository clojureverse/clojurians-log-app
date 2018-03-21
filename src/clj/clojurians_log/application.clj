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
            [clojure.java.io :as io]
            [reloaded.repl]))

(def config nil)

(defn system []
  reloaded.repl/system)

(defn prod-system [{:keys [datomic http cache-time] :as config}]
  (component/system-map
   :routes         (-> (new-endpoint home-routes)
                       (assoc :http-origin (get-in config [:http :origin]))
                       (component/using [:datomic]))
   :middleware     (new-middleware (select-keys config [:middleware]))
   :handler        (-> (new-handler)
                       (component/using [:routes :middleware]))
   :http           (-> (new-web-server (:port http))
                       (component/using [:handler]))
   :server-info    (server-info (:port http))
   :datomic        (new-datomic-db (:uri datomic))
   :datomic-schema (-> (new-datomic-schema)
                       (component/using [:datomic]))))

(defn -main [& [config-file]]
  (let [conf (if (and config-file (.exists (io/file config-file)))
               (config/config (io/file config-file) :prod)
               (config/config :prod))]
    (alter-var-root #'config (constantly conf))
    (reloaded.repl/set-init! #(prod-system conf))
    (reloaded.repl/go)))
