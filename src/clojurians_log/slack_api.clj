(ns clojurians-log.slack-api
  (:require [clj-slack.channels :as slack-channels]
            [clj-slack.core :refer [slack-request stringify-keys]]
            [clj-slack.emoji :as slack-emoji]
            [clj-slack.users :as slack-users]
            [clojurians-log.application :as cl-app]
            [clojurians-log.datomic :as d]
            [clojurians-log.db.import :as import]
            [clojurians-log.db.queries :as queries]))

(defn slack-conn []
  {:api-url "https://slack.com/api"
   ;; TODO: get rid of this global config access
   :token (get-in cl-app/config [:slack :api-token])})

(defn wrap-rate-limit [f]
  (fn invoke [& args]
    (try
      (apply f args)
      (catch clojure.lang.ExceptionInfo ex
        (let [data (ex-data ex)]
          (if (= 429 (:status data))
            (let [wait-for (Integer/parseInt (get-in data [:headers "retry-after"]))]
              (println "rate-limited. Retry in" wait-for "seconds")
              (Thread/sleep (* (inc wait-for) 1000))
              (apply invoke args))
            (throw ex)))))))

(defn wrap-paginate [k f]
  (fn []
    (let [process-batch (fn process-batch [batch]
                          (let [cursor (get-in batch [:response_metadata :next_cursor])]
                            (concat (get batch k)
                                    (when-not (empty? cursor)
                                      (lazy-seq (process-batch (f {:limit "100000"
                                                                   :cursor cursor})))))))]
      (let [batch (f {:limit "100000"})]
        (process-batch batch)))))

(defn slack-list-users
  "like clj-slack.users/list, but supports extra arguments (for paginations)"
  ([connection]
   (slack-list-users connection {}))
  ([connection optionals]
   (->> optionals
        stringify-keys
        (slack-request connection "users.list"))))

(def users (wrap-paginate
            :members
            (let [users (wrap-rate-limit slack-list-users)]
              (fn [& args]
                (apply users (slack-conn) args)))))

(def channels (wrap-paginate
               :channels
               (let [channels (wrap-rate-limit slack-channels/list)]
                 (fn [& args]
                   (apply channels (slack-conn) args)))))

(defn emoji []
  (:emoji (slack-emoji/list (slack-conn))))

(defn import-users!
  ([conn]
   (import-users! conn (users)))
  ([conn users]
   (doseq [users (partition-all 1000 users)]
     @(d/transact conn (mapv import/user->tx users)))))

(defn import-channels! [conn]
  (let [channel->db-id (queries/channel-id-map (d/db conn))
        channels       (mapv import/channel->tx (channels))]
    @(d/transact conn
                 (mapv (fn [{slack-id :channel/slack-id :as ch}]
                         (if-let [db-id (channel->db-id slack-id)]
                           (assoc ch :db/id db-id)
                           ch))
                       channels))))

(defn import-emojis!
  ([conn]
   (import-emojis! conn (emoji)))
  ([conn emojis]
   (doseq [emojis (partition-all 1000 emojis)]
     @(d/transact conn (mapv import/emoji->tx emojis)))))
