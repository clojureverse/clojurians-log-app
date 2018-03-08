(ns clojurians-log.routes
  (:require [clojure.java.io :as io]
            [clojurians-log.data :as data]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.response :as response]
            [clojurians-log.views :as views]
            [clojurians-log.slack-messages :as slack-messages]
            [clojurians-log.time-util :as time-util]
            [java-time :as jt]
            [compojure.core :refer [GET routes]]
            [compojure.route :refer [resources]]
            [datomic.api :as d]
            [ring.util.response :refer [response]]))

(defn context [request]
  {:request request})

(defn message-page-might-have-updated? [page-date-str last-fetch-time-ts]
  (if (nil? last-fetch-time-ts)
    true
    (let [fetch-date (-> last-fetch-time-ts
                         time-util/html-ts->time
                         jt/zoned-date-time
                         jt/local-date)
          page-date (jt/local-date time-util/inst-day-formatter page-date-str)]
      (.isEqual fetch-date page-date))))

(defn log-route [endpoint request]
  (let [config                    @(get-in endpoint [:config :value])
        conn                      (get-in endpoint [:datomic :conn])
        {:keys [channel date ts]} (:route-params request)
        cache-time                (get-in config [:message-page :cache-time] 0)]

    ;; Since we're displaying a log, presumably, all of the content is permanently cachable
    ;; Message page content also most likely have not changed and does not require any processing/page-generation.
    ;; In those cases, skip page generate entirely, instead of generating the contents, then have the ring
    ;; middleware respond with a 304.
    (if (and (not (zero? cache-time))
             (not (message-page-might-have-updated? date (get-in request [:headers "if-modified-since"]))))
      (-> (ring.util.response/response nil)
          (ring.util.response/status 304))

      (let [db       (d/db conn)
            messages (queries/channel-day-messages db channel date)
            user-ids (slack-messages/extract-user-ids messages)]
        (-> request
            context
            (assoc :data/channel (queries/channel db channel)
                   :data/channels (queries/channel-list db date)
                   :data/messages messages
                   :data/target-message (some #(when (= (:message/ts %) ts) %) messages)
                   :data/usernames (into {} (queries/user-names db user-ids))
                   :data/channel-days (queries/channel-days db channel)
                   :data/title (str channel " " date " | Clojurians Slack Log")
                   :data/date date
                   :data/hostname (get-in config [:http :hostname]))
            views/log-page
            (assoc-in [:response/headers "Cache-Control"] (str "public, max-age: " cache-time))
            (assoc-in [:response/headers "Last-Modified"] (time-util/time->html-ts (jt/zoned-date-time time-util/UTC)))
            response/render)))))

(defn home-routes [{:keys [config] :as endpoint}]
  (let [conn (get-in endpoint [:datomic :conn])]
    (routes
     (GET "/healthcheck" _
       {:headers {"Content-Type" "text/plain"}
        :body "OK"})

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
     (GET "/:channel/:date.html" request
       (log-route endpoint request))

     (GET "/:channel/:date/:ts" [channel date ts :as request]
       (log-route endpoint request))

     (resources "/"))))

(comment
  (data/load-channel-messages {:request {:params {:channel "clojure" :year "2017" :month "01" :day "01"}}}))
