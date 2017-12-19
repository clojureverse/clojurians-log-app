(ns clojurians-log.routes
  (:require [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE routes]]
            [compojure.route :refer [resources]]
            [ring.util.response :refer [response]]
            [clojurians-log.response :as response]
            [clojurians-log.data :as data]
            [clojurians-log.views :as views]))

(defn context [request]
  {:request request})

(defn home-routes [endpoint]
  (routes
   (GET "/" _
     (-> "public/index.html"
         io/resource
         io/input-stream
         response
         (assoc :headers {"Content-Type" "text/html; charset=utf-8"})))

   ;; https://clojurians-log.clojureverse.org/clojure/2017-11-15.html
   (GET "/:channel/:year-:month-:day.html" request
     (-> request
         context
         data/load-channel-messages
         views/log-page
         response/render))

   (resources "/")))
