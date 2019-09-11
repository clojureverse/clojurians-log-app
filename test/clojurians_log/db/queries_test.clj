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
      (is (= [#:channel{:slack-id "C03S1KBA2", :name "clojure"}
              #:channel{:slack-id "C03S1L9DN", :name "clojurescript"}]
             (channel-list db)))))

  (testing "returns only channels with messages on the given day, includes message-count"
    (let [[conn db] (test-db "quiet-channels")]
      (is (= [#:channel{:slack-id "C05006WDW", :name "jobs", :message-count 1}
              #:channel{:slack-id "C7YF1SBT3", :name "reitit", :message-count 3}]
             (channel-list db "2018-02-03"))))))

(deftest threaded-messages-test
  (let [[conn db] (test-db "threaded-messages")
        target-day "2018-02-07"
        thread-messages (channel-thread-messages-of-day db "datomic" target-day)
        thread-messages-ts(->> thread-messages
                               (map :message/ts)
                               (into #{}))
        parents-ts (->> thread-messages
                        (map :message/thread-ts)
                        (into #{}))
        target-interval (tu/day-str->date-interval target-day)]

    (testing "all returned messages should belong to a thread"
      (is (every? #(contains? % :message/thread-ts) thread-messages)))

    (testing "no thread parents should be present"
      (let [parents-ts (->> thread-messages
                            (map :message/thread-ts)
                            (into #{}))]
        (is (every? #(not (contains? parents-ts (:message/ts %))) thread-messages))))

    (testing "all threads start on the targeted date"
      (is (every? #(tu/within-interval (-> (:message/thread-ts %)
                                           tu/ts->inst
                                           jt/to-java-date)
                                       target-interval)
                  thread-messages)))

    ;; The following tests should hold given the contents of threaded-messages.edn
    ;; To see how the data was generated, see repl/test_data.clj.
    (testing "returns messages for the expected threads"
      (is (= #{"1517995093.000487" "1518040988.000079"}
             parents-ts))

      ;; Does not contain message for threads not starting on the target date
      (is (not (contains? parents-ts "1517924158.000577"))))

    (testing "returns the expected messages"
      (is (= #{"1518008583.000370" "1518058291.000073" "1518095027.000404" "1518095379.000012"}
             thread-messages-ts))

      ;; Should exclude non-threaded message from the same day
      (is (not (contains? thread-messages-ts "1518034517.000637")))

      ;; Should exclude non-threaded message from another day
      (is (not (contains? thread-messages-ts "1518050129.000168")))

      ;; Should exclude thread message that started on another day
      (is (not (contains? thread-messages-ts "1517924158.000577"))))))
