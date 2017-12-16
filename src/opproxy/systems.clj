(ns opproxy.systems
  (:gen-class)
  (:require [opproxy.handler :refer [app]]
            [system.core :refer [defsystem]]
            (system.components
             [jetty :refer [new-web-server]]
             [repl-server :refer [new-repl-server]])
            [environ.core :refer [env]]))

(defsystem dev-system
  [:web (new-web-server (Integer/valueOf (env :http-port)) app)])

(defsystem prod-system
  [:web (new-web-server (Integer/valueOf (env :http-port)) app)
   :repl-server (new-repl-server (Integer/valueOf (env :repl-port)))])
  
