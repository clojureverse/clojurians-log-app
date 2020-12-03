(ns repl-sessions.stats-from-indexer
  (:require [clojurians-log.time-util :as time-util])
  (:import (java.time LocalDate)))

;; Fetching all messages for a given channel and day is really fast, but we also
;; want to know the list of all channels for a given day, and how many messages
;; each had, and which day was the previous or next day that had messages, and
;; these queries are really slow, since they basically need to traverse all
;; messages.

;; To work around that we build up these "indexes" and keep them in an atom. We
;; have an "indexer" component that rebuilds them regularly.

(keys @clojurians-log.db.queries/!indexes)
;; => (:chan-day-cnt :day-chan-cnt :chan-id->name :chan-name->id)

(defn day-chan-cnt []
  (:day-chan-cnt @clojurians-log.db.queries/!indexes))

;; For example day-chan-cnt groups first on day, then on channel, and then shows
;; the count of messages.

(day-chan-cnt)
;;=>
{"2018-01-28" {"C03S1KBA2" 1},
 "2018-02-02" {"C064BA6G2" 28,
               "C099W16KZ" 19,
               "C0617A8PQ" 51,
               ,,,}}

;; So we can easily sum up all messages for a given day.

(defn day-total [day]
  (apply + (vals (get (day-chan-cnt) day))))

(day-total "2018-02-02")
;; => 887

;; If we want to query this we just need to extrapolate to a range of days. We
;; use clojure.java-time elsewhere but actually the java.time API is quite nice
;; and nowadays I tend to use it directly. A simple date (so year+month+day)
;; without any time or timezone information is represented as a
;; java.time.LocalDate.

;; Good example here of recursion and lazy-seq. Note that the lazy-seq is
;; optional here, you can remove it and still get a valid result, it would just
;; be eager instead of lazy.

;; There are obviously more ways to write this, for instance with loop/recur.
;; This use of recursion + cons is a very "classic lisp" approach.

(defn range-of-local-dates [^LocalDate ld1 ^LocalDate ld2]
  (when (.isBefore ld1 ld2)
    (cons ld1 (lazy-seq (range-of-local-dates (.plusDays ld1 1) ld2)))))

;; A bit more clojure-y, use vectors with [year month day], return strings
;; like "2018-02-02", since that is what we have in the indexes. Note that just
;; calling `str` on any java.time class usually gives a nicely formatted result.

(defn range-of-days [[y1 m1 d1] [y2 m2 d2]]
  (map str
       (range-of-local-dates
        (java.time.LocalDate/of y1 m1 d1)
        (java.time.LocalDate/of y2 m2 d2))))

;; So this is what that looks like now. I made the range half-open (not
;; including the end date), might make more sense to make it inclusive.

(range-of-days [2018 2 2] [2018 2 5])
;; => ("2018-02-02" "2018-02-03" "2018-02-04")

;; So now we can grab the numbers for these days and sum them up. This is a
;; textbook example of where a transducer works nicely. Check the LI episode for
;; transducers if you haven't seen this before. Obviously there are other ways
;; to write this too, like a simple (apply + (map ...))

(defn days-total [days]
  (transduce (map day-total) + 0 days))

;; And there you go

(days-total
 (range-of-days [2018 2 2] [2018 2 5]))
;; => 1913
