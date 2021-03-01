(ns clojurians-log.slack-msg-test
  (:require [clojurians-log.db.queries :as queries]
            [time-literals.data-readers]
            [clojure.test :refer :all]))

(deftest filter-channel-day-messages-test
  (testing "thread messages should be removed"
    (is (= [] (queries/filter-channel-day-messages [{:message/ts        "1512384953.000000"
                                                     :message/thread-ts "1512384953.000000"}]))))
  (testing "thread parents should be retained"
    (is (= {:message/ts   "1014090027.000404"
            :message/inst #time/instant "2002-02-19T03:40:27.000404Z"}
           (first (queries/filter-channel-day-messages  [{:message/ts "1014090027.000404"}])))))
  
  (testing "broadcast messages should be retained"
    (is (= {:message/ts                "1518095027.000404"
            :message/thread-ts         "1518040988.000079"
            :message/thread-broadcast? true
            :message/top-level?        true
            :message/inst              #time/instant "2018-02-08T13:03:47.000404Z"}
           (first (queries/filter-channel-day-messages [{:message/ts                "1518095027.000404"
                                                         :message/thread-ts         "1518040988.000079"
                                                         :message/thread-broadcast? true}
                                                        {:message/ts        "1014090027.000404"
                                                         :message/thread-ts "1010040908.000079"}])))))
    
  (testing "should be sorted by timestamp"
      (is (= [{:message/ts   "1512389691.000900"
               :message/inst #time/instant "2017-12-04T12:14:51.000900Z"}
              {:message/ts   "1512394953.120000"
               :message/inst #time/instant "2017-12-04T13:42:33.120Z"}
              {:message/ts   "1518384553.060000"
               :message/inst #time/instant "2018-02-11T21:29:13.060Z"}
              {:message/ts   "1518989553.060000"
               :message/inst #time/instant "2018-02-18T21:32:33.060Z"}]
             (vec (queries/filter-channel-day-messages [{:message/ts "1518989553.060000"}
                                                        {:message/ts "1512394953.120000"}
                                                        {:message/ts "1512389691.000900"}
                                                        {:message/ts "1518384553.060000"}])))))
  
  (testing "should get the message/inst added to the messages"
    (is (= [{:message/ts                "1512394953.120000"
             :message/thread-ts         "1512389691.000900",
             :message/thread-broadcast? true,
             :message/top-level?        true,
             :message/inst              #time/instant "2017-12-04T13:42:33.120Z"}
            {:message/ts   "1518989553.060000"
             :message/inst #time/instant "2018-02-18T21:32:33.060Z"}] 
           (vec (queries/filter-channel-day-messages [{:message/ts "1518989553.060000"}
                                                      {:message/ts                "1512394953.120000"
                                                       :message/thread-ts         "1512389691.000900"
                                                       :message/thread-broadcast? true}])))))
  
  (testing "top-level messages should get marked as :message/top-level?"
    (is (= [{:message/ts                "1512389691.000900"
             :message/thread-ts         "1512388691.000900"
             :message/thread-broadcast? true
             :message/top-level?        true
             :message/inst              #time/instant "2017-12-04T12:14:51.000900Z"}
            {:message/ts   "1512394953.120000"
             :message/inst #time/instant "2017-12-04T13:42:33.120Z"} 
            {:message/ts                "1518989553.060000"
             :message/thread-broadcast? true
             :message/top-level?        true
             :message/inst              #time/instant "2018-02-18T21:32:33.060Z"}]
           (vec (queries/filter-channel-day-messages [{:message/ts                "1518989553.060000"
                                                       :message/thread-broadcast? true}
                                                      {:message/ts "1512394953.120000"}
                                                      {:message/ts                "1512389691.000900"
                                                       :message/thread-ts         "1512388691.000900"
                                                       :message/thread-broadcast? true}]))))))

(comment
  (require '[kaocha.repl :as k])
  (k/run)
  )
