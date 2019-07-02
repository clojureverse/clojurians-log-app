(ns clojurians-log.response
  (:require [hiccup2.core :as hiccup]))

(defn render [{:response/keys [html status headers] :or {status 200} :as context}]
  {:status (or status 200)
   :headers (merge
             {"Content-Type" "text/html;charset=UTF-8"}
             headers)
   :body (str (hiccup/html html))})
