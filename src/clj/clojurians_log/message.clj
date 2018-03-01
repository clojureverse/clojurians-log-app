(ns clojurians-log.message
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]))

(def parser
  (insta/parser
   (io/resource "clojurians-log/slack-message.bnf")))

(defn join-adjacent
  "Takes output from an insta-parse parser and returns a new
  vector of tokens with repeated types merged.
  eg. (join-adjacent [[:undecorated \"Hello\"] [:undecorated \" \"] [:undecorated \"World\"]])
  yields: [[:undecorated \"Hello World\"]]"
  [tokens]
  (reduce (fn [result [type content :as token]]
            (let [[prev-type prev-content :as prev-token] (last result)]
              (if (and type
                       (= :undecorated prev-type)
                       (= :undecorated type))
                (assoc result (dec (count result)) [:undecorated (str prev-content content)])
                (conj result token))))
          [] tokens))

(defn parse
  "Returns a vector of [type string] pairs, where type identifies one of the special markup types available in slack.
  eg. (parse \"Hello *bold*!\")
  yields: [[:undecorated \"Hello \"] [:bold \"bold\"] [:undecorated \"!\"]]"
  [message]
  (join-adjacent (parser message)))
