(defproject cljs-todos "0.1.0-SNAPSHOT"
  :description "FIXME: write this!"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [noir "1.2.1"]
                 
                 [pinot "0.1.1-SNAPSHOT"]
                 
                 [hiccup "0.3.6"]
                 [org.clojars.wilkes/gaka "0.2.2"] ; for 1.3 compat
                 
                 [org.clojure/tools.logging "0.2.0"]
                 [org.clojure/data.json "0.1.1"]
                 
                 ]
  :dev-dependencies [[lein-eclipse "1.0.0"]]
  :main cljs-todos.x.server)

