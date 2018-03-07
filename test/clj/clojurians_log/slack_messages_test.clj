(ns clojurians-log.slack-messages-test
  (:require [clojurians-log.slack-messages :refer :all]
            [clojure.test :refer :all]))

(deftest test-extract-user-ids
  (is (= #{"ABC345" "ABC123"}
         (extract-user-ids
          [{:message/text "Hello <@ABC123>, how's <@ABC345|jonny> doing?"}]))))

(deftest test-render-hiccup
  (let [message "*Hey* <@U4F2A0Z8ER> how are things?"
        user-lookup {"U4F2A0Z8ER" "xandrews"}]
    (is (= [:p [[:b "Hey"] " "
                [:span.username "@" "xandrews"]
                " how are things?"]]
           (message->hiccup message user-lookup)))))

(deftest test-render-test
  (let [message "*Hey* <@U4F2A0Z8ER> how are things?"
        user-lookup {"U4F2A0Z8ER" "xandrews"}]
    (is (=  "*Hey* @xandrews how are things?"
            (message->text message user-lookup)))))
