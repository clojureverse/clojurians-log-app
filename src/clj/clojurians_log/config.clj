(ns clojurians-log.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
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

(def middleware-stack
  {:prod [[wrap-defaults site-defaults]
          wrap-with-logger
          wrap-gzip
          prone/wrap-exceptions]
   :dev [[wrap-file "dev-target/public"]
         [wrap-defaults site-defaults]
         wrap-with-logger
         wrap-gzip
         prone/wrap-exceptions]})

(defn config
  ([file profile]
   (assoc-in (aero/read-config file {:profile profile})
             [:http :middleware]
             (middleware-stack profile)))
  ([profile]
   (config (io/resource "clojurians-log/config.edn") profile)))
