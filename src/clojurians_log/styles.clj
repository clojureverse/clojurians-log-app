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
   {:margin-left "1rem"}

   [:div.day-prev (assoc small-square-button
                         :margin-right "0.3rem")]
   [:div.day-next (assoc small-square-button
                         :margin-left "0.3rem")]]

  [:.message.thread-msg {:margin-left "1rem"}]

  [:.emoji
   [:img {:height "22px"
          :width  "22px"}]])
