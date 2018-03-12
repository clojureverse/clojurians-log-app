(ns clojurians-log.db.queries
  (:require [datomic.api :as d]
            [clojurians-log.time-util :as time-util]))

(defn channel-list
  ([db]
   (->> (d/q '[:find [(pull ?chan [:channel/slack-id :channel/name]) ...]
               :in $
               :where
               [?msg :message/channel ?chan]]
             db)
        (sort-by :channel/name)))
  ([db day]
   (->> (d/q '[:find (pull ?chan [:channel/slack-id :channel/name]) (count ?msg)
               :in $ ?day
               :where
               [?msg :message/day ?day]
               [?msg :message/channel ?chan]]
             db
             day)
        (map #(assoc (first %) :channel/message-count (last %))))))

(defn- assoc-inst [message]
  (assoc message :message/inst (time-util/ts->inst (:message/ts message))))

(defn channel-day-messages [db chan-name day]
  (->> (d/q '[:find [(pull ?msg [:message/text :message/ts {:message/user [:user/name :user-profile/image-48]}]) ...]
              :in $ ?chan-name ?day
              :where
              [?msg :message/day ?day]
              [?msg :message/channel ?chan]
              [?msg :message/user ?user]
              [?chan :channel/name ?chan-name]]
            db
            chan-name
            day)
       (map assoc-inst)
       (sort-by :message/inst)))

(defn channel-days [db chan-name]
  (->> (d/q '[:find ?day (count ?msg)
              :in $ ?chan-name
              :where
              [?chan :channel/name ?chan-name]
              [?msg :message/channel ?chan]
              [?msg :message/day ?day]]
            db
            chan-name)
       (sort-by first)))

(defn channel [db name]
  (d/q '[:find (pull ?chan [*]) .
         :in $ ?chan-name
         :where
         [?chan :channel/name ?chan-name]]
       db
       name))

(defn user-names
  [db names]
  (d/q '[:find ?id ?username
         :in $ [?id ...]
         :where
         [?user :user/slack-id ?id]
         [?user :user/name ?username]]
       db
       names))

(defn channel-names
  [db names]
  (d/q '[:find ?id ?name
         :in $ [?id ...]
         :where
         [?channel :channel/slack-id ?id]
         [?channel :channel/name ?name]]
       db
       names))