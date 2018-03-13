(ns clojurians-log.db.queries-test
  (:require [clojurians-log.db.queries :refer :all]
            [clojure.test :refer :all]
            [clojurians-log.test-helper :refer [test-db]]
            [datomic.api :as d]))

(deftest channel-list-test
  (testing "without arg - return all channels"
    (let [[conn db] (test-db)]
      (is (= (channel-list db)
             [#:channel{:slack-id "C03S1KBA2", :name "clojure"}
              #:channel{:slack-id "C03S1L9DN", :name "clojurescript"}]))))

  (testing "returns only channels with messages on the given day, includes message-count"
    (let [[conn db] (test-db "quiet-channels")]
      (is (= (channel-list db "2018-02-03")
             [#:channel{:slack-id "C7YF1SBT3", :name "reitit", :message-count 3}
              #:channel{:slack-id "C05006WDW", :name "jobs", :message-count 1}])))))
