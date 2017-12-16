(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.9.0"]
                 [org.danielsz/system "0.4.1"]
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [ring/ring-json "0.4.0"]
                 [compojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [clj-http "3.7.0"]
                 
                 [environ "1.1.0"]
                 [boot-environ "1.1.0"]
                 
                 [com.taoensso/carmine "2.16.0"]
                 [cider/cider-nrepl "0.15.1"]
                 [org.slf4j/slf4j-simple "1.7.25"]
                 [adzerk/boot-reload "0.5.2" :scope "test"]])

(require
 '[environ.boot :refer [environ]]
 '[system.boot :refer [system run]]
 '[system.repl :refer [go reset]]
 '[opproxy.systems :refer [dev-system]])

(deftask dev []
  (comp
   (environ :env {:http-port "3001" :repl-port "9888"})
   (watch :verbose true)
   (system :sys #'dev-system :auto true :files ["handler.clj"])
   (repl :server true)))

(deftask build []
  (comp
   (aot)
   (pom)
   (uber)
   (jar)))
