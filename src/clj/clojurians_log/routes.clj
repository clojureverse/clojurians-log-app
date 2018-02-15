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
     (GET "/" request
       (let [db (d/db conn)]
         (-> request
             context
             (assoc :data/title "Clojurians Slack Log"
                    :data/channels (queries/channel-list db))
             views/channel-list-page
             response/render)))

     (GET "/:channel" [channel :as request]
       (let [db (d/db conn)]
         (-> request
             context
             (assoc :data/title (str "Clojurians Slack Log | " channel)
                    :data/days (queries/channel-days db channel)
                    :data/channel-name channel)
             views/channel-page
             response/render)))

     ;; https://clojurians-log.clojureverse.org/clojure/2017-11-15.html
     (GET "/:channel/:date.html" [channel date :as request]
       (let [db (d/db conn)]
         (-> request
             context
             (assoc :data/channel (queries/channel db channel)
                    :data/channels (queries/channel-list db date)
                    :data/messages (queries/channel-day-messages db channel date)
                    :data/title (str channel " " date " | Clojurians Slack Log")
                    :data/date date)
             views/log-page
             response/render)))

     (resources "/"))))

(comment
  (data/load-channel-messages {:request {:params {:channel "clojure" :year "2017" :month "01" :day "01"}}}))
