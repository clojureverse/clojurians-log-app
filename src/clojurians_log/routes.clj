(ns clojurians-log.routes
  (:require [clojure.java.io :as io]
            [clojurians-log.data :as data]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.response :as response]
            [clojurians-log.views :as views]
            [clojurians-log.slack-messages :as slack-messages]
            [clojurians-log.time-util :as time-util]
            [clojurians-log.routes-def :refer [routes]]
            [java-time :as jt]
            [compojure.route :refer [resources]]
            [datomic.api :as d]
            [ring.util.response :refer [response]]
            [bidi.ring]
            [bidi.bidi :as bidi]))

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

(defn- merge-thread-messages
  "Attach thread messages as :message/children to their parent message"
  [messages thread-messages]
  (let [messages-by-thread-ts (group-by :message/thread-ts thread-messages)]
    (map (fn[msg]
           (if-let [children (get messages-by-thread-ts (:message/ts msg))]
             (assoc msg :message/children children)
             msg))
         messages)))

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
            thread-messages (queries/thread-messages db (map #(:message/ts %) messages))
            user-ids (slack-messages/extract-user-ids messages)]

        (if (empty? messages)
          (-> (ring.util.response/response "Oops! No messages here!")
              (ring.util.response/content-type "text/html; charset=utf-8")
              (ring.util.response/status 404))

          (-> request
              context
              (assoc :data/channel (queries/channel db channel)
                     :data/channels (queries/channel-list db date)
                     :data/messages (merge-thread-messages messages thread-messages)
                     :data/target-message (some #(when (= (:message/ts %) ts) %) (apply conj messages thread-messages))
                     :data/usernames (into {} (queries/user-names db user-ids))
                     :data/channel-days (queries/channel-days db channel)
                     :data/title (str channel " " date " | Clojurians Slack Log")
                     :data/date date
                     :data/http-origin (get-in config [:http :origin]))
              views/log-page
              (assoc-in [:response/headers "Cache-Control"] (str "public, max-age: " cache-time))
              (assoc-in [:response/headers "Last-Modified"] (time-util/time->html-ts (jt/zoned-date-time time-util/UTC)))
              response/render))))))

(defn- db-from-endpoint [endpoint]
  (->> (get-in endpoint [:datomic :conn])
       (d/db)))

(defn index-route [endpoint request]
  (let [db (db-from-endpoint endpoint)]
    (-> request
        context
        (assoc :data/title "Clojurians Slack Log"
               :data/channels (queries/channel-list db))
        views/channel-list-page
        response/render)))

(defn channel-history-route [endpoint request]
  (let [db (db-from-endpoint endpoint)
        {:keys [channel]} (:route-params request)]
    (-> request
        context
        (assoc :data/title (str "Clojurians Slack Log | " channel)
               :data/channel-days (queries/channel-days db channel)
               :data/channel-name channel)
        views/channel-page
        response/render)))

(defn- dispatch
  "Used internally to resolve a route-handler symbol (as configured in clojurians-logs.route-defs)
  into a function, then calling the function with the supplied `request`"
  [endpoint handler-sym]
  (fn [request]
    (if (fn? handler-sym)
      (handler-sym endpoint request)
      ((var-get (ns-resolve 'clojurians-log.routes handler-sym)) endpoint request))))

(defn home-routes [{:keys [config] :as endpoint}]
  (bidi.ring/make-handler routes (partial dispatch endpoint)))

(comment
  (data/load-channel-messages {:request {:params {:channel "clojure" :year "2017" :month "01" :day "01"}}}))
