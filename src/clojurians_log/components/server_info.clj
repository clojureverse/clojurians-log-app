(ns clojurians-log.components.server-info
  (:require [com.stuartsierra.component :as component]))

(defrecord ServerInfoPrinter [http]
  component/Lifecycle
  (start [component]
    (println "Started clojurians-log on" (str "http://" (:host http) ":" (:port http)))
    component)
  (stop [component]
    component))

(defn server-info [http]
  (->ServerInfoPrinter http))
