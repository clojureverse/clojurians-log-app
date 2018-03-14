(ns clojurians-log.routes
  (:require [clojure.java.io :as io]
            [clojurians-log.data :as data]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.response :as response]
            [clojurians-log.views :as views]
            [clojurians-log.slack-messages :as slack-messages]
            [clojurians-log.time-util :as time-util]
            [java-time :as jt]
            [compojure.route :refer [resources]]
            [datomic.api :as d]
            [ring.util.response :refer [response]]
            [bidi.ring :as bidi.r :refer (make-handler)]
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
            thread-messages (queries/channel-thread-messages-of-day db channel date)
            user-ids (slack-messages/extract-user-ids messages)]
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
            response/render)))))

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
               :data/days (queries/channel-days db channel)
               :data/channel-name channel)
        views/channel-page
        response/render)))

(defn- dispatch [endpoint handler-var]
  (fn [request]
    (if (fn? handler-var)
      (handler-var endpoint request)
      ((var-get handler-var) endpoint request))))

(def routes
  ["/" {"healthcheck" (fn [endpoint req]
                        {:headers {"Content-Type" "text/plain"}
                         :body "OK"})

        ;; We're using a var instead of supplying a function directly because
        ;; we don't have to reload this route definition structure everytime a handler function
        ;; gets re-evaluated/replaced.
        "" (-> #'index-route (bidi/tag :index))

        [:channel] (-> #'channel-history-route (bidi/tag :channel-history))
        [:channel "/" :date ".html"] (-> #'log-route (bidi/tag :log-old-url))
        [:channel "/" :date]         (-> #'log-route (bidi/tag :log))
        [:channel "/" :date "/" :ts] (-> #'log-route (bidi/tag :log-page-target-message))}])

(defn home-routes [{:keys [config] :as endpoint}]
  (make-handler routes (partial dispatch endpoint)))

(comment
  (data/load-channel-messages {:request {:params {:channel "clojure" :year "2017" :month "01" :day "01"}}}))
