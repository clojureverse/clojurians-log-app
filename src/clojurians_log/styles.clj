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
  [:html :body :div :span :applet :object :iframe :h1 :h2
   :h3 :h4 :h5 :h6 :p :blockquote :pre :a :abbr :acronym
   :address :big :cite :code :del :dfn :em :img :ins :kbd
   :q :s :samp :small :strike :strong :sub :sup :tt :var
   :b :u :i :center :dl :dt :dd :ol :ul :li :fieldset
   :form :label :legend :table :caption :tbody :tfoot
   :thead :tr :th :td :article :aside :canvas :details
   :embed :figure :figcaption :footer :header :hgroup
   :menu :nav :output :ruby :section :summary :time
   :mark :audio :video
   {:margin "0",
    :padding "0",
    :border "0",
    :font "inherit",
    :font-size "100%",
    :vertical-align "baseline"}]

  [:html {:line-height "1"}]

  [:ol :ul {:list-style "none"}]

  [:table {:border-collapse "collapse", :border-spacing "0"}]

  [:caption :th :td
   {:text-align "left", :font-weight "normal", :vertical-align "middle"}]

  [:q :blockquote {:quotes "none"}]

  ;; in original legacy.css:
  ;; q:before, q:after, blockquote:before, blockquote:after {
  ;; content: "";
  ;; content: none;
  ;; }

  [:q:before
   :q:after
   :blockquote:before
   :blockquote:after
   {:content "none"}]

  [:a [:img {:border "none"}]]

  [:article
   :aside
   :details
   :figcaption
   :figure
   :footer
   :header
   :hgroup
   :main
   :menu
   :nav
   :section
   :summary
   {:display "block"}]

  [:* {:box-sizing "border-box"}]

  [:html
   {:position "relative",
    :height "100%",
    :font-size "16px",
    :font-family "'Lato', sans-serif"}]

  [:body {:height "100%", :width "100%", :margin "0"}]

  [:.header {:height "2rem", :width "100%"}]

  [:.main {:height "100%", :width "100%"}]

  [:.footer
   {:position "absolute",
    :left "0",
    :bottom "0",
    :height "64px",
    :width "100%"}]

  [:.channel-menu {:margin-top "1rem"}]

  [:.channel-menu_name
   {:display "inline-block",
    :padding "0 .5rem 0 2.5rem",
    :color "#555459",
    :font-size "1.4rem",
    :font-weight "900",
    :cursor "pointer"}]

  [:.channel-menu_prefix
   {:color "#9e9ea6", :padding-right ".1rem", :font-weight "500"}]

  [:.listings
   {:height "100%",
    :color "#ab9ba9",
    :background-color "#4d394b",
    :overflow-y "auto",
    :overflow-x "hidden"}]

  [:.message-history
   {:overflow-y "auto",
    :overflow-x "hidden",
    :height "100%",
    :margin-left "2rem"}]

  [:.listings_channels {:margin "1rem 0 2rem"}]

  [:.listings_header
   {:text-align "left",
    :font-size ".8rem",
    :line-height "1.25rem",
    :margin "0 1rem .1rem",
    :text-transform "uppercase",
    :font-weight "700",
    :color "#ab9ba9"}]

  [:.listings_header_date {:color "#4c9689"}]

  [:.channel_list
   {:list-style-type "none", :text-align "left", :color "#ab9ba9"}]

  [:.channel
   {:line-height "24px",
    :-moz-border-radius-topright "0.25rem",
    :-webkit-border-top-right-radius "0.25rem",
    :border-top-right-radius "0.25rem",
    :-moz-border-radius-bottomright "0.25rem",
    :-webkit-border-bottom-right-radius "0.25rem",
    :border-bottom-right-radius "0.25rem",
    :margin-right "17px",
    :color "#ffffff",
    :padding-left "1rem"}]

  [:.channel [:a {:color "#ffffff", :text-decoration "none"}]]

  [:.channel [:a:hover  {:text-decoration "underline"}]]

  [:.unread
   {:line-height "14px",
    :color "#ffffff",
    :vertical-align "baseline",
    :white-space "nowrap",
    :font-size ".8rem",
    :font-weight "700",
    :float "right",
    :margin-top "3px",
    :background "#eb4d5c",
    :padding "2px 9px",
    :text-shadow "0 1px 0 rgba(0,0,0,0.2)",
    :margin-right "3px",
    :border-radius "9px"}]

  [:.channel.active {:background "#4c9689"}]

  [:.channel_prefix {:color "#b2d5c9"}]

  [:.disclaimer
   {:font-size "0.8rem",
    :padding-left "1rem",
    :padding-top "1rem",
    :margin-right "17px"}]

  [:.message
   {:position "relative",
    :margin-top ".5rem",
    :padding ".25rem 2rem .1rem 3rem",
    :min-height "36px"}]

  [:.message_profile-pic
   {:width "36px",
    :-moz-border-radius "0.2rem",
    :display "block",
    :position "absolute",
    :-webkit-border-radius "0.2rem",
    :border-radius "0.2rem",
    :background-size "cover",
    :height "36px",
    :left "0",
    :background-image
    "url(data:image/png;base64,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)"}]

  [:.message_username
   {:padding-right ".25rem",
    :color "#8b898f",
    :margin-left "0",
    :font-style "normal",
    :text-decoration "none"}]

  [:.message_timestamp
   {:line-height "1.2rem",
    :color "#babbbf",
    :text-align "left",
    :font-size "12px",
    :top "0",
    :margin-left "0",
    :width "36px",
    :display "inline",
    :position "relative",
    :margin-right "0",
    :left "0"}]

  [:.message_timestamp [:a {:text-decoration "none", :color "#babbbf"}]]

  [:.message_timestamp
   [:a:hover
    {:text-decoration "underline", :color "#5050df"}]]

  [:.message:target :.message.targeted {:background-color "#FFF8DC"}]

  [:.message_content {:display "block", :min-height "1rem"}]

  ;;in orginial legacy.css
  ;;  .user-menu {
  ;; float: left;
  ;; width: 220px;
  ;; height: 100%;
  ;; cursor: pointer;
  ;; background: #3e313c;
  ;; border-top: 2px solid #372c36;
  ;; padding: 7px 0 9px 8px;
  ;; height: 4rem;
  ;; position: fixed;
  ;; bottom: 0;
  ;; left: 0;
  ;; }

  [:.user-menu
   {:bottom "0",
    :float "left",
    :width "220px",
    :background "#3e313c",
    :cursor "pointer",
    :padding "7px 0 9px 8px",
    :position "fixed",
    :border-top "2px solid #372c36",
    :height "4rem",
    :left "0"}]

  [:.user-menu_profile-pic
   {:float "left",
    :width "48px",
    :background-image
    "url(data:image/png;base64,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)",
    :-moz-border-radius "0.2rem",
    :display "inline-block",
    :-webkit-border-radius "0.2rem",
    :margin-right "8px",
    :border-radius "0.2rem",
    :background-size "cover",
    :height "48px"}]

  [:.user-menu_username
   {:display "block",
    :color "#ffffff",
    :font-weight "900",
    :line-height "1.5rem",
    :margin-top ".2rem",
    :max-width "120px"}]

  [:.connection_icon {:width "12px", :height "12px"}]

  [:.connection_status {:color "#ab9ba9"}]

  [:.input-box {:height "100%", :margin-left "220px"}]

  [:.input-box_text
   {:line-height "1.2rem",
    :box-shadow "none",
    :color "#3d3c40",
    :min-height "41px",
    :bottom "0",
    :font-size ".95rem",
    :margin-left "2%",
    :width "90%",
    :-webkit-background-clip "padding-box",
    :-moz-box-shadow "none",
    :background-clip "padding-box",
    :-moz-border-radius "0.2rem",
    :-webkit-box-shadow "none",
    :-webkit-appearance "none",
    :padding "9px 5px 9px 8px",
    :-moz-background-clip "padding-box",
    :outline "0",
    :-webkit-border-radius "0.2rem",
    :border "2px solid #e0e0e0",
    :border-radius "0.2rem",
    :margin-bottom "auto"}]

  [:pre.highlight
   {:margin ".5rem 0 .2rem", :font-size ".75rem", :line-height "1.15rem"}]

  [:pre
   {:background "#fbfaf8",
    :padding ".5rem",
    :word-break "normal",
    :display "block",
    :border "1px solid rgba(0, 0, 0, .15)",
    :white-space "pre-wrap",
    :word-wrap "break-word",
    :border-radius "4px"}]

  [:pre
   :code
   {:font-family "Monaco, Menlo, Consolas, \"Courier New\", monospace",
    :color "#333"}]

  [:code
   {:line-height "1.2",
    :white-space "normal",
    :color "#c25",
    :background-color "#f7f7f9",
    :border "1px solid #e1e1e8"}]

  [:pre.highlight
   [">"
    [:code
     {:white-space "pre-wrap",
      :background-color "transparent",
      :border "none",
      :color "inherit"}]]]

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

  )

#_(defstyles style

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

    )
