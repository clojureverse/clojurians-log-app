(ns clojurians-log.slack-messages
  (:require [clojure.string :as str]))

(defn- extract-texts
  [messages]
  (let [texts (map :message/text messages)]
    (str/join " " texts)))

(defn- parse-users
  [texts]
  (re-seq #"(?<=<@).*?(?=>)" texts))

(defn extract-user-ids
  "returns all the user/slack-ids from a string"
  [messages]
  (into #{} (comp
              (map :message/text)
              (map parse-users)
              cat)
        messages))

(defn replace-ids-names
  "replaces user/slack-id with user/name"
  [message id-names]
  (str/replace message #"<@(.*?)>"
               (fn [[whole-match partial-match]]
                 (str "@" (get id-names partial-match)))))
