(ns clojurians-log.message-parser-test
  (:require [clojurians-log.message-parser :refer [parse parse2] :as sut]
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
    (is (= [[:italic "hello"]]
           (parse "_hello_")))
    (is (= [[:emoji "thumbsup"]]
           (parse ":thumbsup:")))
    (is (= [[:undecorated "just_some_snake_case"]]
           (parse "just_some_snake_case"))))

  (testing "putting it together"
    (let [message "Hey <@U4F2A0Z8ER>: here is the `my-ns.core` code ```
  (let [code 42]
   (inc code))
```
*what do* _you_ *think* :mindblown:
please respond in <@C346HE24SD>"]
      (is (= [[:undecorated "Hey "]
              [:user-id "U4F2A0Z8ER"]
              [:undecorated ": here is the "]
              [:inline-code "my-ns.core"]
              [:undecorated " code "]
              [:code-block "(let [code 42]\n   (inc code))\n"]
              [:undecorated "\n"]
              [:bold "what do"]
              [:undecorated " "]
              [:italic "you"]
              [:undecorated " "]
              [:bold "think"]
              [:undecorated " "]
              [:emoji "mindblown"]
              [:undecorated "\nplease respond in "]
              [:channel-id "C346HE24SD"]]
             (parse message))))))


(deftest test-parse2
  (testing "basic messages"
    (is (= [[:undecorated "This is a normal message"]]
           (parse2 "This is a normal message")))
    (is (= [[:user-id "U4F2A0Z8ER"]]
           (parse2 "<@U4F2A0Z8ER>")))
    (is (= [[:channel-id "C4F2A26SGSHBW"]]
           (parse2 "<#C4F2A26SGSHBW>")))
    (is (= [[:channel-id "C03S1L9DN" "clojurescript"]]
           (parse2 "<#C03S1L9DN|clojurescript>")))
    (is (= [[:inline-code "DateTime"]]
           (parse2 "`DateTime`")))
    (is (= [[:code-block "(some clojure code)"]]
           (parse2 "```(some clojure code)```")))
    (is (= [[:bold "hey!"]]
           (parse2 "*hey!*")))
    (is (= [[:italic "hello"]]
           (parse2 "_hello_")))
    (is (= [[:emoji "thumbsup"]]
           (parse2 ":thumbsup:")))
    (is (= [[:bold "hi!"] [:undecorated " "] [:emoji "smiles"]]
           (parse2 "*hi!* :smiles:")))
    (is (= [[:undecorated "12:34:56:78:90:12:34:56:78:90:12:34:56:78:90:12"]]
           (parse2 "12:34:56:78:90:12:34:56:78:90:12:34:56:78:90:12")))
    (is (= [[:strike-through "strike-through"]]
           (parse2 "~strike-through~")))
    (is (= [[:undecorated "just_some_snake_case"]]
           (parse2 "just_some_snake_case")))
    (is (= [[:url "https://google.com"]]
           (parse2 "<https://google.com>")))
    (is (= [[:undecorated "from: "]
            [:url "https://google.com"]]
           (parse2 "from: <https://google.com>")))
    (is (= [[:undecorated "&<>"]]
           (parse2 "&amp;&lt;&gt;"))))

  (testing "nested regions"
    ;; Basic case
    (is (= [[:bold "hello"]]
           (parse2 "*hello*")))

    ;; two unrelated regions
    (is (= [[:italic "hello"] [:undecorated " "] [:bold "world"]]
           (parse2 "_hello_ *world*")))

    ;; single nested case
    (is (= [[:italic [:bold "hello"]]]
           (parse2 "_*hello*_")))

    ;; undecorated text outside regions
    (is (= [[:bold "hello"] [:undecorated " world again"]]
           (parse2 "*hello* world again")))

    (is (= [[:undecorated "hello "] [:bold "world"] [:undecorated " again"]]
           (parse2 "hello *world* again")))

    (is (= [[:undecorated "hello world "] [:bold "again"]]
           (parse2 "hello world *again*")))

    ;; undecorated text inside regions
    (is (= [[:italic [[:bold "hello"] [:undecorated " world"]]]]
           (parse2 "_*hello* world_")))

    (is (= [[:italic [[:undecorated "hello "] [:bold "world"]]]]
           (parse2 "_hello *world*_")))

    (is (= [[:italic [[:bold "hello"] [:undecorated " world again"]]]]
           (parse2 "_*hello* world again_")))

    (is (= [[:italic [[:undecorated "hello "] [:bold "world"] [:undecorated " again"]]]]
           (parse2 "_hello *world* again_")))

    (is (= [[:italic [[:undecorated "hello world "] [:bold "again"]]]]
           (parse2 "_hello world *again*_")))

    ;; Two nested regions
    (is (= [[:italic [[:bold "hello"] [:undecorated " "] [:strike-through "world"]]]]
           (parse2 "_*hello* ~world~_"))))

  (testing "No nested regions inside a code block"
    (is (= [[:undecorated "Some text "]
            [:code-block "some code <#C03S1L9DN|clojurescript>"]]
           (parse2 "Some text ```some code <#C03S1L9DN|clojurescript>```"))))

  (testing "putting it together"
    (let [message "Hey <@U4F2A0Z8ER>: here is the `my-ns.core` code ```
  (let [code 42]
   (inc code))
```
*what do* _you_ *think* :mindblown:
please respond in <#C346HE24SD>"]
      (is (= [[:undecorated "Hey "]
              [:user-id "U4F2A0Z8ER"]
              [:undecorated ": here is the "]
              [:inline-code "my-ns.core"]
              [:undecorated " code "]
              [:code-block "(let [code 42]\n   (inc code))\n"]
              [:undecorated "\n"]
              [:bold "what do"]
              [:undecorated " "]
              [:italic "you"]
              [:undecorated " "]
              [:bold "think"]
              [:undecorated " "]
              [:emoji "mindblown"]
              [:undecorated "\nplease respond in "]
              [:channel-id "C346HE24SD"]]
             (parse2 message))))))

;; Try out Slack message parsing at
;; https://api.slack.com/docs/messages/builder?msg=%7B%22text%22%3A%22xx1_%20*basic*%60%22%7D
(deftest parse-test
  (are [x y] (= y (parse2 x))
    "basic"              [[:undecorated "basic"]]
    "*bold*"             [[:bold "bold"]]
    "basic *bold* basic" [[:undecorated "basic "] [:bold "bold"] [:undecorated " basic"]]
    "basic *basic"       [[:undecorated "basic *basic"]]
    "_italic_"           [[:italic "italic"]]
    "xx _italic_ xx"     [[:undecorated "xx "] [:italic "italic"] [:undecorated " xx"]]
    "xx_oops_"           [[:undecorated "xx_oops_"]]
    "xx1*basic*"         [[:undecorated "xx1*basic*"]]
    "xx1`*basic*`"       [[:undecorated "xx1`*basic*`"]]
    "xx1_`*basic*`"      [[:undecorated "xx1_"] [:inline-code "*basic*"]]
    "xx1_ *basic*`"      [[:undecorated "xx1_ *basic*`"]]
    "> foo *_bar_*"      [[:blockquote [[:undecorated "foo "] [:bold [:italic "bar"]]]]]
    ">foo *_bar_*"       [[:blockquote [[:undecorated "foo "] [:bold [:italic "bar"]]]]]
    ">>foo *_bar_*"      [[:blockquote [[:undecorated ">foo "] [:bold [:italic "bar"]]]]]
    "> >foo *_bar_*"     [[:undecorated "> >foo "] [:bold [:italic "bar"]]]
    ">>>a\nmultiline\nquote" [[:blockquote "a\nmultiline\nquote"]]
    "<http://google.com|Google>" [[:url "http://google.com" "Google"]]
    "<https://clojure.org/reference/multimethods#_isa_based_dispatch>" [[:url "https://clojure.org/reference/multimethods#_isa_based_dispatch"]]
    ))
