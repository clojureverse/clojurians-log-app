(ns clojurians-log.time-util
  (:require [java-time :as jt]
            [java-time.local :as jt.l]
            [clojure.string :as str])
  (:import [java.time Instant]
           [java.time.format DateTimeFormatter]))

(defn ts->inst
  "Convert a Slack timestamp like \"1433399521.000490\" into a java.time.Instant like
  #inst \"2015-06-04T06:32:01.000490Z\""
  [ts]
  (let [[seconds micros] (map #(Double/parseDouble %)
                              (str/split ts #"\."))
        inst (Instant/ofEpochSecond seconds (* 1e3 micros))]
    inst))

(def UTC (jt/zone-id "UTC"))
(def inst-id-formatter (jt/with-zone (jt/formatter "'inst-'yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'") UTC))
(def inst-time-formatter (jt/with-zone (jt/formatter "HH:MM:ss") UTC))
(def inst-day-formatter (jt/with-zone (jt/formatter "yyyy-MM-dd") UTC))

(defn format-inst-id
  "Format an Instant into an id used to link to individual messages. Fun fact: the
  old version of the site did this wrong. It seems it offset all times by three
  hours, so a message posted at 3pm would show up as posted as 6pm. Don't change
  this as it will break existing links."
  [inst]
  (jt/format inst-id-formatter
             (jt/plus inst (jt/hours 3))))

(defn inst-id->inst
  [inst-id-str]
  (as-> inst-id-str $
    (jt/instant inst-id-formatter $)
    (jt/minus $ (jt/hours 3))))

(defn format-inst-time
  "Format an Instant to a simple hour:minute:second time to be displayed in the
  view."
  [inst]
  (jt/format inst-time-formatter inst))

(defn format-inst-day
  "Format an instant as year-month-day, e.g. 2017-11-20."
  [inst]
  (jt/format inst-day-formatter inst))

(defn time->html-ts
  [zoned-dt]
  (jt/format DateTimeFormatter/RFC_1123_DATE_TIME zoned-dt))

(defn html-ts->time
  [ts]
  (jt/zoned-date-time DateTimeFormatter/RFC_1123_DATE_TIME ts))

(defn day-interval [y m d]
  (let [start (jt/with-zone
                (jt/zoned-date-time y m d)
                UTC)
        end (jt/plus start (jt/days 1))]
    [start end]))

(defn day-str->date-interval
  "Returns [from-date to-date] that brackets the given `day`.
  By using this range, we can check if another date is within this day."
  [day-str]
  (let [date (jt.l/local-date inst-day-formatter day-str)]
    (->> (day-interval (.getYear date)
                       (.getMonth date)
                       (.getDayOfMonth date))
         (map #(java.util.Date/from (.toInstant %))))))

(defn within-interval [date [interval-start interval-end]]
  (and (.after date interval-start)
       (.before date interval-end)))

(defmacro time-with-label
  "Evaluates expr, prints the execution time with the supplied `label`, returns the value of expr."
  [label expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (prn (str ~label ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))
