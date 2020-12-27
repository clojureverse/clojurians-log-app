(ns repl.emoji-cleanup
  (:require [clojurians-log.datomic :as d]
            [clojurians-log.application :as app]
            [clojurians-log.repl :as repl]
            [clojurians-log.slack-api :as slack]))

(defn conn
  "Reach into the system for the datomic connection.

  Not 100% kosher but remember this is not production code as such."
  []
  (get-in (app/system) [:datomic :conn]))

(defn db []
  (d/db (conn)))

;; Our :emoji/shortcode property did not originally have an index, meaning we
;; kept creating more entities for emojis, instead of upserting them.
;;
;; To fix this, first retract all shortcode attributes, then transact the schema
;; so it gets the index, then re-import

;; Do the following before deploying the new schema changes:

(def shortcodes (d/q '[:find ?i ?s :where [?i :emoji/shortcode ?s]] (db)))
(def tx-data (for [[i s] shortcodes]
               [:db/retract i :emoji/shortcode s]))

(run! (partial d/transact (conn)) (partition-all 1000 tx-data))

@(d/transact (conn) [{:db/ident       :emoji/shortcode
                      :db/valueType   :db.type/string
                      :db/cardinality :db.cardinality/one
                      :db/unique      :db.unique/identity}])

(slack/import-emojis! (conn))

;; After deploying the new version, re-import the data to get the emoji reactions in there

(def result (load-files! (log-files))) ;; [(volatile count) (promise)]

(future
  (while (not (realized? (second result)))
    (println @(first result))
    (Thread/sleep 2000))
  (println :done))

;; Check vincent's message, should have +4 -1 hearts (= 3 hearts)

(group-by second
          (map (juxt :reaction/type
                     (comp :emoji/shortcode :reaction/emoji))
               (:reaction/_message
                (datomic.api/entity (db)
                                    [:message/key "C0GLTDB2T--1608789973.125700"]))))


;; Re-do reactions but now including the key


(def reactions (d/q '[:find ?r ?m :where [?r :reaction/message ?m]] (db)))
(def tx-data (for [[r m] reactions]
               [:db/retract r :reaction/message m]))

(run! (partial d/transact (conn)) (partition-all 1000 tx-data))

(count (d/q '[:find [?r ?m]  :where [?r :reaction/message ?m]] (db)))
