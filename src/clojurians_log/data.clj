(ns clojurians-log.data
  (:require [clojure.java.io :as io]
            [clojure.tools.reader.edn :as edn]
            [clojure.data.json :as json]
            [clojurians-log.time-util :as cl.tu]))

(def channels
  (memoize
   #(edn/read-string (slurp (io/resource "channels.edn")))))

(def channels-by-name
  (memoize
   #(into {} (map (juxt :name identity)) (vals (channels)))))

(def users
  (memoize
   #(edn/read-string {:readers {'js identity}} (slurp (io/resource "users.edn")))))

(defn channel [id]
  (get (channels) id))

(defn user [id]
  (get (users) id))

(defn channel-by-name [name]
  (get (channels-by-name) name))

(defn coerce-message [message]
  (-> message
      (update :user user)
      (assoc :inst (cl.tu/ts->inst (:ts message)))))

(defn event-seq [file]
  (some->> file
           io/reader
           line-seq
           (map json/read-json)))

(defn log-files []
  (file-seq (io/file (io/resource "logs"))))


(comment
  (take 10 (keys (channels)))
  ;;=> ("C0747K4K0" "C0JDNS75G" "C6U2MP5GU" "C0BQDEJ8M" "C08TP4YP4" "C6MRNBCKC" "C2B8HG92P" "C0N1QHE3W" "C064BA6G2" "C7Q9GSHFV")

  (user (:creator (channel "C0JDNS75G"))))
