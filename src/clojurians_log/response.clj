(ns clojurians-log.response
  (:require [hiccup2.core :as hiccup]))

(defn render [{:response/keys [html status headers] :or {status 200} :as context}]
  {:status (or status 200)
   :headers (merge
             {"Content-Type" "text/html;charset=UTF-8"}
             headers)
   :body (str (hiccup/html html))})

(defn xml-render [{:response/keys [xml status headers]}]
  {:status (or status 200)
   :headers (merge
             {"Content-Type" "text/xml;charset=UTF-8"}
             headers)
   :body (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n"
              (hiccup/html {:mode :xml} xml))})
