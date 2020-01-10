(ns clojurians-log.components.pohjavirta
  (:require [com.stuartsierra.component :as component]
            [lang-utils.core :refer [seek]]
            [pohjavirta.response :as response]
            [pohjavirta.server :as pohjavirta])
  (:import (io.undertow.io IoCallback Sender)
           (io.undertow.server HttpServerExchange)
           (java.io File FileInputStream InputStream)
           (java.nio ByteBuffer)))

(defrecord WebServer [options server handler]
  component/Lifecycle
  (start [component]
    (let [handler (if (fn? handler) handler (:handler (val (seek (comp :handler val) component))))
          server (pohjavirta/create handler options)]
      (pohjavirta/start server)
      (assoc component :server server)))
  (stop [component]
    (when server
      (pohjavirta/stop server)
      (dissoc component :server))))

(defn new-pohjavirta [options]
  (map->WebServer {:options options}))

(extend-protocol response/BodySender
  InputStream
  (send-body [stream exchange]
    (let [bytes (byte-array 8192)
          send-more (fn send-more [^Sender sender]
                      (let [count (.read stream bytes 0 8192)]
                        (cond
                          (< 0 count)
                          (.send sender
                                 (ByteBuffer/wrap bytes 0 count)
                                 (reify IoCallback
                                   (onComplete [_ _ sender]
                                     (send-more sender))
                                   (onException [_ _ _ _])))

                          (= -1 count)
                          (.close stream))))]
      (send-more (.getResponseSender ^HttpServerExchange exchange))))

  File
  (send-body [file exchange]
    (response/send-body (FileInputStream. file) exchange)))
