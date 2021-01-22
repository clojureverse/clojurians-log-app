(ns clojurians-log.slack-messages
  (:require [clojurians-log.message-parser :as mp]
            [clojure.string :as str]
            [clojure.walk :as walk]
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
  (walk/postwalk
   (fn [token]
     (if (and (vector? token) (= :user-id (first token)))
       (let [user-id (second token)]
         [:user {:user-id user-id :user-name (get id-names user-id user-id)}])
       token))
   message))

(def standard-emoji-map
  "A map from emoji text to emoji.

  `(text->emoji \"smile\") ;; => \"😄\"`"
  (delay
    (with-open [r (io/reader (io/resource "emojis.json"))]
      (let [emoji-list (json/read r :key-fn keyword)]
        (into {}
              (map
               (comp first #(for [alias (:aliases %)] [alias (:emoji %)]))
               emoji-list))))))

(defn text->emoji
  ([text]
   (text->emoji text {}))
  ([text emoji-map]
   (let [emoji-map (merge @standard-emoji-map emoji-map)]
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

(defmulti segment->hiccup
  "Convert a single parsed segment of the form [type content] to hiccup."
  first)

(defn segments->hiccup [segments]
  (cond
    (string? segments)
    segments

    (and (vector? segments) (keyword? (first segments)))
    (segment->hiccup segments)

    (seqable? segments)
    (map segment->hiccup segments)

    :else
    segments))

(defmethod segment->hiccup :default [[type content]]
  content)

(defmethod segment->hiccup :code-block [[type content]]
  [:pre.highlight [:code (hiccup/raw content)]])

(defmethod segment->hiccup :inline-code [[type content]]
  [:code (hiccup/raw content)])

(defmethod segment->hiccup :user [[type content]]
  [:span.username [:a {:href (str "/_/_/users/" (:user-id content))}
                   "@" (:user-name content)]])

(defmethod segment->hiccup :channel-id [[type content name]]
  [:i "#" (if-not (empty? name)
            name
            content)])

(defmethod segment->hiccup :emoji [[type content]]
  [:span.emoji content])

(defmethod segment->hiccup :bold [[type content]]
  [:b (segments->hiccup content)])

(defmethod segment->hiccup :italic [[type content]]
  [:i (segments->hiccup content)])

(defmethod segment->hiccup :strike-through [[type content :as segment]]
  [:del (segments->hiccup content)])

(defmethod segment->hiccup :url [[type content]]
  [:a {:href content} content])

(defn message->hiccup
  "Parse slack markup and convert to hiccup."
  ([message usernames]
   (message->hiccup message usernames {}))
  ([message usernames emojis]
   [:p (segments->hiccup
        (-> message
            (mp/parse2)
            (replace-ids-names usernames)
            (replace-custom-emojis emojis)))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Plain text

(defmulti segment->text
  "Convert a single parsed segment of the form [type content] to plain text."
  first)

(defn segments->text [segments]
  (cond
    (string? segments)
    segments

    (and (vector? segments) (keyword? (first segments)))
    (segment->text segments)

    (seqable? segments)
    (apply str (map segment->text segments))

    :else
    segments))

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
  (str "*" (segments->text content) "*"))

(defmethod segment->text :italic [[type content]]
  (str "_" (segments->text content) "_"))

(defmethod segment->text :strike-through [[type content]]
  (str "~" (segments->text content) "~"))

(defmethod segment->text :url [[type content]]
  content)

(defn message->text
  "Convert Slack markup to plain text."
  [message usernames]
  (-> message
      mp/parse2
      (replace-ids-names usernames)
      segments->text))

(comment
  (str/replace-first "alias:picard" #"alias:" "")
  (text->emoji "facepalm"
               {"facepalm" "alias:picard"
                "picard"   "https://picard.png"})
  (text->emoji "thumbsup")
  ({:facepalm "alias:picard"
    :picard   "https://picard.png"}
   :picard)

  (message->text "*Hey everyone, we\u2019re so excited to be here for DevOps Enterprise Summit talking about*\n:arrow_right: _*Be sure to visit our booth <https://doesvirtual.com/teamform>*_ \n:tv: _*Or join us anytime on Zoom -\u00a0<https://bit.ly/3iIdX1X>*_\n:mega: _*Schedule a private demo - <https://teamform.co/demo>*_\n:gift: _*Register for giveaway (1x PS5 or XBox Series X, 1 x 50min chat with the authors of Team Topologies, 20x IT Rev Books) <https://www.teamform.co/does-giveaway>*_\n\n*We\u2019ve got a exciting week with a bunch of demos of TeamForm scheduled*\n:star: 11-11:15am PDT: TeamForm Live Demo: Managing Supply &amp; Demand at Scale - join @ <https://us02web.zoom.us/j/81956904920>\n:star: 12:45-1:00pm PDT: TeamForm Live Demo: Measuring Team Organising Principles - join @ <https://us02web.zoom.us/j/81956904920>\n:bar_chart: 3:45-4pm PDT: TeamForm Live Demo: Measuring Team Proficiency - join @ <https://us02web.zoom.us/j/81956904920>\n\nLater this week:\n:arrow_right: Register for our AMA with Authors of TeamTopologies <https://sched.co/ej42> with <@ULTTZCP7S> &amp; <@UBE001UAX>" {})
  )
