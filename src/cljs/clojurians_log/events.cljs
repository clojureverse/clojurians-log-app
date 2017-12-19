(ns clojurians-log.events
  (:require [re-frame.core :as re-frame]
            [clojurians-log.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))
