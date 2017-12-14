(set-env!
 :source-paths #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[me.raynes.conch "0.8.0"]])
(task-options!
 pom {:project 'clj-pproxy
      :version "0.0.1"}
 aot {:namespace '#{clj-pproxy.core}}
 jar {:main 'clj_pproxy.core
      :manifest {"Description" "offline persistence proxy server"}}
 
(deftask build
  "jar 묶기"
  []
  (comp
   (aot)
   (pom)
   (uber)
   (jar)))
