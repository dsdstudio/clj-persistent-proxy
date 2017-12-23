(ns opproxy.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [clj-http.client :as hc]
            [environ.core :refer [env]]
            [taoensso.carmine :as car :refer (wcar)]))

(def conn {:pool {} :spec {:uri (env :redis-url)}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(defn- cache-hit?
  [^String key]
  (= 1 (wcar* (car/exists key))))

(defn- write-cache
  [^String key data]
  (println "write-cache::" key)
  (wcar*
   (car/set key (pr-str data))))

(defn- cache-data
  [^String key]
  (println "cache-hit::" key)
  (read-string (wcar*
                (car/get key))))

(defn- md5 [^String s]
  (let [algorithm (java.security.MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (java.math.BigInteger. 1 raw))))

(defn- to-query-param [query-params]
  (str "?" (clojure.string/join "&" (map (fn [[k v]] (str k "=" v)) query-params))))

(defn- proxy-response
  [request-method request-url request-cookie form-params]
  (cond
    (= :get request-method) (hc/get request-url {:cookies request-cookie})
    (= :post request-method) (hc/post request-url {:form-params form-params :cookies request-cookie})
    (= :put request-method) (hc/put request-url {:form-params form-params :cookies request-cookie})
    (= :delete request-method) (hc/delete request-url {:cookies request-cookie})))

(defn- to-ring-response
  "ring middleware에서 cookie 값에 없는 키를 삽입하면 assert 에러가 남.
  그래서 httpclient에서 나온 response중 :version, :discard 키를 제거하도록 처리"
  [response]
  (assoc-in response [:cookies] (into {} (map (fn [[k v]] [k (dissoc v :version :discard)]) (:cookies response)))))

(defroutes app-routes
  (GET "/api/keys" []
        (response (wcar*
                   (car/keys "*"))))
  (route/not-found "Not Found"))

(def static-assets
  (map #(str "/**/" %) ["*.js"
                        "*.css"
                        "*.html"
                        "*.htm"
                        "*.mp4"
                        "*.m4v"
                        "*.aac"
                        "*.mp3"
                        "*.mp4"
                        "*.*map"
                        "*.gif"
                        "*.jpg"
                        "*.jpeg"
                        "*.png"]))

(def matcher (org.springframework.util.AntPathMatcher.))

(defn resource-url? [url]
  (some #(-> matcher
             (.match % url)) static-assets))

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
          request-cookie (:cookies request)
          query-params (:query-params request)
          request-url (str proxy-url
                           (:uri request)
                           (if (empty? query-params) "" (to-query-param query-params)))
          form-params (:form-params request)
          cache-key (md5 (str proxy-url
                              "\n"
                              request-url
                              "\n"
                              request-method
                              "\n"
                              (pr-str form-params)))]
      (if (cache-hit? cache-key) (cache-data cache-key)
          (let [response (proxy-response request-method request-url request-cookie form-params)
                converted-response (to-ring-response response)]
            (if-not (resource-url? request-url) (write-cache cache-key converted-response))
            converted-response)))))

;; threading first macro 는 마지막 인자부터 소비한다. 미들웨어의 순서에 주의 할것.. 
(def app
  (-> app-routes
      (wrap-proxy-request (env :proxy-server))
      (wrap-json-response {:keywords? true})
      (wrap-json-body {:keywords? true})
      (wrap-defaults (-> site-defaults
                         (assoc-in [:security :anti-forgery] false)))))
