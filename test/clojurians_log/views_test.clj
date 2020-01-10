(ns clojurians-log.views-test
  (:require [clojurians-log.views :as views :refer :all]
            [clojure.test :refer :all]
            [clojurians-log.test-helper :as h :refer [html-select html-select-1]]
            [clojure.string :as str]
            [ring.mock.request :as mock]))

(deftest page-head-test
  (testing "It renders the page title"
    (is (= [:title "Hello, world"]
           (html-select-1 (page-head {:data/title "Hello, world"}) [:head :title])))))
#_
(deftest routes-test
  (testing "All known routes should return http ok (200)"
    (let [system (h/test-system)

          _ (h/system-load-fixture! system "two-channels-two-days")

          ring-handler (h/system-ring-handler system)

          map->flat-seq (fn [m] (apply concat m))

          route-params {:channel "clojure"
                        :date "2018-02-02"
                        :ts "1517583973.000174"}

          ;; Generate a list of urls to try to retrieve
          urls (concat
                (for [route-name [:health-check :index]]
                  (bidi/path-for routes route-name))
                (for [route-name [:channel-history :log-old-url :log :log-target-message]]
                  (apply bidi/path-for routes route-name (map->flat-seq route-params))))]

      ;; Try to fetch every url and make sure we can get a proper response
      ;; TODO Some urls responds quite slowly (around 500ms)
      (doseq [url urls
              :let [response (ring-handler (mock/request :get url))]]
        (is (= 200 (:status response)) url)))))
#_
(deftest log-page-test
  (let [log-page (-> {:data/date "2018-01-02"
                      :data/channel {:channel/name "clojure"}
                      :data/channel-days [["2018-01-01" 5]
                                          ["2018-01-02" 3]
                                          ["2018-01-03" 4]]}
                     log-page
                     :response/html)]

    (testing "It links to the front page and to prev/next days"
      (is (= [[:a {:href "/"} "Clojurians"]
              [:a {:href "/clojure/2018-01-03"} [:div.day-prev "<"]]
              [:a {:href "/clojure/2018-01-01"} [:div.day-next ">"]]]
             (html-select log-page [:a])))))

  (let [system (h/test-system)

        _ (h/system-load-fixture! system "two-channels-two-days")

        ring-handler (h/system-ring-handler system)
        response (ring-handler
                  (mock/request :get
                                (bidi/path-for routes :log :channel "clojure" :date "2018-02-02")))
        log-hiccup (h/html->hiccup (:body response))

        ;; Selecting seems a 'little slow' on the order of several ms
        a-elements (html-select log-hiccup [:a])]

    (testing "All relative links should conform to known route url format"
      (doseq [link (->> a-elements
                        (map second)
                        (map :href)
                        (filter #(str/starts-with? % "/")))]
        (is (not (nil? (bidi/match-route routes link ))))))))
