(ns clojurians-log.xml2hiccup-test
  (:require [clojure.test :refer [deftest is are testing run-tests run-all-tests]]
            [clojurians-log.xml2hiccup :refer [build-tag xml2hiccup]]))

(deftest build-tag-test
  (are [tag attrs tag' attrs'] (= [tag' attrs'] (build-tag tag attrs))
    :div {}                                    :div          {}
    :div {:id "the-id"}                        :div#the-id   {}
    :div {:class "error"}                      :div.error    {}
    :div {:class "c1 c2   c3"}                 :div.c1.c2.c3 {}
    :div {:id "id" :class "ccc"}               :div#id.ccc   {}
    :div {:id "id" :class "c" :other-attr "a"} :div#id.c     {:other-attr "a"}))


(deftest xml2hiccup-test
  (are [in out] (= out (xml2hiccup in))
    {:tag :title, :attrs {}, :content '("Hello world")}
    [:title "Hello world"]

    "Some string"
    "Some string"

    {:tag :div,
     :attrs {:class "listings_channels"},
     :content '({:tag :h2,
                 :attrs {:class "listings_header listings_header_date"
                         :style "color: red;"},
                 :content ("2017-01-02")}
                {:tag :h2, :attrs {:class "listings_header"}, :content ("Channels")}
                {:tag :ul, :attrs {:class "channel_list"}, :content ()})}

    [:div.listings_channels
     [:h2.listings_header.listings_header_date {:style "color: red;"} "2017-01-02"]
     [:h2.listings_header "Channels"]
     [:ul.channel_list]]))
