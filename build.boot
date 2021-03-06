(set-env!
 :source-paths #{"src", "test"}
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

                 
                 [org.springframework/spring-core "5.0.2.RELEASE"]

                 [com.taoensso/carmine "2.16.0"]
                 [cider/cider-nrepl "0.16.0-SNAPSHOT"]
                 [org.slf4j/slf4j-simple "1.7.25"]

                 [ring/ring-mock "0.3.2" :scope "test"]
                 
                 [adzerk/boot-test "1.2.0" :scope "test"]
                 [adzerk/boot-reload "0.5.2" :scope "test"]])

(require
 '[adzerk.boot-test :refer :all]
 '[environ.boot :refer [environ]]
 '[system.boot :refer [system run]]
 '[system.repl :refer [go reset]]
 '[opproxy.systems :refer [dev-system]])

(task-options!
 pom {:project 'opproxy
      :version "0.1.0"}
 jar {:manifest {} :main 'opproxy.core}
 aot {:namespace #{'opproxy.core}})

(def default-config {:http-port "3001"
                     :repl-port "9888"
                     :proxy-server "http://localhost:9090"
                     :redis-url "redis://127.0.0.1:6379"})

(deftask watch-test []
  (comp
   (environ :env default-config)
   (watch :verbose true)
   (repl :server true)
   (speak)
   (test)))

(deftask dev []
  (comp
   (environ :env default-config)
   (watch :verbose true)
   (system :sys #'dev-system :auto true :files ["handler.clj"])
   (repl :server true)))

(deftask build []
  (comp
   (aot)
   (uber)
   (jar :file "opproxy-server.jar")
   (sift :include #{#"opproxy-server.jar"})
   (target)))
