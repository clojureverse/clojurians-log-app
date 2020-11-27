(ns clojurians-log.views
  (:require [hiccup2.core :as hiccup]
            [cemerick.url :refer [url]]
            [clojurians-log.time-util :as cl.tu]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [clojurians-log.slack-messages :as slack-messages]
            [reitit.core]
            [clojure.java.io :as io]))

(defn- thread-child?
  "Answers if the `message` is a message within a thread."
  [{:message/keys [thread-ts ts]:as message}]
  (and thread-ts (not= ts thread-ts)))

(defn- find-message-with-ts
  [messages ts]
  (some #(when (= (:message/ts %) ts) %) messages))

(defn jsfile [path]
  (when-let [f (io/file (io/resource (str "public" path)))]
    (let [ts (.lastModified f)]
      [:script
       {:src (str path "?version=" ts)}])))

(defn stylesheet [path]
  (when-let [f (io/file (io/resource (str "public" path)))]
    (let [ts (.lastModified f)]
      [:link
       {:href (str path "?version=" ts)
        :rel "stylesheet"
        :type "text/css"}] )))

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

   (stylesheet "/css/style.css")
   (stylesheet "/css/gh-fork-ribbon.min.css")])

(defn og-title [{:keys [request]
                 :data/keys [title channel date target-message messages usernames] :as context}]
  (let [app-title (get-in request [:config :application :title])]
    (cond
      ;; Is the message part of a thread?
      (thread-child? target-message)
      (let [thread-parent (find-message-with-ts messages (:message/thread-ts target-message))]
        (format "@%s in reply to @%s, in #%s, %s | %s"
                (get-in target-message [:message/user :user/name])
                (get-in thread-parent [:message/user :user/name])
                (:channel/name channel)
                date
                app-title))

      :else
      (format "@%s in #%s, %s | %s"
              (get-in target-message [:message/user :user/name])
              (:channel/name channel)
              date
              app-title))))

(defn fork-me-badge []
  [:a.github-fork-ribbon
   {:href "https://github.com/clojureverse/clojurians-log-app"
    :data-ribbon "Fork me on GitHub"
    :title "Fork me on GitHub"}
   "Fork me on GitHub"])


(defn path-for [context & args]
  (reitit.core/match->path
   (apply reitit.core/match-by-name (get-in context [:request :reitit.core/router]) args)))

(defn log-page-head [{:data/keys [title channel date target-message http-origin usernames] :as context}]
  (cond-> (page-head context)
    ;; Always add a canonical rel
    :-> (conj [:link {:rel "canonical" :href (str http-origin (path-for context
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

(defn- log-page-sidebar [{:data/keys [channel date channel-days] :as context}]
  [:div.sidebar.listings
   [:div.app-title [:a {:href "/"} (get-in context [:request :config :application :title])]]
   [:p.disclaimer "This page is not created by, affiliated with, or supported by Slack Technologies, Inc."]
   (channel-list context)
   [:div.listings_direct-messages]])

(defn- log-page-header [{:data/keys [channel date channel-days] :as context}]
  [:div.header
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

(defn- single-message
  "Returns the hiccup of a single message"
  [{:keys [request]
    :data/keys [usernames channel date hostname emojis]
    :as context}
   {:message/keys [user inst user text thread-ts ts] :as message}]

  (let [{:user/keys         [name slack-id]
         :user-profile/keys [display-name real-name image-48]} user
        slack-instance (:clojurians-log.application/slack-instance request)]

    ;; things in the profile
    ;; :image_512 :email :real_name_normalized :image_48 :image_192 :real_name :image_72 :image_24
    ;; :avatar_hash :title :team :image_32 :display_name :display_name_normalized
    (list [:div.message
           {:id (cl.tu/format-inst-id inst) :class (when (thread-child? message) "thread-msg")}
           [:a.message_profile-pic {:href (str slack-instance "/team/" slack-id) :style (str "background-image: url(" image-48 ");")}]
           [:a.message_username {:href (str slack-instance "/team/" slack-id)}
            (some #(when-not (str/blank? %) %) [display-name real-name name])]
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
    (fork-me-badge)
    [:div.content
     (log-page-sidebar context)
     [:div.main
      (log-page-header context)
      (message-history context)]]]])

(defn- channel-page-html [{:data/keys [channel-days channel-name] :as context}]
  [:html.channel-page
   (page-head context)
   [:body
    (fork-me-badge)
    [:div.main
     [:div.app-title [:a {:href "/"} (get-in context [:request :config :application :title])]]
     [:h1 "Channel: #" channel-name]
     [:ul.channel-days
      (for [[day cnt] channel-days]
        [:li [:a {:href (path-for context
                                  :clojurians-log.routes/channel-date
                                  {:channel channel-name
                                   :date day})}
              day " (" cnt ")"]])]]]])

(defn- channel-list-page-html [{:data/keys [channels] :as context}]
  [:html.channel-list-page
   (page-head context)
   [:body
    [:div.main
     (fork-me-badge)
     [:table
      [:tr
       [:td
        [:div.app-title
         [:a {:href "/"}
          (get-in context [:request :config :application :title])]]]
       [:td.padding-15px
        [:a {:href (path-for context :clojurians-log.routes/about)}
         "About"]]
       [:td.padding-15px
        [:a {:href (path-for context :clojurians-log.routes/sitemap)}
         "Sitemap"]]]]
     [:h1 "Channels"]
     [:ul.channel-index
      (for [{:channel/keys [name]} channels]
        [:li
         [:a {:href (path-for context
                              :clojurians-log.routes/channel
                              {:channel name})}
          "# " name]])]]]])

(defn- about-html [context]
  [:html.about-page
   (page-head context)
   [:body
    (fork-me-badge)
    [:div.main
     [:div.app-title
      [:a {:href "/"}
       (get-in context [:request :config :application :title])]]
     [:section#about
      (:data/about-hiccup context)]]]])

(defn- sitemap-xml [{:data/keys [channel-day-tuples http-origin] :as context}]
  [:urlset {:xmlns "http://www.sitemaps.org/schemas/sitemap/0.9"}
   (for [[{:channel/keys [name]} channel-days] channel-day-tuples]
     (for [[day cnt] channel-days]
       [:url
        [:loc (str http-origin
                   (path-for context
                             :clojurians-log.routes/channel-date
                             {:channel name
                              :date day}))]
        [:lastmod day]]))])


(defn- page-head-stats [{:data/keys [title]}]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
   [:meta {:content="width=device-width, initial-scale=1" :name "viewport"}]
   [:title title]
   [:link {:href "https://unpkg.com/sakura.css/css/sakura.css"
           :rel "stylesheet"
           :type "text/css"}]
   (stylesheet "/css/gh-fork-ribbon.min.css")
   [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.9.4/Chart.min.js"}]
   ])

(defn- message-stats-page-html [{:data/keys [message-stats] :as context}]
  [:html
   (page-head-stats context)
   [:body
    [:div
     [:h4 "Slack message stats"]
     [:p 
      [:strong {:style {:border-bottom "1px solid black"}} (:day (first message-stats))]
      " to "
      [:strong {:style {:border-bottom "1px solid black"}} (:day (last message-stats))]]
     [:div {:width "100%"}
      [:canvas#message-chart]]
     [:h4 "Total message count: " (reduce #(+ %1 (:msg-count %2)) 0 message-stats)]
     [:table
      [:thead
       [:tr
        [:th "Day"]
        [:th "Message count"]]]
      [:tbody
       (for [day-stat message-stats] [:tr [:td (:day day-stat)] [:td (:msg-count day-stat)]])]]]
    [:script#message-data {:type "application/json"} (json/write-str message-stats)]
    (jsfile "/js/stats.js")]])

(defn log-page [context]
  (assoc context :response/html (log-page-html context)))

(defn channel-page [context]
  (assoc context :response/html (channel-page-html context)))

(defn channel-list-page [context]
  (assoc context :response/html (channel-list-page-html context)))

(defn about [context]
  (assoc context :response/html (about-html context)))

(defn sitemap [context]
  (assoc context :response/xml (sitemap-xml context)))

(defn message-stats-page [context]
  (assoc context :response/html (message-stats-page-html context)))