(ns co.gaiwan.slack.api.core
  (:require [lambdaisland.glogc :as log]
            [clojurians-log.application :as cl-app]
            [co.gaiwan.slack.api.middleware :as mw]
            [co.gaiwan.slack.api.web :as web]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack API functions

(defn conn
  ([]
   (conn (System/getenv "SLACK_TOKEN")))
  ([slack-token]
   {:api-url "https://slack.com/api" :token slack-token}))

(defn collection-endpoint
  [key endpoint]
  (mw/wrap-paginate
   #(log/info :slack-api/pagination-error-resp %)
   key
   (mw/wrap-rate-limit
    (fn self
      ([connection]
       (self connection {}))
      ([connection opt]
       (web/slack-request connection endpoint opt))))))

(def get-emoji (collection-endpoint :emoji "emoji.list"))
(def get-users (collection-endpoint :members "users.list"))
(def get-channels (collection-endpoint :channels "conversations.list"))
(def get-history (collection-endpoint :messages "conversations.history"))
(def get-pins (collection-endpoint :items "pins.list"))

(defn error?
  "Is the response an error?
  This checks for a couple of different cases that might arise. Generally it's
  advisable to always check if a response is an error before trying to use its
  results.
  Slack returns an :ok true/false key in every response. For paginated
  collection responses we normally unwrap the outer map, unless (= :ok false),
  so this should also work on collection responses.
  For rare exceptions where Slack returns a non-200 response with an empty body
  we simply return `:error`."
  [response]
  (or (= :error response)
      (and (map? response) (false? (:ok response)))))
