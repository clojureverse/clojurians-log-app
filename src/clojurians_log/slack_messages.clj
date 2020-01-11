(ns clojurians-log.slack-messages
  (:require [clojurians-log.message-parser :as mp]
            [clojure.string :as str]
            [hiccup2.core :as hiccup]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

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

(def standard-emoji-map
  "A map from emoji text to emoji.

  `(text->emoji \"smile\") ;; => \"😄\"`"
  (with-open [r (io/reader (io/resource "emojis.json"))]
    (let [emoji-list (-> (json/read r :key-fn keyword)
                         :emojis)]
      (into {} (map (juxt :name :emoji) emoji-list)))))

(defn text->emoji
  ([text]
   (text->emoji text {}))
  ([text emoji-map]
   (let [emoji-map (merge emoji-map standard-emoji-map)]
     (loop [shortcode text]
       (when-let [link (emoji-map shortcode)]
         (cond
           (str/starts-with? link "alias:")
           (recur (str/replace-first link #"alias:" ""))

           (str/starts-with? link "https:")
           [:img {:alt text :src link}]

           ;; return plaintext when we have unicode
           ;; or just nil
           :else link))))))

(defn replace-custom-emojis
  "Replace `:emoji:` with unicode or img tag."
  [message emoji-map]
  (map (fn [[type content :as token]]
         (if (= :emoji type)
           [:emoji (or (text->emoji content emoji-map)
                       (str ":" content ":"))]
           token))
       message))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hiccup

(defn- content-has-child-segments? [content]
  (and (vector? content)
       (vector? (first content))))

(defn- transform-children-or-ident [f content]
  (if (content-has-child-segments? content)
    (map f content)
    content))

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

(defmethod segment->hiccup :channel-id [[type content name]]
  [:i "#" (if-not (empty? name)
            name
            content)])

(defmethod segment->hiccup :emoji [[type content]]
  [:span.emoji content])

(defmethod segment->hiccup :bold [[type content]]
  [:b (transform-children-or-ident segment->hiccup content)])

(defmethod segment->hiccup :italic [[type content]]
  [:i (transform-children-or-ident segment->hiccup content)])

(defmethod segment->hiccup :strike-through [[type content :as segment]]
  [:del (transform-children-or-ident segment->hiccup content)])

(defmethod segment->hiccup :url [[type content]]
  [:a {:href content} content])

(defn message->hiccup
  "Parse slack markup and convert to hiccup."
  ([message usernames]
   (message->hiccup message usernames {}))
  ([message usernames emojis]
   [:p (map segment->hiccup
            (-> message
                (mp/parse2)
                (replace-ids-names usernames)
                (replace-custom-emojis emojis)))]))

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

(defmethod segment->text :channel-id [[type content name]]
  (str "#" (if-not (empty? name)
             name
             content)))

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
           (mp/parse2)
           (replace-ids-names usernames))
       (map segment->text)
       (apply str)))

(comment
  (str/replace-first "alias:picard" #"alias:" "")
  (text->emoji "facepalm"
               {"facepalm" "alias:picard"
                "picard"   "https://picard.png"})
  (text->emoji "thumbsup")
  ({:facepalm "alias:picard"
    :picard   "https://picard.png"}
   :picard))
