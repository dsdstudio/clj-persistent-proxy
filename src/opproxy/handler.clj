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

(defn- cache-hit?
  "해당 키가 캐시에 존재하는지 확인한다."
  [key]
  (= 1 (wcar* (car/exists key))))

(defn- write-cache
  "cache에 데이터를 기록한다. "
  [key data]
  (println "write-cache::" key)
  (wcar*
   (car/set key (pr-str data))))

(defn- cache-data
  "캐시에 있는 데이터를 조회한다. "
  [key]
  (println "cache-hit::" key)
  (read-string (wcar*
                (car/get key))))
  

(defn md5 [^String s]
  (let [algorithm (java.security.MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (java.math.BigInteger. 1 raw))))

(defroutes app-routes
  (GET "/api/keys" []
        (response (wcar*
                   (car/keys "*"))))
  (route/not-found "Not Found"))

;; cache key -> url:method:params
;; Proxy request caching middleware
;; TODO URL Pattern filter 
;; TODO multipart processing
;; DONE save to redis
;; DONE redis cache hit -> get redis data -> response
;; DONE redis cache miss -> request proxy server -> response -> save to redis(response) -> response return 
(defn wrap-proxy-request [handler proxy-url]
  (fn [request]
    (let [request-method (:request-method request)
          query-params (:query-params request)
          request-url (str proxy-url
                           (:uri request)
                           "?"
                           (clojure.string/join "&" (map (fn [[k v]] (str k "=" v)) query-params)))
          form-params (:form-params request)
          cache-key (md5 (str proxy-url
                              "\n"
                              request-url
                              "\n"
                              request-method
                              "\n"
                              (pr-str form-params)))]
      (cond
        (cache-hit? cache-key) (cache-data cache-key)
        :else (let [response (cond
                (= :get request-method) (hc/get request-url)
                (= :post request-method) (hc/post request-url {:form-params form-params})
                (= :put request-method) (hc/put request-url {:form-params form-params})
                (= :delete request-method) (hc/delete request-url))]
                (write-cache cache-key response)
                response)))))

;; threading first macro 는 마지막 인자부터 소비한다. 미들웨어의 순서에 주의 할것.. 
(def app
  (-> app-routes
      (wrap-proxy-request "http://localhost:9090")
      (wrap-json-response {:keywords? true})
      (wrap-json-body {:keywords? true})
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))))
