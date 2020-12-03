(ns clojurians-log.routes
  (:require [clojure.java.io :as io]
            [clojurians-log.data :as data]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.response :as response]
            [clojurians-log.views :as views]
            [clojurians-log.slack-messages :as slack-messages]
            [clojurians-log.time-util :as time-util]
            [java-time :as jt]
            [clojurians-log.datomic :as d]
            [markdown-to-hiccup.core :as m]
            [ring.util.response :refer [response]]
            [reitit.core :as reitit]
            [reitit.ring]
            [clojure.string :as str]))

(defn make-context [request]
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
    (map (fn [msg]
           (if-let [children (get messages-by-thread-ts (:message/ts msg))]
             (assoc msg :message/children children)
             msg))
         messages)))

(defn healthcheck-route [_]
  {:headers {"Content-Type" "text/plain"}
   :status 200
   :body "OK"})

(def emoji-map (atom nil))

(defn emoji-url-map [db]
  (or @emoji-map
      (reset! emoji-map (queries/emoji-url-map db))))

(defn add-cache-control-header [context]
  (let [config  @(get-in context [:request :endpoint :config :value])
        max-age (get-in config [:message-page :cache-time] 0)]
    (assoc-in context [:response/headers "Cache-Control"] (str "public, max-age: " max-age))))

(defn log-route [{:keys [endpoint] :as request}]
  (let [config                    @(get-in endpoint [:config :value])
        conn                      (get-in endpoint [:datomic :conn])
        {:keys [channel date ts]} (:path-params request)
        cache-time                (get-in config [:message-page :cache-time] 0)
        date                      (if (str/ends-with? date ".html")
                                    (str/replace date ".html" "")
                                    date)]

    ;; Since we're displaying a log, presumably, all of the content is permanently cachable
    ;; Message page content also most likely have not changed and does not require any processing/page-generation.
    ;; In those cases, skip page generate entirely, instead of generating the contents, then have the ring
    ;; middleware respond with a 304.
    (if (and (not (zero? cache-time))
             (not (message-page-might-have-updated? date (get-in request [:headers "if-modified-since"]))))
      (-> (ring.util.response/response nil)
          (ring.util.response/status 304))

      (let [db              (d/db conn)
            messages        (queries/channel-day-messages db channel date)
            thread-messages (queries/thread-messages db (map #(:message/ts %) messages))
            user-ids        (slack-messages/extract-user-ids messages)]

        (if (empty? messages)
          (-> (ring.util.response/response "Oops! No messages here!")
              (ring.util.response/content-type "text/html; charset=utf-8")
              (ring.util.response/status 404))

          (-> (make-context request)
              (assoc :data/channel (ffirst (queries/channel db channel))
                     :data/channels (queries/channel-list db date)
                     :data/messages (merge-thread-messages messages thread-messages)
                     :data/target-message (some #(when (= (:message/ts %) ts) %) (apply conj messages thread-messages))
                     :data/usernames (into {} (queries/user-names db user-ids))
                     :data/emojis (emoji-url-map db)
                     :data/channel-days (queries/channel-days db channel)
                     :data/title (str channel " " date " | " (get-in request [:config :application :title]))
                     :data/date date
                     :data/http-origin (get-in config [:http :origin]))
              views/log-page
              add-cache-control-header
              (assoc-in [:response/headers "Last-Modified"] (time-util/time->html-ts (jt/zoned-date-time time-util/UTC)))
              response/render))))))

(defn- db-from-endpoint [endpoint]
  (->> (get-in endpoint [:datomic :conn])
       (d/db)))

(defn index-route [{:keys [endpoint] :as request}]
  (let [db (db-from-endpoint endpoint)]
    (-> (make-context request)
        (assoc :data/title (get-in request [:config :application :title])
               :data/channels (queries/channel-list db))
        views/channel-list-page
        add-cache-control-header
        response/render)))

(defn channel-history-route [{:keys [endpoint] :as request}]
  (let [db (db-from-endpoint endpoint)
        {:keys [channel]} (:path-params request)]
    (-> (make-context request)
        (assoc :data/title (str (get-in request [:config :application :title]) "| " channel)
               :data/channel-days (queries/channel-days db channel)
               :data/channel-name channel)
        views/channel-page
        add-cache-control-header
        response/render)))

(defn about-route [request]
  (-> request
      make-context
      (assoc :data/about-hiccup
             (-> (get-in request [:config :application :about])
                 slurp
                 (m/md->hiccup {:encode? true})
                 (m/component)))
      views/about
      response/render))

(defn user-profile-route  [{:keys [endpoint] :as request}]
    ;; (def request request)  
    (let [db (db-from-endpoint endpoint)]
      (-> request
          make-context
          (assoc :data/username 
                 (queries/user-profile db (get-in request [:path-params :user-id])))
          ;; (println :data/username)        
          views/user-profile-route
          response/render)))

(defn sitemap-route [{:keys [endpoint] :as request}]
  (let [config @(get-in endpoint [:config :value])
        db (db-from-endpoint endpoint)]
    (-> request
        make-context
        (assoc :data/http-origin (get-in config [:http :origin])
               :data/channel-day-tuples
               (for [{:channel/keys [name] :as channel} (queries/channel-list db)]
                 [channel (queries/channel-days db name)]))
        views/sitemap
        response/xml-render)))

(defn message-stats-route [{:keys [endpoint] :as request}]
  (let [config @(get-in endpoint [:config :value])
        {:keys [from-date to-date]} (:path-params request)]
    (-> request
        make-context
        (assoc :data/message-stats (queries/message-stats-between-days from-date to-date))
        views/message-stats-page
        response/render)))

(def routes
  [["/" {:name :clojurians-log.routes/index
         :get index-route}]
   ["/x/x/x/about" {:name :clojurians-log.routes/about
                    :get about-route}]
   ["/x/x/x/sitemap" {:name :clojurians-log.routes/sitemap
                      :get sitemap-route}]
   ["/x/x/x/healthcheck" {:name :clojurians-log.routes/healthcheck
                          :get healthcheck-route}]
   ["/{channel}" {:name :clojurians-log.routes/channel
                  :get channel-history-route}]
   ["/{channel}/{date}" {:name :clojurians-log.routes/channel-date
                         :get log-route}]
   ["/{channel}/{date}/{ts}" {:name :clojurians-log.routes/message,
                              :get log-route}]
   ["/_/stats/{from-date}/{to-date}" {:name :clojurians-log.routes/message-stats :get message-stats-route}]])
