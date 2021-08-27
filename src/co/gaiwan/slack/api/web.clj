(ns co.gaiwan.slack.api.web
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [lambdaisland.glogc :as log]
            [lambdaisland.uri :as uri]))

(defn- verify-api-url!
  [connection]
  (assert
   (and (string? (:api-url connection))
        (and (not (empty? (:api-url connection)))
             (not (nil? (re-find #"^https?:\/\/" (:api-url connection))))))
   (str "clj-slack: API URL is not valid. :api-url has to be a valid URL (https://slack.com/api usually), but is " (pr-str (:api-url connection)))))

(defn- verify-token!
  [connection]
  (assert
   (and (string? (:token connection))
        (not (empty? (:token connection))))
   (str "clj-slack: Access token is not valid. :token has to be a non-empty string, but is " (pr-str (:token connection)))))

(defn- verify-conn!
  "Checks the connection map"
  [conn]
  (verify-api-url! conn)
  (when (not (contains? conn :skip-token-validation))
    (verify-token! conn))
  nil)

(defn- send-get-request
  "Sends a GET http request with formatted params.
  Optional request options can be specified which will be passed to `hato`
  without any changes."
  [url {:keys [token path]} & [opts]]
  (let [full-url (str url path)
        response (http/get full-url (merge {:oauth-token token
                                            :throw-exceptions false}) opts)]
    (if-let [body (:body response)]
      (json/read-str body :key-fn clojure.core/keyword)
      (do
        ;; Slack normally returns a JSON body with `:ok false`, so this is for
        ;; truly exceptional cases
        (log/error :error-from-slack-api (:error response))
        :error))))

(defn- request-options
  "Extracts request options from slack connection map.
  Provides sensible defaults for timeouts."
  [connection]
  (let [default-options {:conn-timeout 60000
                         :socket-timeout 60000}]
    (merge default-options
           (dissoc connection :api-url :token))))

(defn- build-params
  "Builds the full URL (endpoint + params)"
  ([conn endpoint query-map]
   (verify-conn! conn)
   {:token (:token conn)
    :path  (-> (uri/uri (str "/" endpoint))
               (uri/assoc-query* query-map)
               str)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The request API

(defn slack-request
  ([conn endpoint]
   (slack-request conn endpoint {}))
  ([conn endpoint opt]
   (verify-conn! conn)
   (let [url    (:api-url conn)
         params (build-params conn endpoint opt)]
     (send-get-request url params (request-options conn)))))
