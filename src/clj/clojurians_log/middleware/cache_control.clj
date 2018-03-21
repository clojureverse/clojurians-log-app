(ns clojurians-log.middleware.cache-control
  (:require [clojurians-log.time-util :as time-util]
            [java-time :as jt]))

(defn wrap-cache-control [handler cache-time]
  (fn [req]
    (let [res (handler req)
          last-modified (-> time-util/UTC
                            jt/zoned-date-time
                            time-util/time->html-ts)]
      (prn req)
      (cond-> res
        (= 200 (:status res))
        (update :headers merge {"Cache-Control" (str "public, max-age: " cache-time)
                                "Last-Modified" last-modified})))))
