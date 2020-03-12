(ns clojurians-log.views
  (:require [hiccup2.core :as hiccup]
            [cemerick.url :refer [url]]
            [clojurians-log.time-util :as cl.tu]
            [clojure.string :as str]
            [clojurians-log.slack-messages :as slack-messages]
            [reitit.core]))

(def origin "https://clojurians-log.clojureverse.org")

(defn- thread-child?
  "Answers if the `message` is a message within a thread."
  [{:message/keys [thread-ts ts]:as message}]
  (and thread-ts (not= ts thread-ts)))

(defn- find-message-with-ts
  [messages ts]
  (some #(when (= (:message/ts %) ts) %) messages))

(defn page-head [{:data/keys [title]}]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible", :content "IE=edge"}]
   [:title title]

   ;; Remnant of the static site, let's not sell out our user's click data to
   ;; google just for a font.
   #_[:link {:href "http://fonts.googleapis.com/css?family=Lato:400,700,900",
             :rel "stylesheet",
             :type "text/css"}]

   ;; This one is just copied over from the static site, seems it was generated
   ;; with Compass and SASS. At some point I'd prefer to delete this and do the
   ;; styling over in clean Garden or Garden+Tachyons.
   [:link {:href "/css/legacy.css", :rel "stylesheet", :type "text/css"}]
   [:link {:href "/css/style.css", :rel "stylesheet", :type "text/css"}]])

(defn og-title [{:data/keys [title channel date target-message messages usernames] :as context}]
  (cond
    ;; Is the message part of a thread?
    (thread-child? target-message)
    (let [thread-parent (find-message-with-ts messages (:message/thread-ts target-message))]
      (format "@%s in reply to @%s, in #%s, %s | Clojurians Slack"
              (get-in target-message [:message/user :user/name])
              (get-in thread-parent [:message/user :user/name])
              (:channel/name channel)
              date))

    :else
    (format "@%s in #%s, %s | Clojurians Slack"
            (get-in target-message [:message/user :user/name])
            (:channel/name channel)
            date)))

(defn fork-me-badge []
  [:a {:href "https://github.com/clojureverse/clojurians-log-app"}
   [:img {:style "position: absolute; top: 0; right: 0; border: 0;"
          :src "https://s3.amazonaws.com/github/ribbons/forkme_right_orange_ff7600.png"
          :alt "Fork me on GitHub"}]])


(defn path-for [context & args]
  (reitit.core/match->path
   (apply reitit.core/match-by-name (get-in context [:request :reitit.core/router]) args)))

(defn log-page-head [{:data/keys [title channel date target-message http-origin usernames] :as context}]
  (cond-> (page-head context)
    ;; Always add a canonical rel
    :-> (conj [:link {:rel "canonical" :href (str origin (path-for context
                                                                   :clojurians-log.routes/channel-date
                                                                   {:channel (:channel/name channel)
                                                                    :date date}))}])

    ;; Are we targeting a specific message in the log page?
    ;; If, add tags to enable open graph support.
    ;; This allows external services to generate a preview/summary card of the page.
    (some? target-message)
    (conj [:meta {:property "og:title" :content (og-title context)}]
          [:meta {:property "og:type" :content "website"}]
          [:meta {:property "og:url" :content (str (url http-origin
                                                        (path-for context
                                                                  :clojurians-log.routes/message
                                                                  {:channel (:channel/name channel)
                                                                   :date date
                                                                   :ts (:message/ts target-message)})))}]
          [:meta {:property "og:image" :content (get-in target-message [:message/user :user-profile/image-48])}]
          [:meta {:property "og:image:width" :content 50}]
          [:meta {:property "og:image:height" :content 50}]
          [:meta {:property "og:description" :content
                  (slack-messages/message->text (:message/text target-message) usernames)}]
          ;; Add javascript to jump to the targeted message when the page is finished loading
          [:script (hiccup.util/raw-string
                    (format
                     "document.addEventListener(\"DOMContentLoaded\", function(event) {
                          var element = document.getElementById('%s');
                          element.classList.add(\"targeted\");
                          element.scrollIntoView();
                       });"
                     (cl.tu/format-inst-id (:message/inst target-message))))])))

(defn channel-day-offset
  "Given a list of [date msg-count] pairs, return `date` of the entry that is
  `offset` positions away. Returns nil if the applying the offset goes out of
  bounds."
  [channel-days today offset]
  (as-> channel-days $
    (map vector (range) $)
    (some (fn [[index [a-date]]]
            (when (= today a-date)
              index))
          $)
    (+ $ offset)
    (nth channel-days $ nil)
    (first $)))

(defn- log-page-header [{:data/keys [channel date channel-days] :as context}]
  [:div.header
   [:div.team-menu [:a {:href "/"} "Clojurians"]]
   [:div.channel-menu
    [:span.channel-menu_name [:span.channel-menu_prefix "#"] (:channel/name channel)]
    [:span.day-arrows
     (when-let [past-date (channel-day-offset channel-days date 1)]
       [:a {:href (path-for context
                            :clojurians-log.routes/channel-date
                            {:channel (:channel/name channel)
                             :date past-date})}
        [:div.day-prev "<"]])
     date
     (when-let [future-date (channel-day-offset channel-days date -1)]
       [:a {:href (path-for context
                            :clojurians-log.routes/channel-date
                            {:channel (:channel/name channel)
                             :date future-date})}
        [:div.day-next ">"]])]]])

(defn- channel-list [{:data/keys [date channels] :as context}]
  [:div.listings_channels
   [:h2.listings_header.listings_header_date date]
   [:h2.listings_header "Channels"]
   [:ul.channel_list
    (for [{:channel/keys [name slack-id message-count]} channels]
      [:li.channel
       [:span.channel_name
        [:a
         {:href (path-for context
                          :clojurians-log.routes/channel-date
                          {:channel name
                           :date date})}
         [:span [:span.prefix "#"] " " name " (" message-count ")"]]]])]])

(defn- single-message
  "Returns the hiccup of a single message"
  [{:data/keys [usernames channel date hostname emojis] :as context}
   {:message/keys [user inst user text thread-ts ts] :as message}]

  (let [{:user/keys         [name slack-id]
         :user-profile/keys [image-48]} user]

    ;; things in the profile
    ;; :image_512 :email :real_name_normalized :image_48 :image_192 :real_name :image_72 :image_24
    ;; :avatar_hash :title :team :image_32 :display_name :display_name_normalized
    (list [:div.message
           {:id (cl.tu/format-inst-id inst) :class (when (thread-child? message) "thread-msg")}
           [:a.message_profile-pic {:href (str "https://clojurians.slack.com/team/" slack-id) :style (str "background-image: url(" image-48 ");")}]
           [:a.message_username {:href (str "https://clojurians.slack.com/team/" slack-id)} name]
           [:span.message_timestamp [:a {:rel  "nofollow"
                                         :href (path-for context
                                                         :clojurians-log.routes/message
                                                         {:channel (:channel/name channel)
                                                          :date date
                                                          :ts ts})}
                                     (cl.tu/format-inst-time inst)]]
           [:span.message_star]
           [:span.message_content [:p (slack-messages/message->hiccup text usernames emojis)]]])))

(defn- message-hiccup
  "Returns either a single message hiccup, or if the given message starts a thread,
  hiccup of all thread messages in a list"
  [context message]
  (if-let [children (:message/children message)]
    (concat (list (single-message context message))
            (for [thread-msg children]
              (single-message context thread-msg)))

    (single-message context message)))

(defn- message-history [{:data/keys [messages] :as context}]
  [:div.message-history
   (map #(message-hiccup context %) messages)])

(defn- log-page-html [context]
  [:html
   (log-page-head context)
   [:body
    (log-page-header context)
    [:div.main
     [:div.listings
      [:p.disclaimer "This page is not created by, affiliated with, or supported by Slack Technologies, Inc."]
      (channel-list context)
      [:div.listings_direct-messages]]
     (message-history context)]]])

(defn- channel-page-html [{:data/keys [channel-days channel-name] :as context}]
  [:html
   (page-head context)
   [:body
    [:div.main
     [:h1 "Channel: #" channel-name]
     [:ul
      (for [[day cnt] channel-days]
        [:li [:a {:href (path-for context
                                  :clojurians-log.routes/channel-date
                                  {:channel channel-name
                                   :date day})}
              day " (" cnt ")"]])]]]])

(defn- channel-list-page-html [{:data/keys [channels] :as context}]
  [:html
   (page-head context)
   [:body
    [:div.main
     (fork-me-badge)
     [:a {:href (path-for context :clojurians-log.routes/about)}
      "About Clojurians Slack Log"]
     [:h1 "Channels"]
     [:ul
      (for [{:channel/keys [name]} channels]
        [:li
         [:a {:href (path-for context
                              :clojurians-log.routes/channel
                              {:channel name})}
          "# " name]])]]]])

(defn log-page [context]
  (assoc context :response/html (log-page-html context)))

(defn channel-page [context]
  (assoc context :response/html (channel-page-html context)))

(defn channel-list-page [context]
  (assoc context :response/html (channel-list-page-html context)))

(defn- about-html [context]
  [:html
   (page-head context)
   [:body
    [:div.main
     (fork-me-badge)
     [:h1 "About Clojurians Slack Log"]
     [:p "One of the main on-line hangouts for Clojure people is the " [:a {:href "clojurians.net"} "Clojurians Slack community"] ". Unfortunately it suffers from its popularity. Slack will only retain the last 10,000 messages of history, which means that less than two weeks of logs are kept available. A lot of valuable information is in that chat history, and so the Clojureverse team set up a logging service so that these logs aren’t lost in time."]]]])

(defn about [context]
  (assoc context :response/html (about-html context)))
