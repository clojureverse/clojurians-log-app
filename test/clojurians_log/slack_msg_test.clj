(ns clojurians-log.slack-mgs-test
  (:require [clojurians-log.db.queries :as queries]
            [clojurians-log.time-util :as time-util]
            [clojure.test :refer :all]))

(deftest filter-channel-day-messages-test
  (testing "thread messages should be removed"
    (is (= [] (queries/filter-channel-day-messages [{:message/ts        "1512384953.000000"
                                                     :message/thread-ts "1512384953.000000"}]))))
  (testing "thread parents should be retained"
    (is (= 
         {:ts   "1014090027.000404"
          :inst (.toInstant #inst "2002-02-19T03:40:27.000404Z")}         
         (first (queries/filter-channel-day-messages  [{:message/ts "1014090027.000404"}]))))) 
  (testing "broadcast messages should be retained"
    (is(=
         {:ts                "1518095027.000404"
          :thread-ts         "1518040988.000079"
          :thread-broadcast? true
          :top-level?        true
          :inst              (.toInstant #inst "2018-02-08T13:03:47.000404Z")
          }
         (first (queries/filter-channel-day-messages [{:message/ts                "1518095027.000404"
                                                       :message/thread-ts         "1518040988.000079"
                                                       :message/thread-broadcast? true}
                                                      {:message/ts        "1014090027.000404"
                                                       :message/thread-ts "1010040908.000079"}])))))
  (testing "should be sorted by timestamp"
    (is (= [] (queries/filter-channel-day-messages [])))   
    
    )
  (testing "should get the message/inst added to the messages"
       (is (= {:ts   "1014090027.000404"
               :inst (.toInstant #inst "2002-02-19T03:40:27.000404Z")}         
         (first (queries/filter-channel-day-messages  [{:message/ts "1014090027.000404"}]))))
    )  
  (testing "top-level messages should get marked as :message/top-level?"
    (is (=
         {:ts                "1014090027.000404",
          :thread-broadcast? true,
          :top-level?        true,
          :inst              (.toInstant #inst "2002-02-19T03:40:27.000404Z")}
         (queries/filter-channel-day-messages  [{:message/ts                "1014090027.000404"
                                         :message/thread-broadcast? true}]))))
  )