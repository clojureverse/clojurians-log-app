(ns clojurians-log.views-test
  (:require [clojurians-log.views :as views :refer :all]
            [clojure.test :refer :all]
            [net.cgrand.enlive-html :as enlive]
            [clojurians-log.test-helper :refer [html-select html-select-1]]
            [clojure.string :as str]
            [clojurians-log.routes-def :refer [routes]]
            [bidi.bidi :as bidi]))

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
              [:a {:href "/clojure/2018-01-01"} [:div.day-prev "<"]]
              [:a {:href "/clojure/2018-01-03"} [:div.day-next ">"]]])))

    (testing "All relative links should conform to known route url format"
      (doseq [link (->> (html-select log-page [:a])
                        (map second)
                        (map :href)
                        (filter #(str/starts-with? % "/")))]
        (is (not (nil? (bidi/match-route routes link ))))))))
