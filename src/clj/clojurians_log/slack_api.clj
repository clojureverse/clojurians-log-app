(ns clojurians-log.slack-api
  (:require [clj-slack.users :as slack-users]
            [clj-slack.channels :as slack-channels]
            [datomic.api :as d]
            [clojurians-log.db.import :as import]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.application :as cl-app]))

(defn conn []
  {:api-url "https://slack.com/api"
   :token (get-in cl-app/config [:slack :api-token])})

(defn users []
  (:members (slack-users/list (conn))))

(defn channels []
  (:channels (slack-channels/list (conn))))

(defn import-users! [conn]
  (doseq [users (partition-all 1000 (users))]
    @(d/transact conn (mapv import/user->tx users))))

(defn import-channels! [conn]
  (let [channel->db-id (queries/channel-id-map (d/db conn))
        channels       (mapv import/channel->tx (channels))]
    @(d/transact conn (map (fn [{slack-id :channel/slack-id :as ch}]
                             (if-let [db-id (channel->db-id slack-id)]
                               (assoc ch :db/id db-id)
                               ch))
                           channels))))
