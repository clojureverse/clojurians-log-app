(ns repl.emoji-cleanup
  (:require [clojurians-log.datomic :as d]
            [clojurians-log.repl :as repl]
            [clojurians-log.slack-api :as slack]))

;; Our :emoji/shortcode property did not originally have an index, meaning we
;; kept creating more entities for emojis, instead of upserting them.
;;
;; To fix this, first retract all shortcode attributes, then transact the schema
;; so it gets the index, then re-import

;; Do the following before deploying the new schema changes:

(def shortcodes (d/q '[:find ?i ?s :where [?i :emoji/shortcode ?s]] (repl/db)))
(def tx-data (for [[i s] shortcodes]
               [:db/retract i :emoji/shortcode s]))

(run! (partial d/transact (repl/conn)) (partition-all 1000 tx-data))

@(d/transact (repl/conn) [{:db/ident       :emoji/shortcode
                           :db/valueType   :db.type/string
                           :db/cardinality :db.cardinality/one
                           :db/unique      :db.unique/identity}])

(slack/import-emojis! (repl/conn))

;; After deploying the new version, re-import the data to get the emoji reactions in there

(def result (load-files! (log-files))) ;; [(volatile count) (promise)]

(future
  (while (not (realized? (second result)))
    (println @(first result))
    (Thread/sleep 2000))
  (println :done))

(group-by second
          (map (juxt :reaction/type
                     (comp :emoji/shortcode :reaction/emoji))
               (:reaction/_message
                (datomic.api/entity (repl/db)
                                    [:message/key "C0GLTDB2T--1608789973.125700"]))))
