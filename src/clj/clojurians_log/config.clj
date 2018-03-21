(ns clojurians-log.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojurians-log.middleware.cache-control :refer [wrap-cache-control]]
            [prone.middleware :as prone]
            [ring.middleware.defaults :refer [wrap-defaults]]
            [ring.middleware.file :refer [wrap-file]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.session.memory :as mem]))

(def session-store (mem/memory-store))

(def site-defaults
  {:params {:urlencoded true
            :multipart true
            :nested true
            :keywordize true}
   :cookies true
   :session {:store session-store
             :flash true
             :cookie-attrs {:http-only true :same-site :strict}}
   :security {:anti-forgery true
              :xss-protection {:enable? true :mode :block}
              :frame-options :sameorigin
              :content-type-options :nosniff}
   :static {:resources "public"}
   :responses {:not-modified-responses true
               :absolute-redirects true
               :content-types true
               :default-charset "utf-8"}})

(defn add-middleware [config]
  (let [profile (::profile config)
        dev?    (= :dev profile)]
    (assoc config
           :middleware
           (cond-> [[wrap-defaults site-defaults]
                    wrap-with-logger
                    wrap-gzip
                    [wrap-cache-control (:cache-time config)]]
             dev? (into [[wrap-file "dev-target/public"]
                         prone/wrap-exceptions])))))

(defn config-file []
  (io/resource "clojurians-log/config.edn"))

(defn read-config [file profile]
  (assoc (aero/read-config file {:profile profile})
         ::profile profile))

(defn config [profile & [defaults]]
  (-> (config-file)
      (read-config profile)
      (merge defaults)
      (add-middleware)))
