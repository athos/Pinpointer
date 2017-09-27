(defproject pinpointer "0.1.0-SNAPSHOT"
  :description "Pinpointer is yet another spec error reporter with sophisticated error analysis"
  :url "https://github.com/athos/Pinpointer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha19" :scope "provided"]
                 [org.clojure/clojurescript "1.9.908" :scope "provided"]
                 [clansi "1.0.0"]
                 [fipp "0.6.8"]
                 [spectrace "0.1.0-SNAPSHOT"]]

  :plugins [[lein-cloverage "1.0.9"]
            [lein-cljsbuild "1.1.4"]
            [lein-eftest "0.3.1"]]

  :cljsbuild
  {:builds
   {:dev {:source-paths ["src"]
          :compiler {:output-to "target/main.js"
                     :output-dir "target"
                     :optimizations :whitespace
                     :pretty-print true}}}}

  :eftest {:report eftest.report.pretty/report}

  :profiles
  {:dev {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]]}}

  :aliases {"test-all" ["do" ["test-clj"]]
            "test-clj" ["eftest"]})
