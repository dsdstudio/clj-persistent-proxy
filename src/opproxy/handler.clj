(ns opproxy.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [clj-http.client :as hc]
            [taoensso.carmine :as car :refer (wcar)]))

(def conn {:pool {} :spec {:uri "redis://127.0.0.1:6379"}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(defroutes app-routes
  (GET "/api/keys" []
        (response (wcar*
                   (car/keys "*"))))
  (route/not-found "Not Found"))

;; Proxy request caching middleware
;; TODO multipart processing
;; TODO save to redis
;; TODO redis cache hit -> get redis data -> response
;; TODO redis cache miss -> request proxy server -> response -> save to redis(response) -> response return 
(defn wrap-proxy-request [handler proxy-url]
  (fn [request]
    (let [request-method (:request-method request)
          query-params (:query-params request)
          request-url (str proxy-url
                           (:uri request)
                           "?"
                           (clojure.string/join "&" (map (fn [[k v]] (str k "=" v)) query-params)))
          form-params (:form-params request)
          response (cond
                     (= :get request-method) (hc/get request-url)
                     (= :post request-method) (hc/post request-url {:form-params form-params})
                     (= :put request-method) (hc/put request-url {:form-params form-params})
                     (= :delete request-method) (hc/delete request-url))]
      response)))

;; threading first macro 는 마지막 인자부터 소비한다. 미들웨어의 순서에 주의 할것.. 
(def app
  (-> app-routes
      (wrap-proxy-request "http://localhost:9090")
      (wrap-json-response {:keywords? true})
      (wrap-json-body {:keywords? true})
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
