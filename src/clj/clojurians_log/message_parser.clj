(ns clojurians-log.message-parser
  (:require [instaparse.core :as insta]
            [clojure.java.io :as io]
            [clojure.string :as str]))

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

(defn re-seq-pos [pattern string]
  (let [m (re-matcher pattern string)]
    ((fn step []
       (when (.find m)
         (cons (cond-> {:start (.start m) :end (.end m) :match (.group m)}
                 (> (.groupCount m) 0)
                 (assoc :matches
                        (re-groups m)))
               (lazy-seq (step))))))))

(def message-patterns
  {:code-block #"```(?s:(.*?))```"
   :inline-code #"`(.*?)`"
   :reference #"<((?:#C|@U)[A-Z0-9]{7,})(?:\|(.*?))?>"
   :emoji #":(.*?):"
   :italic #"\b_(.*?)_"
   :bold #"\*(.*?)\*"
   :strike-through #"\b~(.*?)~"})

(defn match-all-patterns [message-patterns message]
  (apply concat
         (for [[pattern-k pattern] message-patterns]
           (if-let [result (re-seq-pos pattern message)]
             (map #(assoc % :type pattern-k) result)))))

(defn- match-compare [m1 m2]
  ;; Order matches such that...
  (cond
    ;; Items with smallest start fields sorts to the top.
    (not= (:start m1) (:start m2))
    (- (:start m1) (:start m2))

    ;; If two blocks/patterns start at the same location then the larger
    ;; block/pattern takes precedence and sorts to the top.
    (not= (:end m1) (:end m2))
    (- (:end m2) (:end m1))

    ;; We shouldn't really reach here.
    :else
    (assert false "missing match comparison case")))

(defn- match->token [match]
  (condp = (:type match)
    :reference
    (let [[_ ref name] (:matches match)
          ref-type (condp = (second ref)
                       \U :user-id
                       \C :channel-id)]
      (cond-> [ref-type (subs ref 1)]
        (not (empty? name))
        (conj name)))

    :code-block
    [(:type match) (str/triml (nth (:matches match) 1))]

    [(:type match) (or (nth (:matches match) 1)
                       (:match match))]))

(defn parse2 [message]
  (let [matches (->> (match-all-patterns message-patterns message)
                     (sort match-compare))]

    ;; (clojure.pprint/pprint matches)
    ;; Loop starting from the beginning of the message string...
    (loop [cursor 0
           matches matches
           result []]

      (cond
        ;; If there are no more characters in the message to process, we're done.
        ;; Return the results accumulated so far
        (>= cursor (count message))
        result

        ;; There are still characters to be processed...
        ;; If there are no more matches in the rest of the message,
        ;; mark the rest of the message as undecorated.
        (empty? matches)
        (conj result [:undecorated (subs message cursor)])

        ;; If the starting location of the match is less than the current cursor,
        ;; this means we already came across a better/larger match previously.
        ;; We should move on to the next match and discard this one.
        (> cursor (:start (first matches)))
        (recur cursor (rest matches) result)

        :else
        (let [match (first matches)]
          (recur (:end match)
                 (rest matches)
                 (cond-> result
                   (> (:start match) cursor)
                   (conj [:undecorated (subs message cursor (:start match))])
                   :finally
                   (conj (match->token match)))))))))

(defn parse-with-pattern [pattern-k message]
  (let [pattern (get message-patterns pattern-k)]
    (re-seq-pos pattern message)))

(comment

  (let [data (clojurians-log.db.queries/channel-day-messages (user/db) "clojure" "2018-02-02")]
    (time (->> data
               (map #(parse (:message/text %)))
               doall))
    nil)

  (let [data (clojurians-log.db.queries/channel-day-messages (user/db) "clojure" "2018-02-02")]
    (time (->> data
               (map #(parse2 (:message/text %)))
               doall))
    nil)

)
