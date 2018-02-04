(ns repl.datomic
  (:require [datomic.api :as d]
            [clojurians-log.data :as data]
            [clojure.string :as str]))

(defn conn [] (-> reloaded.repl/system :datomic :conn))
(defn db [] (d/db (conn)))
;; - message / message event
;; - users
;; - channels

(d/transact (conn) [{:db/ident :message/text
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}
                  {:db/ident :message/timestamp
                   :db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}])

(first (data/event-seq "logs/2017-06-13.txt"))

{"event_ts": "1501621953.010722"
 "ts": "1501621953.010722"
 "subtype": "message_changed"
 "message": {"text": "back in the early days thes wrapper libraries "
             "type": "message"
             "user": "U050ECB92"
             "ts": "1501621946.007124"
             "edited": {"user": "U050ECB92"
                        "ts": "1501621953.000000"}}
 "type": "message"
 "hidden": true
 "channel": "C03S1KBA2"
 "previous_message": {"text": "back in the early redis libraries directly."
                      "type": "message"
                      "user": "U050ECB92"
                      "ts": "1501621946.007124"}}

{"text": "back in the early days there were a million clojure redis wrapper libraries but these days I just use Jedis directly."
 "type": "message"
 "user": "U050ECB92"
 "ts": "1501621946.007124"
 "edited": {"user": "U050ECB92"
            "ts": "1501621953.000000"}}

{:source_team "T03RZGPFR"
 :text "also try optimizations: simple or whitespace to see if it's something else (not minification)"
 :ts "1497301204.478635"
 :user "U06F82LES"
 :team "T03RZGPFR"
 :type "message"
 :channel "C073DKH9P"}






(for [k '(:email :first_name :real_name_normalized :image_48 :image_192 :real_name :image_original :image_72 :image_24 :avatar_hash :title :team :image_32 :last_name :display_name :display_name_normalized)]
  {:db/ident (keyword "user-profile" (str/replace (str/replace (str k) #"_" "-") #":" ""))
   :db/cardinality :db.cardinality/one
   :db/valueType :db.type/boolean})




 (data/user "U06F82LES")
{:id "U06F82LES",
 :name "pesterhazy",
 :real_name "Paulus Esterhazy",
 :is_admin false,
 :is_owner false,
 :profile
 {:email "pesterhazy@gmail.com",
  :first_name "Paulus",
  :real_name_normalized "Paulus Esterhazy",
  :image_48 "https://avatars.slack-edge.com/2015-06-18/6554700387_adc487aa0b891b377f44_48.jpg",
  :image_192 "https://avatars.slack-edge.com/2015-06-18/6554700387_adc487aa0b891b377f44_192.jpg",
  :real_name "Paulus Esterhazy",
  :image_original "https://avatars.slack-edge.com/2015-06-18/6554700387_adc487aa0b891b377f44_original.jpg",
  :image_72 "https://avatars.slack-edge.com/2015-06-18/6554700387_adc487aa0b891b377f44_72.jpg",
  :image_24 "https://avatars.slack-edge.com/2015-06-18/6554700387_adc487aa0b891b377f44_24.jpg",
  :avatar_hash "adc487aa0b89",
  :title "Hammock-driven developer",
  :team "T03RZGPFR",
  :image_32 "https://avatars.slack-edge.com/2015-06-18/6554700387_adc487aa0b891b377f44_32.jpg",
  :last_name "Esterhazy",
  :display_name "pesterhazy",
  :display_name_normalized "pesterhazy"}}


(d/transact (conn) clojurians-log.db.schema/full-schema)

(into {} (d/entity (d/db (conn)) :user/profile))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; 2018-02-03

(d/pull)

(d/q '[:find [(pull ?chan [:channel/slack-id :channel/name]) ...]
       :in $ ?day
       :where
       [?msg :message/day ?day]
       [?msg :message/channel ?chan]]
     (db)
     "2017-11-01")


(d/q '[:find (pull ?msg [:message/text]) (pull ?user [:user/name])
       :in $ ?chan-name ?day
       :where
       [?msg :message/channel ?chan]
       [?msg :message/user ?user]
       [?chan :channel/name ?chan-name]
       [?msg :message/day ?day]]
     (db)
     "beginners"
     "2017-11-10")
