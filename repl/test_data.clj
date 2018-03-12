(ns test-data
  (:require [datomic.api :as d]
            [clojure.pprint :refer [pprint]]))

(defn pprint-str [val]
  (with-out-str
    (pprint val)))

(defn conn [] (-> reloaded.repl/system :datomic :conn))
(defn db [] (d/db (conn)))

(defn channel-data [db chans]
  (for [chan chans]
    (-> db
        (d/entity [:channel/name chan])
        (select-keys [:channel/slack-id
                      :channel/name
                      :channel/created
                      :channel/creator])
        (update :channel/creator #(do [:user/slack-id (:user/slack-id %)])))))

(defn user-data [db users]
  (for [user users]
    (-> db
        (d/entity [:user/slack-id user])
        (d/touch))))

(defn message-data [db channels days]
  (->> (d/q '[:find [(pull ?msg [:db/id
                                 :message/key
                                 :message/text
                                 {:message/channel [:channel/slack-id]}
                                 {:message/user [:user/slack-id]}
                                 :message/ts
                                 :message/day
                                 #_:message/thread-ts
                                 #_:message/thread-inst]) ...]
              :in $ [?chans ...] [?days ...]
              :where
              [?chan :channel/name ?chans]
              [?msg  :message/day ?days]
              [?msg  :message/channel ?chan]]
            db
            channels
            days)
       (map #(update % :message/user first))
       (map #(update % :message/channel first))))


;; two-channels-two-days
(let [db (db)
      chan-data (channel-data db ["clojure" "clojurescript"])
      msg-data  (message-data db #{"clojure" "clojurescript"} ["2018-02-02" "2018-02-03"])
      user-data (user-data db (concat (map (comp last :message/user) msg-data)
                                      (map (comp last :channel/creator) chan-data)))]
  (spit "resources/clojurians-log/test-data/two-channels-two-days.edn"
        (pprint-str (mapv #(mapv (fn [m]
                                   (dissoc (into {} m) :db/id))
                                 %)
                          [user-data
                           chan-data
                           msg-data]))))



;; quiet-channels
(let [db (db)
      channels #{"jobs" "keechma" "reitit" "yada" "dirac"}
      days #{"2018-02-01" "2018-02-02" "2018-02-03" "2018-02-04"}
      chan-data (channel-data db channels)
      msg-data  (message-data db channels days)
      user-data (user-data db (concat (map (comp last :message/user) msg-data)
                                      (map (comp last :channel/creator) chan-data)))]
  (spit "resources/clojurians-log/test-data/quiet-channels.edn"
        (pprint-str (mapv #(mapv (fn [m]
                                   (dissoc (into {} m) :db/id))
                                 %)
                          [user-data
                           chan-data
                           msg-data]))))
