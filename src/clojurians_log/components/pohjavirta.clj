(ns clojurians-log.components.pohjavirta
  (:require [com.stuartsierra.component :as component]
            [lang-utils.core :refer [seek]]
            [pohjavirta.response :as response]
            [pohjavirta.server :as pohjavirta])
  (:import (io.undertow.io IoCallback Sender)
           (io.undertow.server HttpServerExchange)
           (java.io File FileInputStream InputStream)
           (java.nio ByteBuffer)))

(defrecord WebServer [options server routes middleware]
  component/Lifecycle
  (start [component]
    (let [handler (:routes routes)
          wrap-mw (:wrap-mw middleware)
          server (pohjavirta/create (wrap-mw handler) options)]
      (pohjavirta/start server)
      (assoc component :server server)))
  (stop [component]
    (when server
      (pohjavirta/stop server)
      (dissoc component :server))))

(defn new-pohjavirta [options]
  (map->WebServer {:options options}))
