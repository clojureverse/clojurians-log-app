(ns repl.slack)

(user/add-dependency '[org.julienxx/clj-slack "0.5.5"])

  (def slack-token "...")

(require 'clj-slack.users
         'clj-slack.channels)

(def connection {:api-url "https://slack.com/api" :token slack-token})


(def users (clj-slack.users/list connection))
(def channels (clj-slack.channels/list connection))

(keys users)
(:ok :members :cache_ts)

(count (:members users))
12414

(take 3 (:members users))

(count (:channels channels))
538

(clojurians-log.db.import/user->tx
 (first (:members users)))
