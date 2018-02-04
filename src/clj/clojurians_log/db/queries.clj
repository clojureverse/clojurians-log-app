(ns clojurians-log.db.queries
  (:require [datomic.api :as d]))

(defn channel-list
  ([db]
   (d/q '[:find [(pull ?chan [:channel/slack-id :channel/name]) ...]
          :in $
          :where
          [?msg :message/channel ?chan]]
        db))
  ([db day]
   (d/q '[:find [(pull ?chan [:channel/slack-id :channel/name]) ...]
          :in $ ?day
          :where
          [?msg :message/day ?day]
          [?msg :message/channel ?chan]]
        db
        day)))

(defn channel-day-messages [db chan-name day]
  (d/q '[:find (pull ?msg [:message/text]) (pull ?user [:user/name])
         :in $ ?chan-name ?day
         :where
         [?msg :message/channel ?chan]
         [?msg :message/user ?user]
         [?chan :channel/name ?chan-name]
         [?msg :message/day ?day]]
       (db)
       chan-name
       day))
