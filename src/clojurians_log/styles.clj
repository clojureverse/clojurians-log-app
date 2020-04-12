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
  [:ol :ul {:list-style "none"}]

  [:table {
           :border-collapse "collapse"
           :border-spacing 0
           }]


  [:html :body :div :span :applet :object :iframe
   :h1 :h2 :h3 :h4 :h5 :h6 :p :blockquote :pre
   :a :abbr :acronym :address :big :cite :code
   :del :dfn :em :img :ins :kbd :q :s :samp
   :small :strike :strong :sub :sup :tt :var
   :b :u :i :center
   :dl :dt :dd :ol :ul :li
   :fieldset :form :label :legend
   :table :caption :tbody :tfoot :thead :tr :th :td
   :article :aside :canvas :details :embed
   :figure :figcaption :footer :header :hgroup
   :menu :nav :output :ruby :section :summary
   :time :mark :audio :video
   {:margin 0
    :padding 0
    :border 0
    :font "inherit"
    :font-size "100%"
    :vertical-align "baseline"}]

  [:html
   {:line-height 1}]

  [:html
   {:position "relative"
    :height "100%"
    :font-size "16px"
    :font-family "'Lato', sans-serif"}]

  [:*
   {:box-sizing "border-box"}]

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
   {:padding "15px"}]

  [:.disclaimer
   {:font-size "0.8rem"
    :padding-left "1rem"
    :padding-top "1rem"
    :margin-right "17px"}]

  [:.listings
   {:height "100%"
    :color "#ab9ba9"
    :background-color "#4d394b"
    :overflow-y "auto"
    :overflow-x "hidden"
    }]

  [:.channel_list {:list-style-type "none"
                   :text-align "left"
                   :color "#ab9ba9"}]

  [:.channel {:line-height "24px"
              :-moz-border-radius-topright "0.25rem"
              :-webkit-border-top-right-radius "0.25rem"
              :border-top-right-radius "0.25rem"
              :-moz-border-radius-bottomright "0.25rem"
              :-webkit-border-bottom-right-radius "0.25rem"
              :border-bottom-right-radius "0.25rem"
              :margin-right "17px"
              :color "#ffffff"
              :padding-left "1rem"}]

  [:.channel
   [:a {:color "#ffffff"
        :text-decoration "none"}]]

  [:.channel
   [:a:hover {:text-decoration "underline"}]]

  [:.channel-menu {:margin-top "1rem"}]

  [:.channel-menu_name
   {:display "inline-block"
    :padding "0 .5rem 0 2.5rem"
    :color "#555459"
    :font-size "1.4rem"
    :font-weight "900"
    :cursor "pointer"}]

  [:.channel-menu_prefix
   {:color "#9e9ea6"
    :padding-right ".1rem"
    :font-weight "500"}]

  )
