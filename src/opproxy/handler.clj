(ns opproxy.handler
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [response]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
            [clj-http.client :as hc]
            [taoensso.carmine :as car :refer (wcar)]))

(def conn {:pool {} :spec {:uri "redis://127.0.0.1:6379"}})
(defmacro wcar* [& body] `(car/wcar conn ~@body))

(println (wcar*
          (car/ping)))

(defroutes app-routes
  (GET "/api/keys" []
        (response (wcar*
                   (car/keys "*"))))
  (route/not-found "Not Found"))

;; TODO Proxy request caching middleware
;; 
(defn wrap-proxy-request [handler proxy-url]
  (fn [request]
    (let [response (handler request)]
      (println proxy-url response)
      response)))
      

(def app
  (-> app-routes
      (wrap-json-response {:keywords? true})
      (wrap-json-body {:keywords? true})
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      (wrap-proxy-request "https://api.github.com")))
