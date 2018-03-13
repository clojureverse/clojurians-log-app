(ns clojurians-log.db.queries-test
  (:require [clojurians-log.db.queries :refer :all]
            [clojure.test :refer :all]
            [clojurians-log.test-helper :refer [test-db]]
            [datomic.api :as d]
            [clojurians-log.time-util :as tu]
            [java-time :as jt]))

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

(deftest threaded-messages-test
  (let [[conn db] (test-db "threaded-messages")
        target-day "2018-02-07"
        thread-messages (channel-thread-messages-of-day db "datomic" target-day)
        target-interval (tu/day-str->date-interval target-day)]

    (testing "all returned messages should belong to a thread"
      (is (every? #(contains? % :message/thread-ts) thread-messages)))

    (testing "no thread parents should be present"
      (is (every? #(not= (:message/thread-ts %) (:message/ts %)) thread-messages)))

    (testing "all threads start on the targeted date"
      (is (every? #(tu/within-interval (-> (:message/thread-ts %)
                                           tu/ts->inst
                                           jt/to-java-date)
                                       target-interval)
                  thread-messages)))))
