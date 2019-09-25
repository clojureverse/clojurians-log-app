(ns clojurians-log.slack-messages-test
  (:require [clojurians-log.slack-messages :refer :all]
            [clojure.test :refer :all]))

(deftest test-extract-user-ids
  (is (= #{"ABC345" "ABC123"}
         (extract-user-ids
          [{:message/text "Hello <@ABC123>, how's <@ABC345|jonny> doing?"}]))))

(deftest test-nested-styled-segment
  (is (= (clojurians-log.slack-messages/segment->hiccup
          [:strike-through
           [[:undecorated "you can use "]
            [:bold "::stest/opts"]
            [:undecorated " and it’ll work"]]])

         [:del
          ["you can use "
           [:b "::stest/opts"]
           " and it’ll work"]])))

(deftest test-render-hiccup
  (let [message     "*Hey* <@U4F2A0Z8ER> how are things?"
        reply       "Thanks, I'm wonderful :smile:"
        user-lookup {"U4F2A0Z8ER" "xandrews"}]
    (is (= [:p [[:b "Hey"] " "
                [:span.username "@" "xandrews"]
                " how are things?"]]
           (message->hiccup message user-lookup)))
    (is (= [:p ["Thanks, I'm wonderful " [:span.emoji "😄"]]]
           (message->hiccup reply user-lookup)))))

(deftest test-render-custom-emoji
  (let [message-1 "Oh no, :facepalm:"
        message-2 ":picard:"
        emoji-map {"facepalm" "https://emoji/facepalm.png"
                   "picard"   "alias:facepalm"}]
    (is (= [:p
            ["Oh no, "
             [:span.emoji
              [:img {:alt "facepalm" :src "https://emoji/facepalm.png"}]]]]
           (message->hiccup message-1 {} emoji-map)))
    (is (= [:p
            [[:span.emoji
              [:img {:alt "picard" :src "https://emoji/facepalm.png"}]]]]
           (message->hiccup message-2 {} emoji-map)))))

(deftest test-render-test
  (let [message     "*Hey* <@U4F2A0Z8ER> how are things?"
        user-lookup {"U4F2A0Z8ER" "xandrews"}]
    (is (=  "*Hey* @xandrews how are things?"
            (message->text message user-lookup)))))
