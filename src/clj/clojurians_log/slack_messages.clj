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
  (re-seq #"(?<=<@)[^\|>]+" texts))

(defn extract-user-ids
  "Given a seq of slack messages, return user ids mentioned."
  [messages]
  (into #{} (comp
              (map :message/text)
              (map parse-users)
              cat)
        messages))

(defn replace-ids-names
  "Replaces user/slack-id with user/name.

  Message is a vector of vectors format returned by mp/parse."
  [message id-names]
  (map (fn [[type content :as token]]
         (if (= :user-id type)
           [:user {:user-id content :user-name (get id-names content)}]
           token))
       message))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hiccup

(defmulti segment->hiccup
  "Convert a single parsed segment of the form [type content] to hiccup."
  first)

(defmethod segment->hiccup :default [[type content]]
  content)

(defmethod segment->hiccup :code-block [[type content]]
  [:pre.highlight [:code (hiccup/raw content)]])

(defmethod segment->hiccup :inline-code [[type content]]
  [:code (hiccup/raw content)])

(defmethod segment->hiccup :user [[type content]]
  [:span.username "@" (:user-name content)])

(defmethod segment->hiccup :channel-id [[type content]]
  [:i "#" content])

(defmethod segment->hiccup :emoji [[type content]]
  [:span.emoji ":" content ":"])

(defmethod segment->hiccup :bold [[type content]]
  [:b content])

(defmethod segment->hiccup :italic [[type content]]
  [:i content])

(defn message->hiccup
  "Parse slack markup and convert to hiccup."
  [message usernames]
  [:p (map segment->hiccup
           (-> message
               (mp/parse)
               (replace-ids-names usernames)))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plain text

(defmulti segment->text
  "Convert a single parsed segment of the form [type content] to plain text."
  first)

(defmethod segment->text :default [[type content]]
  content)

(defmethod segment->text :code-block [[type content]]
  (str "\n" content "\n"))

(defmethod segment->text :inline-code [[type content]]
  (str "`" content "`"))

(defmethod segment->text :user [[type content]]
  (str "@" (:user-name content)))

(defmethod segment->text :channel-id [[type content]]
  (str "#" content))

(defmethod segment->text :emoji [[type content]]
  (str ":" content ":"))

(defmethod segment->text :bold [[type content]]
  (str "*" content "*"))

(defmethod segment->text :italic [[type content]]
  (str "_" content "_"))

(defn message->text
  "Convert Slack markup to plain text."
  [message usernames]
  (->> (-> message
           (mp/parse)
           (replace-ids-names usernames))
       (map segment->text)
       (apply str)))
