(ns clojurians-log.slack-messages
  (:require [clojurians-log.message-parser :as mp]
            [clojure.string :as str]
            [hiccup2.core :as hiccup]
            [clojure.pprint :as pp]))

(defn parse-channels
  [text]
  (re-seq #"(?<=<#).*?(?=>)" text))

(defn normalize-channel-names
  ([key] {key false})
  ([key value] {key value}))

(defn extract-channels-messages
  [messages]
  (into {} (comp
              (map :message/text)
              (map parse-channels)
              cat
              (map #(str/split % #"\|"))
              (map #(apply normalize-channel-names %)))
        messages))

(defn make-channel-map
  [acc {:keys [channel/slack-id channel/name]}]
  (assoc acc slack-id name))

(defn missing-channels
  [channels]
  (reduce-kv (fn [acc key val]
               (if (false? val)
                 (conj acc key)
                 acc))
             #{} channels))

(defn known-channels
  [messages channels]
  (merge (extract-channels-messages messages)
         (reduce make-channel-map {} channels)))

(defn replace-name
  [content id-names]
  (if (:name content)
    content
    (assoc content :name ((:id content) id-names))))

(defn replace-channel-ids-names
  "Replaces user/slack-id with user/name.

  Message is a vector of vectors format returned by mp/parse."
  [message id-names]
  (pp/pprint message)
  (map (fn [[type & content :as token]]
         (if (= :channel type)
           [:channel (replace-name (into {} content) id-names)]
           token))
       message))

(defn- parse-users
  [text]
  (re-seq #"(?<=<@).*?(?=>)" text))


(defn extract-user-ids
  "Given a seq of slack messages, return user ids mentioned."
  [messages]
  (into #{} (comp
              (map :message/text)
              (map parse-users)
              cat)
        messages))

(defn replace-user-ids-names
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

(defmethod segment->hiccup :channel [[type content]]
  [:i "#" (:name content)])

(defmethod segment->hiccup :emoji [[type content]]
  [:span.emoji ":" content ":"])

(defmethod segment->hiccup :bold [[type content]]
  [:b content])

(defmethod segment->hiccup :italic [[type content]]
  [:i content])

(defn message->hiccup
  "Parse slack markup and convert to hiccup."
  [message usernames channel-names]
  [:p (map segment->hiccup
           (-> message
               (mp/parse)
               (replace-user-ids-names usernames)
               (replace-channel-ids-names channel-names)))])

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

(defmethod segment->text :channel [[type content]]
  (str "#" (:name content)))

(defmethod segment->text :emoji [[type content]]
  (str ":" content ":"))

(defmethod segment->text :bold [[type content]]
  (str "*" content "*"))

(defmethod segment->text :italic [[type content]]
  (str "_" content "_"))

(defn message->text
  "Convert Slack markup to plain text."
  [message usernames channel-names]
  (->> (-> message
           (mp/parse)
           (replace-user-ids-names usernames)
           (replace-channel-ids-names channel-names))
       (map segment->text)
       (apply str)))


(def test-response
  {:data/messages
     '( {:message/text "is there a debugger solution for cljsrn?",
         :message/ts "1518186225.000322",
         :message/user {:user/name "doglooksgood",}
                       :user-profile/image-48 "https://avatars.slack-edge.com/2016-02-22/22424886018_aa7335a1b4b9f5fc30dc_48.png"
         :message/inst "2018-02-09T14:23:45.000322Z"}
        {:message/text "<@U0NBGRGD6> normal chrome debug <#C03S1L9DN|clojurescript> tools",
         :message/ts "1518187663.000506",
         :message/user {:user/name "carocad",
                        :user-profile/image-48 "https://secure.gravatar.com/avatar/99e8b59c0f6a015418e24c2c15cad283.jpg?s=48&d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0025-48.png"},
         :message/inst "2018-02-09T14:47:43.000506Z"}
        {:message/text
                     "but <#C073DKH9P> dirac will not work for cljsrn? or in the recent version it works?",
         :message/ts "1518190477.000419",
         :message/user {:user/name "doglooksgood",
                        :user-profile/image-48
                                   "https://avatars.slack-edge.com/2016-02-22/22424886018_aa7335a1b4b9f5fc30dc_48.png"},
         :message/inst "2018-02-09T15:34:37.000419Z"}
        {:message/text
                     "no that i know. <#C0620C0C8|reagent> But (afaik) <#C0E1SN0NM|cljsrn> not even react devtools can send commands back to the device so your best alternative is using source maps and devtools to pinpoint an error and rely on the repl for evaluation",
         :message/ts "1518191810.000638",
         :message/user {:user/name "carocad",
                        :user-profile/image-48
                                   "https://secure.gravatar.com/avatar/99e8b59c0f6a015418e24c2c15cad283.jpg?s=48&d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0025-48.png"},
         :message/inst "2018-02-09T15:56:50.000638Z"})
   :data/channels
   '(
      {:channel/slack-id "C03S1L9DN",
       :channel/name "clojurescript",
       :channel/message-count 14}
      {:channel/slack-id "C0E1SN0NM",
       :channel/name "cljsrn",
       :channel/message-count 4}
      {:channel/slack-id "C0620C0C8",
       :channel/name "reagent",
       :channel/message-count 51}
      {:channel/slack-id "C03S1KBA2",
       :channel/name "clojure",
       :channel/message-count 83}
      {:channel/slack-id "C06B40HMY",
       :channel/name "remote-jobs",
       :channel/message-count 12}
      {:channel/slack-id "C05006WDW",
       :channel/name "jobs",
       :channel/message-count 2}
      {:channel/slack-id "C0A5GSC6T",
       :channel/name "ring",
       :channel/message-count 2}
      {:channel/slack-id "C0CB40N8K",
       :channel/name "community-development",
       :channel/message-count 1}
      {:channel/slack-id "C0B22RS2Y",
       :channel/name "lein-figwheel",
       :channel/message-count 1}
      {:channel/slack-id "C0AB48493",
       :channel/name "leiningen",
       :channel/message-count 2}
      {:channel/slack-id "C6N245JGG",
       :channel/name "shadow-cljs",
       :channel/message-count 204}
      {:channel/slack-id "C06E3HYPR",
       :channel/name "clojure-dev",
       :channel/message-count 49}
      {:channel/slack-id "C09C8GRLY",
       :channel/name "spacemacs",
       :channel/message-count 32}
      {:channel/slack-id "C0744GXCJ",
       :channel/name "cursive",
       :channel/message-count 45}
      {:channel/slack-id "C322LFP1A",
       :channel/name "lumo",
       :channel/message-count 8}
      {:channel/slack-id "C06B7L97Y",
       :channel/name "clojure-greece",
       :channel/message-count 40}
      {:channel/slack-id "C1Q164V29",
       :channel/name "sql",
       :channel/message-count 24}
      {:channel/slack-id "C0JKW8K62",
       :channel/name "test-check",
       :channel/message-count 6}
      {:channel/slack-id "C03RZMDSH",
       :channel/name "datomic",
       :channel/message-count 35}
      {:channel/slack-id "C055HRXPM",
       :channel/name "clojure-italy",
       :channel/message-count 8}
      {:channel/slack-id "C0H7M5HFE",
       :channel/name "mount",
       :channel/message-count 5}
      {:channel/slack-id "C4C63FWP5",
       :channel/name "unrepl",
       :channel/message-count 71}
      {:channel/slack-id "C03RZGPG3",
       :channel/name "off-topic",
       :channel/message-count 8}
      {:channel/slack-id "C0702A7SB",
       :channel/name "yada",
       :channel/message-count 11}
      {:channel/slack-id "C050HE28Y",
       :channel/name "clojure-russia",
       :channel/message-count 16}
      {:channel/slack-id "C053AK3F9",
       :channel/name "beginners",
       :channel/message-count 163}
      {:channel/slack-id "C68M60S4F",
       :channel/name "fulcro",
       :channel/message-count 58}
      {:channel/slack-id "C1B1BB2Q3",
       :channel/name "clojure-spec",
       :channel/message-count 19}
      {:channel/slack-id "C0F2A0MJN",
       :channel/name "parinfer",
       :channel/message-count 98}
      {:channel/slack-id "C064BA6G2",
       :channel/name "clojure-uk",
       :channel/message-count 34}
      {:channel/slack-id "C562XHH0C",
       :channel/name "portkey",
       :channel/message-count 8}
      {:channel/slack-id "C0617A8PQ",
       :channel/name "cider",
       :channel/message-count 17}
      {:channel/slack-id "C07UQ678E",
       :channel/name "cljs-dev",
       :channel/message-count 41},)})