(ns clojurians-log.slack-api
  (:require [clj-slack.users :as slack-users]
            [clj-slack.channels :as slack-channels]
            [clj-slack.emoji :as slack-emoji]
            [clojurians-log.datomic :as d]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.db.import :as import]
            [clojurians-log.application :as cl-app]))

(defn slack-conn []
  {:api-url "https://slack.com/api"
   :token (get-in cl-app/config [:slack :api-token])})

(defn users []
  (:members (slack-users/list (slack-conn))))

(defn channels []
  (:channels (slack-channels/list (slack-conn))))

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
