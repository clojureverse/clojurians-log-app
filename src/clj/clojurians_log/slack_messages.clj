(ns clojurians-log.slack-messages
  (:require [clojurians-log.message-parser :as mp]
            [clojure.string :as str]
            [hiccup2.core :as hiccup]))

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
  "replaces user/slack-id with user/name.
  message is a vector of vectors format returned by mp/parse"
  [message id-names]
  (map (fn [[type content :as token]]
         (if (= :user-id type)
           [:user {:user-id content :user-name (get id-names content)}]
           token))
       message))

(defmulti render-segment first)

(defmethod render-segment :default [[type content]]
  content)

(defmethod render-segment :code-block [[type content]]
  [:pre.highlight [:code (hiccup/raw content)]])

(defmethod render-segment :inline-code [[type content]]
  [:code (hiccup/raw content)])

(defmethod render-segment :user [[type content]]
  [:span.username "@" (:user-name content)])

(defmethod render-segment :channel-id [[type content]]
  [:i content])

(defmethod render-segment :emoji [[type content]]
  [:span.emoji ":" content ":"])

(defmethod render-segment :bold [[type content]]
  [:b content])

(defmethod render-segment :italic [[type content]]
  [:i content])

(defn render-hiccup
  "parse slack markup and convert to hiccup"
  [message usernames]
  [:p (map render-segment
           (-> message
               (mp/parse)
               (replace-ids-names usernames)))])
