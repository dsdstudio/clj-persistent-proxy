(ns opproxy.core
  (:gen-class)
  (:require
   [environ.core :refer [env]]
   [system.repl :refer [set-init! start]]
   [opproxy.systems :refer [prod-system]]))

(def env-keys [:http-port
               :repl-port
               :proxy-server
               :redis-url])

(defn- assert-env? []
  (every? #(contains? env %) env-keys))

(defn- print-usage []
  (println
   "need parameter <http.port> <proxy.server> \n" 
   "java -Dhttp.port=3001 -Dproxy.server=http://localhost:9090 -Dredis-url=\"redis://localhost:6379\""))

(defn -main
  "start application"
  [& args]
  (if-not (assert-env?) (print-usage)
          (let [system (or (first args) #'prod-system)]
            (set-init! system)
            (start))))
