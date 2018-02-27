(ns clojurians-log.message-test
  (:require [clojurians-log.message :refer [parse] :as sut]
            [clojure.test :refer :all]))

(deftest test-parse
  (testing "basic messages"
    (is (= [[:undecorated "This is a normal message"]]
           (parse "This is a normal message")))
    (is (= [[:user-id "U4F2A0Z8ER"]]
           (parse "<@U4F2A0Z8ER>")))
    (is (= [[:channel-id "C4F2A26SGSHBW"]]
           (parse "<@C4F2A26SGSHBW>")))
    (is (= [[:inline-code "DateTime"]]
           (parse "`DateTime`")))
    (is (= [[:code-block "(some clojure code)"]]
           (parse "```(some clojure code)```")))
    (is (= [[:bold "hey!"]]
           (parse "*hey!*")))
    (is (= [[:strong "hello"]]
           (parse "_hello_")))
    (is (= [[:emoji "thumbsup"]]
           (parse ":thumbsup:")))
    (is (= [[:undecorated "just_some_snake_case"]]
           (parse "just_some_snake_case"))))

  (testing "putting it together"
    (let [message "Hey <@U4F2A0Z8ER> here is the `my-ns.core` code ```
  (let [code 42]
   (inc code))
```
*what do* _you_ *think* :mindblown:
please respond in <@C346HE24SD>"]
      (is (= [[:undecorated "Hey "]
              [:user-id "U4F2A0Z8ER"]
              [:undecorated " here is the "]
              [:inline-code "my-ns.core"]
              [:undecorated " code "]
              [:code-block "\n  (let [code 42]\n   (inc code))\n"]
              [:undecorated "\n"]
              [:bold "what do"]
              [:undecorated " "]
              [:strong "you"]
              [:undecorated " "]
              [:bold "think"]
              [:undecorated " "]
              [:emoji "mindblown"]
              [:undecorated "\nplease respond in "]
              [:channel-id "C346HE24SD"]]
             (parse message))))))
