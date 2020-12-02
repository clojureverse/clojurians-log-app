(ns repl.reactions
  (:require [clojurians-log.application :as app]
            [clojure.java.io :as io]
            [clojurians-log.slack-api :as slack]
            [clojurians-log.db.queries :as q]
            [clojurians-log.db.import :as import]
            [clojurians-log.data :as data]
            [clojure.java.io :as io]
            [clojurians-log.datomic :as d]
            [clojure.tools.reader.edn :as edn]
            [clojure.string :as str]
            [clojure.core.async :as async :refer [>!! <! >! go-loop go <!!]]
            [clojure.data.json :as json])
  (:use [clojurians-log.repl]))

(d/q '[:find (pull ?m [* {:reaction/_message [{:reaction/emoji [:emoji/shortcode :emoji/url]}]}])
        :where
        [?u :user/name "borkdude"]
        [?m :message/day "2018-02-08"]
        [?m :message/channel ?chan]
        [?chan :channel/name "aleph"]]
      (db))

(q/channel-day-messages (db) "aleph" "2018-02-08")

(d/q '[:find (pull ?r [*]) (pull ?m [*])
       :where
       [?u :user/name "borkdude"]
       [?m :message/day "2018-02-08"]
       [?m :message/channel ?chan]
       [?chan :channel/name "aleph"]
       [?r :reaction/message ?m]]
     (db))

(d/q '[:find ?m ?r
       :where
       [?r :reaction/message ?m]]
     (db))

(d/q '[:find (pull ?r [*])
       :where
       [?r :reaction/type]]
     (db))

;; find a message by key
(d/q '[:find (pull ?m [*])
       :where
       [?m :message/key "C0E1SN0NM--1604347443.149900"]]
     (db))


(comment
  ;; add some emojis
  (d/transact (conn) [{:emoji/shortcode "+1" :emoji/url "url1"}])
  (d/transact (conn) [{:emoji/shortcode "joy" :emoji/url "url1"}])
  (d/q '[:find (pull ?e [*]) :where [?e :emoji/shortcode]] (db))
  
  ;; add default emojis
  (def default-emojis
    (with-open [r (io/reader (io/resource "emojis.json"))]
      (let [emoji-list (-> (json/read r :key-fn keyword) :emojis)]
        (map #(hash-map :emoji/shortcode (:name %)) emoji-list))))
  (d/transact (conn) default-emojis)
  (d/q '[:find (pull ?e [*]) :where [?e :emoji/shortcode]] (db))
  
  
  (load-demo-data! "../clojurians-log-demo-data2")
  )

(comment
  (d/q '[:find ?eid :in $ ?eid :where [?eid]] (db) 17592186091857))

(comment
  (d/transact (conn) [{:db/id 17592186091852 :emoji/shortcode "joy" :emoji/url "url1"}])

  (d/transact (conn) [{:db/ident       :emoji/shortcode
                       :db/index true}])

  (d/transact (conn) [{:db/ident       :emoji/shortcode
                       :db/unique      :db.unique/identity}])

  (d/transact (conn) [{:reaction/type "reaction_added"
                       :reaction/emoji [:emoji/shortcode "+1"]
                       :reaction/ts "1001"
                       :reaction/user [:user/name "plexus"]
                       :reaction/message [:message/key "C0G922PCH--1518108773.000057"]}]))


