(ns clojurians-log.repl-test
  (:require [clojurians-log.repl :as repl]
            [clojurians-log.slack-api :as slack]
            [clojure.test :refer :all]))


(deftest load-slack-data!-test
  (testing "it handles renames"
    (with-redefs [slack/users (constantly [])
                  slack/channels (constantly {:id "C03RZGPG1"
                                              :name "announcements"
                                              :creator "U03RZGPFT"
                                              :created 1425231209})]
      (repl/load-slack-data!)
      )

    ))
