(ns clojurians-log.slack-mgs-test
  (:require [clojurians-log.db.queries :as queries]
            [clojurians-log.time-util :as time-util]
            [clojure.test :refer :all]))

(deftest filter-channel-day-messages-test
  (testing "thread messages should be removed"
    (is (= [] (queries/filter-channel-day-messages [{:message/ts        "1512384953.000000"
                                                     :message/thread-ts "1512384953.000000"}]))))
  (testing "thread parents should be retained"
    (is (= #:message{:ts   "1014090027.000404"
                   :inst (.toInstant (clojure.instant/read-instant-timestamp "2002-02-19T03:40:27.000404Z"))}
        (first(queries/filter-channel-day-messages  [{:message/ts "1014090027.000404"}])))))
  (testing "broadcast messages should be retained"
    (is (=
         #:message{:ts                "1518095027.000404"
                   :thread-ts         "1518040988.000079"
                   :thread-broadcast? true
                   :top-level?        true
                   :inst              (.toInstant (clojure.instant/read-instant-timestamp "2018-02-08T13:03:47.000404Z"))}
         (first (queries/filter-channel-day-messages [{:message/ts                "1518095027.000404"
                                                       :message/thread-ts         "1518040988.000079"
                                                       :message/thread-broadcast? true}
                                                      {:message/ts        "1014090027.000404"
                                                       :message/thread-ts "1010040908.000079"}])))))
  (testing "should be sorted by timestamp"
    (is (=

         {#:messages{:ts   "1512389691.000900"
                     :inst (.toInstant (clojure.instant/read-instant-timestamp "2017-12-04T12:14:51.000900Z"))}
          #:messages{:ts   "1512394953.120000"
                     :inst (.toInstant (clojure.instant/read-instant-timestamp "2017-12-04T13:42:33.120Z"))}
          #:messages{:ts   "1518384553.060000"
                     :inst (.toInstant (clojure.instant/read-instant-timestamp "2018-02-11T21:29:13.060Z"))}
          #:messages{:ts   "1518989553.060000"
                     :inst (.toInstant (clojure.instant/read-instant-timestamp "2018-02-18T21:32:33.060Z"))}}
         (queries/filter-channel-day-messages [{:message/ts "1518989553.060000"}
                                               {:message/ts "1512394953.120000"}
                                               {:message/ts "1512389691.000900"}
                                               {:message/ts "1518384553.060000"}])))))
(testing "should get the message/inst added to the messages"
  (is (= {#:messages {:ts   "1014090027.000404"
                             :inst (.toInstant (clojure.instant/read-instant-timestamp "2002-02-19T03:40:27.000404Z"))}
                 #:messages {:ts                "1518384553.060000"

                             :thread-broadcast? true
                             :top-level         true
                             :inst              (.toInstant (clojure.instant/read-instant-timestamp "2018-02-11T21:29:13.060Z"))}}
          (queries/filter-channel-day-messages  [{:message/ts "1014090027.000404"}
                                                 {:message/ts                "1218384553.060000"
                                                  :message/thread-broadcast? true}]))))
(testing "top-level messages should get marked as :message/top-level?"
  (is (=
       #:message{:ts                "1014090027.000404"
                 :thread-broadcast? true
                 :top-level?        true
                 :inst              (.toInstant (clojure.instant/read-instant-timestamp "2002-02-19T03:40:27.000404Z"))}
       (first (queries/filter-channel-day-messages  [{:message/ts                "1014090027.000404"
                                                      :message/thread-broadcast? true}])))))