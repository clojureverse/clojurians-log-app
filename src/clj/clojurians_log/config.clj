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

(defn middleware-stack [config]
  {:prod [[wrap-defaults site-defaults]
          wrap-with-logger
          wrap-gzip
          [wrap-cache-control (:cache-time config)]
          prone/wrap-exceptions]
   :dev [[wrap-file "dev-target/public"]
         [wrap-defaults site-defaults]
         wrap-with-logger
         wrap-gzip
         [wrap-cache-control (:cache-time config)]
         prone/wrap-exceptions]})

(defn config-file []
  (io/resource "clojurians-log/config.edn"))

(defn config [profile & [defaults]]
  (let [config (-> (config-file)
                   (aero/read-config {:profile profile})
                   (merge defaults))]
     (assoc config :middleware (get (middleware-stack config) profile))))
