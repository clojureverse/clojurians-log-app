(ns clojurians-log.views
  (:require [hiccup2.core :as hiccup]
            [clojurians-log.time-util :as cl.tu]
            [clojure.string :as str]
            [clojure.pprint :as pp]))

(defn log-page-head [{:data/keys [channel date]}]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:http-equiv "X-UA-Compatible", :content "IE=edge"}]
   [:title  (:name channel) " " date " | Clojurians Slack Log"]

   ;; Remnant of the static site, let's not sell out our user's click data to
   ;; google just for a font.
   #_[:link {:href "http://fonts.googleapis.com/css?family=Lato:400,700,900",
             :rel "stylesheet",
             :type "text/css"}]

   ;; This one is just copied over from the static site, seems it was generated
   ;; with Compass and SASS. At some point I'd prefer to delete this and do the
   ;; styling over in clean Garden or Garden+Tachyons.
   [:link {:href "/css/legacy.css", :rel "stylesheet", :type "text/css"}]])

(defn log-page-header [{:data/keys [channel]}]
  [:div.header
   [:div.team-menu [:a {:href "/"} "Clojurians"]]
   [:div.channel-menu
    [:span.channel-menu_name [:span.channel-menu_prefix "#"] (:name channel) ]]])

(defn channel-list [{:data/keys [date]}]
  [:div.listings_channels
   [:h2.listings_header.listings_header_date date]
   [:h2.listings_header "Channels"]
   [:ul.channel_list
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clojure-canada/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclojure-canada\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../testing/2015-09-03.html"}
       [:span [:span.prefix "#"] "\ntesting\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clara/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclara\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clojure-brasil/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclojure-brasil\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../code-reviews/2015-09-03.html"}
       [:span [:span.prefix "#"] "\ncode-reviews\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../boot/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nboot\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../datomic/2015-09-03.html"}
       [:span [:span.prefix "#"] "\ndatomic\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../sydney/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nsydney\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clojure-nl/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclojure-nl\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../re-frame/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nre-frame\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clojure-russia/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclojure-russia\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../events/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nevents\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../jobs/2015-09-03.html"}
       [:span [:span.prefix "#"] "\njobs\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clojurescript/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclojurescript\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../ldnclj/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nldnclj\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../hoplon/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nhoplon\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../onyx/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nonyx\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clojure-italy/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclojure-italy\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../beginners/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nbeginners\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clojure-argentina/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclojure-argentina\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../sneer-br/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nsneer-br\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../om/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nom\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../admin-announcements/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nadmin-announcements\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../cljs-dev/2015-09-03.html"}
       [:span [:span.prefix "#"] "\ncljs-dev\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../reagent/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nreagent\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../cider/2015-09-03.html"}
       [:span [:span.prefix "#"] "\ncider\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../immutant/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nimmutant\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../melbourne/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nmelbourne\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../off-topic/2015-09-03.html"}
       [:span [:span.prefix "#"] "\noff-topic\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../cursive/2015-09-03.html"}
       [:span [:span.prefix "#"] "\ncursive\n"]]]]
    [:li.channel
     [:span.channel_name
      [:a
       {:href "../clojure/2015-09-03.html"}
       [:span [:span.prefix "#"] "\nclojure\n"]]]]]])

(defn message-history [{:data/keys [messages]}]
  [:div.message-history
   (for [message messages
         :let [{:keys [event_ts channel type user_profile ts inst team user subtype text]} message
               {:keys [id name real_name is_admin is_owner profile]} user
               {:keys [image_48]} profile]]

     ;; things in the profile
     ;; :image_512 :email :real_name_normalized :image_48 :image_192 :real_name :image_72 :image_24
     ;; :avatar_hash :title :team :image_32 :display_name :display_name_normalized

     [:div.message {:id (cl.tu/format-inst-id inst)}
      [:a.message_profile-pic {:href "" :style (str "background-image: url(" image_48 ");")}]
      [:a.message_username {:href ""} name]
      [:span.message_timestamp [:a {:href (str "#" (cl.tu/format-inst-id inst))} (cl.tu/format-inst-time inst)]]
      [:span.message_star]
      ;; TODO render slack style markdown (especially code blocks)
      [:span.message_content [:p (hiccup/raw text)]]
      [:pre {:style {:display "none"}} (with-out-str (pp/pprint message))]])])

(defn log-page [context]
  (assoc context
         :response/html
         [:html
          (log-page-head context)
          [:body
           (log-page-header context)
           [:div.main
            [:div.listings
             [:p.disclaimer
              "This page is not created by, affiliated with, or supported by Slack Technologies, Inc."]
             (channel-list context)
             [:div.listings_direct-messages]]
            (message-history context)]]]))
