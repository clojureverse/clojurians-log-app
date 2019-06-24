(ns clojurians-log.db.import-test
  (:require [clojure.test :refer :all]
            [clojurians-log.db.import :as import]))

(deftest event->tx-test
  (testing "returns valid datomic transaction data"
    (is (= #:message{:key     "C0617A8PQ--1517728899.000025"
                     :ts      "1517728899.000025"
                     :day     "2018-02-04"
                     :text    ":smile:"
                     :channel [:channel/slack-id "C0617A8PQ"]
                     :user    [:user/slack-id "U051BLM8F"]}
           (import/event->tx {:source_team "T03RZGPFR"
                              :text        ":smile:"
                              :ts          "1517728899.000025"
                              :user        "U051BLM8F"
                              :team        "T03RZGPFR"
                              :type        "message"
                              :channel     "C0617A8PQ"}))))

  (testing "parses thread info"
    (is (= {:message/thread-ts   "1517483637.000077"
            :message/thread-inst #inst "2018-02-01T11:13:57.000-00:00"}
           (-> (import/event->tx {:source_team     "T03RZGPFR"
                                  :reply_broadcast true
                                  :channel         "C03S1L9DN"
                                  :type            "message"
                                  :thread_ts       "1517483637.000077"
                                  :ts              "1517487432.000382"
                                  :team            "T03RZGPFR"
                                  :user            "U7THYHQ1F"
                                  :text            "Wow, thanks!"})
               (select-keys [:message/thread-ts :message/thread-inst])))))

  (testing "puts threaded messages on the day of the thread"
    (is (= "2018-02-06"
           (:message/day (import/event->tx {:thread_ts   "1517932488.000769"
                                            :source_team "T03RZGPFR"
                                            :text        "posted on 2018-02-08, replying to a message from 2018-02-06"
                                            :ts          "1518050759.000174"
                                            :user        "U8MJBRSR5"
                                            :team        "T03RZGPFR"
                                            :type        "message"
                                            :channel     "C03RZRRMP"}))))))
