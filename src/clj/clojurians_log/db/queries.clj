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

(defn reverse-compare
  "Compares in the reverse order to clojure.core/compare.
  Used to reverse result ordering."
  [x y]
  (compare y x))

(defn channel-days [db chan-name]
  (->> (d/q '[:find ?day (count ?msg)
              :in $ ?chan-name
              :where
              [?chan :channel/name ?chan-name]
              [?msg :message/channel ?chan]
              [?msg :message/day ?day]]
            db
            chan-name)
       (sort-by first reverse-compare)))

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

(defn channel-id-map [db]
  (into {}
        (d/q '[:find ?slack-id ?chan
               :where
               [?chan :channel/slack-id ?slack-id]]
             db)))

(doseq [v [#'clojurians-log.db.queries/user-names
           #'clojurians-log.db.queries/channel-thread-messages-of-day
           #'clojurians-log.db.queries/channel
           #'clojurians-log.db.queries/channel-id-map
           #'clojurians-log.db.queries/channel-list
           #'clojurians-log.db.queries/channel-days
           #'clojurians-log.db.queries/channel-day-messages
           #'datomic.api/db]]
  (alter-var-root v (fn [f] (memoize f))))
