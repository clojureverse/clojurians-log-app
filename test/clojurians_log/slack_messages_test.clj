(ns clojurians-log.slack-messages-test
  (:require [clojurians-log.slack-messages :as sm]
            [clojurians-log.message-parser :as mp]
            [clojure.test :refer :all]))

(deftest test-extract-user-ids
  (is (= #{"ABC345" "ABC123"}
         (sm/extract-user-ids
          [{:message/text "Hello <@ABC123>, how's <@ABC345|jonny> doing?"}]))))

(deftest test-nested-styled-segment
  (is (= (sm/segment->hiccup
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
        user-lookup {"U4F2A0Z8ER" "xandrews"}
        user-page-suffix "/_/_/users/U4F2A0Z8ER"]
    (is (= [:p [[:b "Hey"] " "
                [:span.username [:a {:href user-page-suffix} "@" "xandrews"]]
                " how are things?"]]
           (sm/message->hiccup message user-lookup)))
    (is (= [:p ["Thanks, I'm wonderful " [:span.emoji "😄"]]]
           (sm/message->hiccup reply user-lookup))))

  (testing "real-world regressions"
    ;; The main thing here is that there are no :undecorated tags left after
    ;; conversion to hiccup
    (is (= '([:b
              "Hey everyone, we’re so excited to be here for DevOps Enterprise Summit talking about"]
             "\n"
             [:span.emoji "arrow_right"]
             " "
             [:i
              [:b
               ("Be sure to visit our booth "
                [:a
                 {:href "https://doesvirtual.com/teamform"}
                 "https://doesvirtual.com/teamform"])]]
             " \n"
             [:span.emoji "tv"]
             " "
             [:i
              [:b
               ("Or join us anytime on Zoom - "
                [:a {:href "https://bit.ly/3iIdX1X"} "https://bit.ly/3iIdX1X"])]]
             "\n"
             [:span.emoji "mega"]
             " "
             [:i
              [:b
               ("Schedule a private demo - "
                [:a {:href "https://teamform.co/demo"} "https://teamform.co/demo"])]]
             "\n"
             [:span.emoji "gift"]
             " "
             [:i
              [:b
               ("Register for giveaway (1x PS5 or XBox Series X, 1 x 50min chat with the authors of Team Topologies, 20x IT Rev Books) "
                [:a
                 {:href "https://www.teamform.co/does-giveaway"}
                 "https://www.teamform.co/does-giveaway"])]]
             "\n\n"
             [:b "We’ve got a exciting week with a bunch of demos of TeamForm scheduled"]
             "\n"
             [:span.emoji "star"]
             " 11-11:15am PDT: TeamForm Live Demo: Managing Supply & Demand at Scale - join @ "
             [:a
              {:href "https://us02web.zoom.us/j/81956904920"}
              "https://us02web.zoom.us/j/81956904920"]
             "\n"
             [:span.emoji "star"]
             " 12:45-1:00pm PDT: TeamForm Live Demo: Measuring Team Organising Principles - join @ "
             [:a
              {:href "https://us02web.zoom.us/j/81956904920"}
              "https://us02web.zoom.us/j/81956904920"]
             "\n"
             [:span.emoji "bar_chart"]
             " 3:45-4pm PDT: TeamForm Live Demo: Measuring Team Proficiency - join @ "
             [:a
              {:href "https://us02web.zoom.us/j/81956904920"}
              "https://us02web.zoom.us/j/81956904920"]
             "\n\nLater this week:\n"
             [:span.emoji "arrow_right"]
             " Register for our AMA with Authors of TeamTopologies "
             [:a {:href "https://sched.co/ej42"} "https://sched.co/ej42"]
             " with "
             "ULTTZCP7S"
             " & "
             "UBE001UAX")
           (sm/segments->hiccup
            (mp/parse2 "*Hey everyone, we\u2019re so excited to be here for DevOps Enterprise Summit talking about*\n:arrow_right: _*Be sure to visit our booth <https://doesvirtual.com/teamform>*_ \n:tv: _*Or join us anytime on Zoom -\u00a0<https://bit.ly/3iIdX1X>*_\n:mega: _*Schedule a private demo - <https://teamform.co/demo>*_\n:gift: _*Register for giveaway (1x PS5 or XBox Series X, 1 x 50min chat with the authors of Team Topologies, 20x IT Rev Books) <https://www.teamform.co/does-giveaway>*_\n\n*We\u2019ve got a exciting week with a bunch of demos of TeamForm scheduled*\n:star: 11-11:15am PDT: TeamForm Live Demo: Managing Supply &amp; Demand at Scale - join @ <https://us02web.zoom.us/j/81956904920>\n:star: 12:45-1:00pm PDT: TeamForm Live Demo: Measuring Team Organising Principles - join @ <https://us02web.zoom.us/j/81956904920>\n:bar_chart: 3:45-4pm PDT: TeamForm Live Demo: Measuring Team Proficiency - join @ <https://us02web.zoom.us/j/81956904920>\n\nLater this week:\n:arrow_right: Register for our AMA with Authors of TeamTopologies <https://sched.co/ej42> with <@ULTTZCP7S> &amp; <@UBE001UAX>"))))
    )
  )

(deftest test-render-custom-emoji
  (let [message-1 "Oh no, :facepalm:"
        message-2 ":picard:"
        emoji-map {"facepalm" "https://emoji/facepalm.png"
                   "picard"   "alias:facepalm"}]
    (is (= [:p
            ["Oh no, "
             [:span.emoji
              [:img {:alt "facepalm" :src "https://emoji/facepalm.png"}]]]]
           (sm/message->hiccup message-1 {} emoji-map)))
    (is (= [:p
            [[:span.emoji
              [:img {:alt "picard" :src "https://emoji/facepalm.png"}]]]]
           (sm/message->hiccup message-2 {} emoji-map)))))

(deftest test-render-test
  (let [message     "*Hey* <@U4F2A0Z8ER> how are things?"
        user-lookup {"U4F2A0Z8ER" "xandrews"}]
    (is (=  "*Hey* @xandrews how are things?"
            (sm/message->text message user-lookup)))))
