(defproject pinpointer "0.1.0-SNAPSHOT"
  :description "Pinpointer makes it easy to grasp which part of data is violating spec conformance."
  :url "https://github.com/athos/Pinpointer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [;; clj stuff
                 [org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/spec.alpha "0.1.123"]
                 [fipp "0.6.8"]
                 [spectrace "0.1.0-SNAPSHOT"]
                 [rewrite-clj "0.5.1"]
                 [clansi "1.0.0"]
                 ;; cljs stuff
                 [org.clojure/clojurescript "1.9.562" :scope "provided"]
                 [rewrite-cljs "0.4.1"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :cljsbuild
  {:builds
   {:dev {:source-paths ["src"]
          :compiler {:output-to "target/main.js"
                     :output-dir "target"
                     :optimizations :whitespace
                     :pretty-print true}}}})
