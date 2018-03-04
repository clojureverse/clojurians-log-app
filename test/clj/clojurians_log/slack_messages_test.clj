(ns clojurians-log.slack-messages-test
  (:require [clojurians-log.slack-messages :refer (render-hiccup)]
            [clojure.test :refer :all]))

(deftest test-render-hiccup
  (let [message "*Hey* <@U4F2A0Z8ER> how are things?"
        user-lookup {"U4F2A0Z8ER" "xandrews"}]
    (is (= [:p [[:b "Hey"] " "
                [:span.username "@" "xandrews"]
                " how are things?"]]
           (render-hiccup message user-lookup)))))
