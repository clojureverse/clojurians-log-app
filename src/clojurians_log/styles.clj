(ns clojurians-log.styles
  (:require [garden-watcher.def :refer [defstyles]]
            [garden.color :as c]))

(def primary-color (c/rgb 62 49 60))

(def small-square-button
  {:display :inline-block
   :background-color primary-color
   :color :white
   :min-height "1rem"
   :min-width "1rem"
   :text-align :center
   :border-radius "20%"})

(defstyles style
  [:h1 {:text-decoration "underline"}]

  [:.day-arrows
   {:margin "1rem 0 0 1rem"}

   [:div.day-prev (assoc small-square-button
                         :margin-right "0.3rem")]
   [:div.day-next (assoc small-square-button
                         :margin-left "0.3rem")]]

  [:.message.thread-msg {:margin-left "1rem"}]

  [:.emoji
   [:img {:height "22px"
          :width  "22px"}]]

  [:.content
   {:display "flex"
    :height "100%"
    :overflow "hidden"}]

  [:.sidebar
   {:max-width "250px"
    :flex-shrink "0"}]

  [:.channel-page
   [:.main
    {:margin "2rem"}]]

  [:.channel-list-page
   [:.main
    {:margin "2rem"}]]

  [:.about-page
   [:.main
    {:margin "2rem"}]]

  [:.sitemap-page
   [:.main
    {:margin "2rem"}]]

  [:.channel-index
   :.channel-days
   [:li
    {:margin "0.3rem 0"
     :font-size "1.2rem"}]]

  [:.app-title
   {:line-height "2rem"
    :font-weight 900
    :text-decoration "none"}]

  [:.sidebar
   [:.app-title
    {:padding "1rem 1rem"}]]

  [:.sidebar
   [:.app-title
    ["" :a
     {:color "#ffffff"
      :background "#3e313c";
      :border-bottom "2px solid #372c36"}]]]

  [:.app-title
   [:a:hover
    {:text-decoration "underline"}]]

  [:.padding-15px
   {:padding-left "15px"}]

  )
