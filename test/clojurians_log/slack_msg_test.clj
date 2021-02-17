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
                   :inst (time-util/ts->inst "1014090027.000404")}
        (first(queries/filter-channel-day-messages  [{:message/ts "1014090027.000404"}])))))
  (testing "broadcast messages should be retained"
    (is (=
         #:message{:ts                "1518095027.000404"
                   :thread-ts         "1518040988.000079"
                   :thread-broadcast? true
                   :top-level?        true
                   :inst              (time-util/ts->inst "1518095027.000404")}
         (first (queries/filter-channel-day-messages [{:message/ts                "1518095027.000404"
                                                       :message/thread-ts         "1518040988.000079"
                                                       :message/thread-broadcast? true}
                                                      {:message/ts        "1014090027.000404"
                                                       :message/thread-ts "1010040908.000079"}])))))
  (testing "should be sorted by timestamp"
    (is (=(type
           {#:messages{:ts   "1512389691.000900"
                       :inst (time-util/ts->inst "1512389691.000900")}
            #:messages{:ts   "1512394953.120000"
                       :inst (time-util/ts->inst "1512394953.120000")}
            #:messages{:ts   "1518384553.060000"
                       :inst (time-util/ts->inst "1518384553.060000")}
            #:messages{:ts   "1518989553.060000"
                       :inst (time-util/ts->inst "1518989553.060000")}})
         (type (queries/filter-channel-day-messages [{:message/ts "1518989553.060000"}
                                                     {:message/ts "1512394953.120000"}
                                                     {:message/ts "1512389691.000900"}
                                                     {:message/ts "1518384553.060000"}]))))))
(testing "should get the message/inst added to the messages"
  (is (= (type {#:messages {:ts   "1014090027.000404"
                            :inst (time-util/ts->inst "1014090027.000404")}
                #:messages {:ts                "1518384553.060000"
                            :thread-broadcast? true
                            :top-level         true
                            :inst               (time-util/ts->inst "1518384553.060000")}})
          (type (queries/filter-channel-day-messages  [{:message/ts "1014090027.000404"}
                                                       {:message/ts                "1218384553.060000"
                                                        :message/thread-broadcast? true}])))))
(testing "top-level messages should get marked as :message/top-level?"
  (is (=
       #:message{:ts                "1014090027.000404"
                 :thread-broadcast? true
                 :top-level?        true
                 :inst            (time-util/ts->inst "1014090027.000404")}
       (first (queries/filter-channel-day-messages  [{:message/ts                "1014090027.000404"
                                                      :message/thread-broadcast? true}])))))