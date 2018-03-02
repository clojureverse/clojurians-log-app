(ns clojurians-log.time-util
  (:require [java-time :as jt]
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
