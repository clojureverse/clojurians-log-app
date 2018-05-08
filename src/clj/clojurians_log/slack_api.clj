(ns clojurians-log.slack-api
  (:require [clj-slack.users :as slack-users]
            [clj-slack.channels :as slack-channels]
            [datomic.api :as d]
            [clojurians-log.db.import :as import]
            [clojurians-log.application :as cl-app]))

(defn conn []
  {:api-url "https://slack.com/api"
   :token (get-in cl-app/config [:slack :api-token])})

(defn users []
  (:members (slack-users/list (conn))))

(defn channels []
  (:channels (slack-channels/list (conn))))

(defn import-users!
  ([conn]
   (import-users! conn (users)))

  ([conn users]
   (doseq [users (partition-all 1000 users)]
     @(d/transact conn (mapv import/user->tx users)))))

(defn import-channels!
  ([conn]
   (import-channels! conn (channels)))

  ([conn channels]
   @(d/transact conn (mapv import/channel->tx channels))))
