(ns clojurians-log.views
  (:require [hiccup2.core :as hiccup]
            [clojurians-log.time-util :as cl.tu]
            [clojure.string :as str]
            [clojure.pprint :as pp]))

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
   [:link {:href "/css/legacy.css", :rel "stylesheet", :type "text/css"}]])

(defn- log-page-header [{:data/keys [channel]}]
  [:div.header
   [:div.team-menu [:a {:href "/"} "Clojurians"]]
   [:div.channel-menu
    [:span.channel-menu_name [:span.channel-menu_prefix "#"] (:channel/name channel) ]]])

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

(defn- message-history [{:data/keys [messages]}]
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
      [:span.message_timestamp [:a {:href (str "#" (cl.tu/format-inst-id inst))} (cl.tu/format-inst-time inst)]]
      [:span.message_star]
      ;; TODO render slack style markdown (especially code blocks)
      [:span.message_content [:p (hiccup/raw text)]]
      #_[:pre {:style {:display "none"}} (with-out-str (pp/pprint message))]])])

(defn- log-page-html [context]
  [:html
   (page-head context)
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
        [:li [:a {:href (str "/" channel-name "/" day ".html")} "#" channel-name " (" cnt ")"]])]]]])

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
