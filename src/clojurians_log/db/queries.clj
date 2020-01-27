(ns clojurians-log.db.queries
  (:require [clojurians-log.datomic :as d]
            [clojurians-log.time-util :as time-util]))

(defonce !indexes (atom {}))

(defn channels-dates-msgcounts [db]
  (d/q '[:find ?slack-id ?chan-name ?day (count ?msg)
         :in $
         :where
         [?chan :channel/slack-id ?slack-id]
         [?chan :channel/name ?chan-name]
         [?msg :message/channel ?chan]
         [?msg :message/day ?day]]
       db))

(defn build-indexes [db]
  (let [cdm (channels-dates-msgcounts db)]
    (reduce
     (fn [acc [slack-id chan-name day msgcount]]
       (-> acc
           (assoc-in [:chan-day-cnt slack-id day] msgcount)
           (assoc-in [:day-chan-cnt day slack-id] msgcount)
           (assoc-in [:chan-id->name slack-id] chan-name)
           (assoc-in [:chan-name->id chan-name] slack-id)))
     {}
     cdm)))

(defn build-indexes! [db]
  (reset! !indexes (build-indexes db)))

(defn channel-list
  ([db]
   (->> (map (fn [[id name]]
               #:channel{:slack-id id
                         :name name})
             (:chan-id->name @!indexes))
        (sort-by :channel/name)))
  ([db day]
   (let [{:keys [day-chan-cnt chan-id->name]} @!indexes]
     (->> (for [[ch-id cnt] (get day-chan-cnt day)]
            #:channel{:slack-id ch-id
                      :name (chan-id->name ch-id)
                      :message-count cnt})
          (sort-by :channel/name)))))

(defn- assoc-inst [message]
  (assoc message :message/inst (time-util/ts->inst (:message/ts message))))

(def ^:private pull-message-pattern
  '(pull ?msg
         [:message/text
          :message/ts
          :message/thread-ts
          {:message/user [:user/name
                          :user/slack-id
                          :user-profile/image-48]}]))

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
       (map first)
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
  (when-let [indexes @!indexes]
    (let [{:keys [chan-day-cnt chan-name->id]} @!indexes]
      (some->> chan-name
               chan-name->id
               chan-day-cnt
               (sort-by first reverse-compare)))))

(defn channel [db name]
  (d/q '[:find (pull ?chan [*])
         :in $ ?chan-name
         :where
         [?chan :channel/name ?chan-name]]
       db
       name))

(defn user-names
  [db ids]
  (d/q '[:find ?id ?username
         :in $ [?id ...]
         :where
         [?user :user/slack-id ?id]
         [?user :user/name ?username]]
       db
       ids))

(defn thread-messages
  "Retrieve all child messages for the given parent threads"
  [db parent-tss]
  (->> (d/q {:find [pull-message-pattern]
             :in '[$ [?parent-ts ...]]
             :where '[[?msg  :message/thread-ts ?parent-ts]]
             }
            db
            parent-tss)
       (map first)
       (map assoc-inst)
       (sort-by :message/inst)))

(defn channel-id-map [db]
  (into {}
        (d/q '[:find ?slack-id ?chan
               :where
               [?chan :channel/slack-id ?slack-id]]
             db)))

(defn emoji-url-map [db]
  (into {}
        (d/q '[:find ?shortcode ?url
               :where
               [?emoji :emoji/shortcode ?shortcode]
               [?emoji :emoji/url ?url]]
             db)))

#_
(doseq [v [#'clojurians-log.db.queries/user-names
           #'clojurians-log.db.queries/channel
           #'clojurians-log.db.queries/channel-id-map
           #'clojurians-log.db.queries/channel-list
           #'clojurians-log.db.queries/channel-days
           #'clojurians-log.db.queries/channel-day-messages
           #'clojurians-log.datomic/db]]
  (alter-var-root v (fn [f] (memoize f))))
