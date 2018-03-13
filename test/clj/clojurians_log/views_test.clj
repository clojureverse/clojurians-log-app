(ns clojurians-log.views-test
  (:require [clojurians-log.views :as views :refer :all]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [clojurians-log.test-helper :refer [html-select html-select-1]]))

(deftest page-head-test
  (testing "It renders the page title"
    (is (= (html-select-1 (page-head {:data/title "Hello, world"}) [:head :title])
           [:title "Hello, world"]))))

(deftest log-page-test
  (let [log-page (-> {:data/date "2018-01-02"
                      :data/channel {:channel/name "clojure"}
                      :data/channel-days [["2018-01-01" 5]
                                          ["2018-01-02" 3]
                                          ["2018-01-03" 4]]}
                     log-page
                     :response/html)]

    (testing "It links to the front page and to prev/next days"
      (is (= (html-select log-page [:a])
             [[:a {:href "/"} "Clojurians"]
              [:a {:href "/clojure/2018-01-01.html"} [:div.day-prev "<"]]
              [:a {:href "/clojure/2018-01-03.html"} [:div.day-next ">"]]])))))
