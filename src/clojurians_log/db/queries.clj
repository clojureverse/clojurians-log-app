(ns clojurians-log.db.queries
  (:require [clojurians-log.datomic :as d]
            [clojurians-log.time-util :as time-util]))

(defonce !indexes (atom nil))

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
     nil
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
          {:reaction/_message [:reaction/user
                               :reaction/type
                               {:reaction/emoji [*]}]}
          :message/thread-broadcast?
          {:message/user [:user/name
                          :user/slack-id
                          :user-profile/real-name
                          :user-profile/display-name
                          :user-profile/image-48]}]))

(defn channel-day-messages [db chan-name day]
  (->> (d/q {:find [pull-message-pattern]
             :in '[$ ?chan-name ?day]
             :where '[[?msg :message/day ?day]
                      [?msg :message/channel ?chan]
                      [?chan :channel/name ?chan-name]]}
            db
            chan-name
            day)
       (map first)
       ;; Remove all thread messages except for the thread parent
       ;; and except for brodcast messages.
       ;; Note that thread parents do not have a :thread-ts value themselves
       (remove #(and (:message/thread-ts %) (not (:message/thread-broadcast? %))))
       (map #(if(:message/thread-broadcast? %) 
              (assoc % :message/top-level? true)
               %))
       (map assoc-inst)
       (sort-by :message/inst)))

;; (channel-day-messages (user/db) "cljs-dev" "2018-02-05")

(defn reverse-compare
  "Compares in the reverse order to clojure.core/compare.
  Used to reverse result ordering."
  [x y]
  (compare y x))

(defn channel-days [db chan-name]
  (when-let [indexes @!indexes]
    (let [{:keys [chan-day-cnt chan-name->id] :as index} @!indexes]
      (when index
        (some->> chan-name
                 chan-name->id
                 chan-day-cnt
                 (sort-by first reverse-compare))))))

(defn channel [db name]
  (d/q '[:find (pull ?chan [*])
         :in $ ?chan-name
         :where
         [?chan :channel/name ?chan-name]]
       db
       name))

(defn get-channel-id-by-name [channel-name]
  (get (:chan-name->id @!indexes) channel-name))

(defn user-names
  [db ids]
  (d/q '[:find ?id ?username
         :in $ [?id ...]
         :where
         [?user :user/slack-id ?id]
         [?user :user/name ?username]]
       db
       ids))

(defn user-profile
  "returing all the data about the user acording to the user id slack number"
  [db id]
  (ffirst (d/q '[:find (pull ?user [*])
                 :in $ ?id
                 :where [?user :user/slack-id ?id]]
               db
               id)))

(defn thread-messages
  "Retrieve all child messages for the given parent threads"
  [db parent-tss]
  (->> (d/q {:find [pull-message-pattern]
             :in '[$ [?parent-ts ...]]
             :where '[[?msg :message/thread-ts ?parent-ts]]}
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

(defn unique-users-between-days [db from-day to-day]
  (d/q '[:find (count ?user)
         :in $ ?from-day ?to-day
         :where
         [?msg :message/user ?user]
         [?msg :message/day ?day]
         [(>= ?day ?from-day)]
         [(<= ?day ?to-day)]]
       db
       from-day
       to-day))

(defn channel-message-stats-between-days [from-day to-day channel-name]
  (let [chan-day-cnt (:chan-day-cnt @!indexes)
        chan-day-data (get chan-day-cnt (get-channel-id-by-name channel-name) {})
        range-of-days (time-util/range-of-days from-day to-day)]
    (mapv #(hash-map :day % :msg-count (get chan-day-data % 0)) range-of-days)))

(defn message-stats-between-days [from-day to-day]
  (letfn [(day-chan-cnt [] (:day-chan-cnt @!indexes))
          (day-total [day] (apply + (vals (get (day-chan-cnt) day))))
          (days-total [days] (transduce (map day-total) + 0 days))]
    (mapv #(hash-map :day % :msg-count (day-total %)) (time-util/range-of-days from-day to-day))))

#_
(doseq [v [#'clojurians-log.db.queries/user-names
           #'clojurians-log.db.queries/channel
           #'clojurians-log.db.queries/channel-id-map
           #'clojurians-log.db.queries/channel-list
           #'clojurians-log.db.queries/channel-days
           #'clojurians-log.db.queries/channel-day-messages
           #'clojurians-log.datomic/db]]
  (alter-var-root v (fn [f] (memoize f))))


(comment
  ;; how to find channel name in the user/db
  (defn t [db]
    (d/q '[:find (pull ?chan [*])
           :where
           [?chan :channel/name "cljs-dev"]]
         db))
  (t (user/db)))
