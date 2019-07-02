(ns clojurians-log.xml2hiccup
  "Convert clojure.xml style data (also used by Enlive) to hiccup."
  (:require [clojure.string :as str]))

(defn build-tag [tag {:keys [id class] :as attrs}]
  [(keyword (cond-> (name tag)
              id (str "#" id)
              class (str "." (-> class
                                 (str/split #"\s+")
                                 (->> (str/join "."))))))
   (dissoc attrs :id :class)])

(defn xml2hiccup [el]
  (if (string? el)
    el
    (let [{:keys [tag attrs content]} el
          [tag attrs]                 (build-tag tag attrs)]
      (cond-> [tag]
        (seq attrs)   (conj attrs)
        (seq content) (into (map xml2hiccup content))))))
