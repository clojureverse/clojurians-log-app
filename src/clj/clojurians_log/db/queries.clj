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

(def ^:private pull-message-pattern
  '[(pull ?msg
          [:message/text
           :message/ts
           :message/thread-ts
           {:message/user [:user/name
                           :user-profile/image-48]}])
    ...])

(defn channel-thread-messages-of-day
  "Retrieve all messages for threads that started in the given channel on the given day"
  [db chan-name day]
  (->> (d/q {:find [pull-message-pattern]
             :in '[$ ?chan-name ?day [?from-date ?to-date]]
             :where '[[?msg  :message/channel ?chan]
                      [?chan :channel/name ?chan-name]
                      [?msg  :message/thread-inst ?thread-inst]
                      [(.after ^java.util.Date ?thread-inst ?from-date)]
                      [(.before ^java.util.Date ?thread-inst ?to-date)]]}
            db
            chan-name
            day
            (time-util/day-str->date-interval day))
       (map assoc-inst)
       (sort-by :message/inst)))

(defn channel-day-messages [db chan-name day]
  (->> (d/q {:find [pull-message-pattern]
             :in '[$ ?chan-name ?day]
             :where '[[?msg :message/day ?day]
                      [?msg :message/channel ?chan]
                      [?msg :message/user ?user]
                      [?chan :channel/name ?chan-name]]}
            db
            chan-name
            day)

       ;; Remove all thread messages except for the thread parent
       ;; Note that thread parents do not have a :thread-ts value themselves
       (remove #(if-let [thread-ts (:message/thread-ts %)]
                  (not= thread-ts (:message/ts %))))
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

(defn message-by-ts [db ts]
  (d/q '[:find (pull ?msg [*]) .
         :in $ ?ts
         :where
         [?msg :message/ts ?ts]]
       db
       ts))

(defn thread-messages
  "Retrieve all child messages for the given parent threads"
  [db parent-tss]
  (->> (d/q {:find [pull-message-pattern]
             :in '[$ [?parent-ts ...]]
             :where '[[?msg  :message/thread-ts ?parent-ts]]
             }
            db
            parent-tss)

       (map assoc-inst)
       (sort-by :message/inst)))

(comment

 (let [channel "datomic"
       day "2015-06-04"
       messages (time-util/time-with-label "channel-day-messages" (channel-day-messages (user/db) channel day))
       thread-msgs1 (time-util/time-with-label "thread-messages" (channel-thread-messages-of-day (user/db) channel day))
       thread-msgs2 (time-util/time-with-label "thread-messages-fast" (thread-messages (user/db)
                                                                                       (map #(:message/ts %) messages)))]
   (println "result1 count:" (count thread-msgs1))
   (println "result2 count:" (count thread-msgs2))
   )

 )
