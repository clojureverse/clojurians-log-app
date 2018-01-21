(ns clojurians-log.config
  (:require [environ.core :refer [env]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [prone.middleware :as prone]
            [aero.core :as aero]
            [clojure.java.io :as io]
            [ring.middleware.file :refer [wrap-file]]))

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

(defn config [profile]
  (assoc-in (aero/read-config (io/resource "clojurians-log/config.edn") {:profile profile})
            [:http :middleware]
            (middleware-stack profile)))
