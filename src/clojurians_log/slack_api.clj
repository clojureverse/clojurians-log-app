(ns clojurians-log.slack-api
  (:require [co.gaiwan.slack.api.core :as slack]
            [clj-slack.channels :as slack-channels]
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

(defn emoji [] (slack/get-emoji (slack-conn)))
(defn users [] (slack/get-users (slack-conn)))
(defn channels [] (slack/get-channels (slack-conn)))

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
