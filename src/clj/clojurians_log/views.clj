(ns clojurians-log.views
  (:require [hiccup2.core :as hiccup]
            [clojurians-log.time-util :as cl.tu]
            [clojure.string :as str]
            [clojurians-log.slack-messages :as slack-messages]))

(defn page-head [{:data/keys [title channel date]}]
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

(defn log-page-head [{:data/keys [title channel date target-message hostname usernames] :as context}]
  (cond-> (page-head context)
    ;; Are we targeting a specific message in the log page?
    ;; If, add tags to enable open graph support.
    ;; This allows external services to generate a preview/summary card of the page.
    (not (nil? target-message))
    (conj [:meta {:property "og:title" :content title}]
          [:meta {:property "og:type" :content "website"}]
          [:meta {:property "og:url" :content (str (url hostname
                                                        (:channel/name channel)
                                                        date
                                                        (cl.tu/format-inst-id (:message/inst target-message))))}]
          [:meta {:property "og:image" :content (get-in target-message [:message/user :user-profile/image-48])}]
          [:meta {:property "og:image:width" :content 50}]
          [:meta {:property "og:image:height" :content 50}]
          [:meta {:property "og:description" :content
                  ;; (format "%s: %s" (get-in target-message [:message/user :user/name]) (:message/text target-message))
                  (hiccup/html
                   (slack-messages/render-hiccup (:message/text target-message) usernames))}]
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
    (some (fn [[index [a-date msg-count]]] (when (and (= a-date today)
                                                     (not (zero? msg-count)))
                                            index)) $)
    (+ $ offset)
    (nth channel-days $ nil)
    (first $)))

(defn- log-page-header [{:data/keys [channel date channel-days]}]
  [:div.header
   [:div.team-menu [:a {:href "/"} "Clojurians"]]
   [:div.channel-menu
    [:span.channel-menu_name [:span.channel-menu_prefix "#"] (:channel/name channel)]
    [:span.day-arrows
     (if-let [prev-date (channel-day-offset channel-days date -1)]
       [:a {:href (str prev-date ".html")} [:div.day-prev "<"]])
     date
     (if-let [next-date (channel-day-offset channel-days date 1)]
       [:a {:href (str next-date ".html")} [:div.day-next ">"]])]]])

(defn- channel-list [{:data/keys [date channels]}]
  [:div.listings_channels
   [:h2.listings_header.listings_header_date date]
   [:h2.listings_header "Channels"]
   [:ul.channel_list
    (for [{:channel/keys [name slack-id message-count]} channels]
      [:li.channel
       [:span.channel_name
        [:a
         {:href (str "/" name "/" date ".html")}
         [:span [:span.prefix "#"] " " name " (" message-count ")"]]]])]])

(defn- message-history [{:data/keys [messages usernames channel date hostname]}]
  [:div.message-history
   (for [message messages
         :let [{:message/keys [user inst user text]} message
               {:user/keys [name]
                :user-profile/keys [image-48]} user]]

     ;; things in the profile
     ;; :image_512 :email :real_name_normalized :image_48 :image_192 :real_name :image_72 :image_24
     ;; :avatar_hash :title :team :image_32 :display_name :display_name_normalized
     [:div.message {:id (cl.tu/format-inst-id inst)}
      [:a.message_profile-pic {:href "" :style (str "background-image: url(" image-48 ");")}]
      [:a.message_username {:href ""} name]
      [:span.message_timestamp [:a {:href (->> (url hostname
                                                    (:channel/name channel)
                                                    date
                                                    (cl.tu/format-inst-id (:message/inst message)))
                                               str)}
                                (cl.tu/format-inst-time inst)]]
      [:span.message_star]
      [:span.message_content [:p (slack-messages/render-hiccup text usernames)]]])])

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

(defn- channel-page-html [{:data/keys [days channel-name] :as context}]
  [:html
   (page-head context)
   [:body
    [:div.main
     [:h1 "Channel: #" channel-name]
     [:ul
      (for [[day cnt] days]
        [:li [:a {:href (str "/" channel-name "/" day ".html")} day " (" cnt ")"]])]]]])

(defn- channel-list-page-html [{:data/keys [channels] :as context}]
  [:html
   (page-head context)
   [:body
    [:div.main
     [:h1 "Channels"]
     [:ul
      (for [{:channel/keys [name]} channels]
        [:li
         [:a {:href (str "/" name)} "# " name]])]]]])

(defn log-page [context]
  (assoc context :response/html (log-page-html context)))

(defn channel-page [context]
  (assoc context :response/html (channel-page-html context)))

(defn channel-list-page [context]
  (assoc context :response/html (channel-list-page-html context)))
