(ns clojurians-log.routes
  (:require [clojure.java.io :as io]
            [clojurians-log.data :as data]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.response :as response]
            [clojurians-log.views :as views]
            [compojure.core :refer [GET routes]]
            [compojure.route :refer [resources]]
            [datomic.api :as d]
            [ring.util.response :refer [response]]))

(defn context [request]
  {:request request})

(defn home-routes [endpoint]
  (let [conn (get-in endpoint [:datomic :conn])]
    (routes
     (GET "/" _
       (-> "public/index.html"
           io/resource
           io/input-stream
           response
           (assoc :headers {"Content-Type" "text/html; charset=utf-8"})))

     ;; https://clojurians-log.clojureverse.org/clojure/2017-11-15.html
     (GET "/:channel/:date.html" [channel date :as request]
       (-> request
           context
           data/load-channel-messages
           (assoc :data/channels (queries/channel-list (d/db conn) date))
           #_(assoc :data/messages )
           views/log-page
           response/render))

     (resources "/"))))

(comment
  (data/load-channel-messages {:request {:params {:channel "clojure" :year "2017" :month "01" :day "01"}}}))
