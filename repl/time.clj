(ns repl.time
  (:require [java-time :as jt]

            [clojure.string :as str]))


(def the-inst (let [[seconds micros] (map #(Double/parseDouble %)
                                          (str/split "1433399521.000490" #"\."))
                    inst (java.time.Instant/ofEpochSecond seconds (* 1e3 micros))]
                inst))

(str the-inst)                                 ;;=>


;;=> #object[java.time.Instant 0x2e8fd63a "2015-06-04T06:32:01.000489950Z"]

(jt/format
 (jt/with-zone (jt/formatter "'inst-'yyyy-MM-dd'T'HH:MM:ss.SSSSSS'Z'") (jt/zone-id "UTC"))
 the-inst
 )

inst-2015-09-03T22:50:26.000005Z


hidden

(jt/format

 (jt/with-zone (jt/formatter "'inst-'yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'") (jt/zone-id "UTC"))
 (jt/plus (clojurians-log.time-util/ts->inst "1502485535.467008")
          (jt/hours 3))
 ;;=> #object[java.time.Instant 0x7c6a79c1 "2017-08-11T21:05:35.467008Z"]
 )
;;=> "inst-2017-08-12T00:05:35.467008Z"
;;=> "inst-2017-08-11T21:05:35.467008Z"
;;=> "inst-2017-08-11T23:05:35.467008Z"


{:source_team "T03RZGPFR",
 :text "yep, I just updated figwheel to 0.5.12 and still no joy",
 :ts "1502485535.467008",
 :user
 {:id "U625N87MF",
  :name "tiagoantao",
  :real_name "Tiago Antao",
  :is_admin false,
  :is_owner false,
  :profile
  {:image_512
   "https://secure.gravatar.com/avatar/9d97b401cb8272790d9ef0ebf27d0935.jpg?s=512&d=https%3A%2F%2Fa.slack-edge.com%2F7fa9%2Fimg%2Favatars%2Fava_0009-512.png",
   :email "tiagoantao@gmail.com",
   :first_name "Tiago",
   :real_name_normalized "Tiago Antao",
   :image_48
   "https://secure.gravatar.com/avatar/9d97b401cb8272790d9ef0ebf27d0935.jpg?s=48&d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0009-48.png",
   :image_192
   "https://secure.gravatar.com/avatar/9d97b401cb8272790d9ef0ebf27d0935.jpg?s=192&d=https%3A%2F%2Fa.slack-edge.com%2F7fa9%2Fimg%2Favatars%2Fava_0009-192.png",
   :real_name "Tiago Antao",
   :image_72
   "https://secure.gravatar.com/avatar/9d97b401cb8272790d9ef0ebf27d0935.jpg?s=72&d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0009-72.png",
   :image_24
   "https://secure.gravatar.com/avatar/9d97b401cb8272790d9ef0ebf27d0935.jpg?s=24&d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0009-24.png",
   :avatar_hash "g9d97b401cb8",
   :title "",
   :team "T03RZGPFR",
   :image_32
   "https://secure.gravatar.com/avatar/9d97b401cb8272790d9ef0ebf27d0935.jpg?s=32&d=https%3A%2F%2Fa.slack-edge.com%2F66f9%2Fimg%2Favatars%2Fava_0009-32.png",
   :last_name "Antao",
   :display_name "tiagoantao",
   :display_name_normalized "tiagoantao"}},
 :team "T03RZGPFR",
 :type "message",
 :channel "C03S1L9DN",
 :inst
 #object[java.time.Instant 0x356e94b3 "2017-08-11T21:05:35.467008Z"]}
