(ns request-response-context)

;; Ring
;; handler = request -> response

;; Pedestal -> Cognitect
;; handler = context -> context
;; context = request + response

{:request
 }

(def req {:http-method :post
          :uri "/"})

(defn handler-1 [req]
  (when (= (:uri req) "/")
    {:status 200
     :body "OK"}))

(handler-1 req)
;; => {:status 200, :body "OK"}

(defn handler-2 [ctx]
  (when (= (:uri (:request ctx)) "/")
    (assoc ctx
           :response
           {:status 200
            :body "OK"})))

(handler-2 {:request req})
;; => {:request {:http-method :post, :uri "/"}, :response {:status 200, :body "OK"}}

{:http-method :get
 :uri "/kaocha/2010-10-05"
 :path-params {:channel "kaocha"
               :date "2010-10-05"}}

{:request {:http-method :get
           :uri "/kaocha/2010-10-05"
           :path-params {:channel "kaocha"
                         :date "2010-10-05"}}
 :data/title ...
 :data/messages ...
 :request/html [:html [:body ...]]
 :request/status 200
 :request/headers}
