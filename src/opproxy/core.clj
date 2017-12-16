(ns opproxy.core
  (:gen-class)
  (:require
   [system.repl :refer [set-init! start]]
   [opproxy.systems :refer [prod-system]]))

(defn -main
  "start application"
  [& args]
  (let [system (or (first args) #'prod-system)]
    (set-init! system)
    (start)))
