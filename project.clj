(defproject pinpointer "0.1.0-SNAPSHOT"
  :description "Pinpointer is yet another spec error reporter based on a precise error analysis"
  :url "https://github.com/athos/Pinpointer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :test-paths ["test/cljc"]

  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [clansi "1.0.0"]
                 [fipp "0.6.10"]
                 [spectrace "0.1.0"]]

  :plugins [[lein-cloverage "1.0.9"]
            [lein-cljsbuild "1.1.4"]
            [lein-doo "0.1.8"]
            [lein-eftest "0.3.1"]]

  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/out/test.js"
                                   :output-dir "target/out"
                                   :main pinpointer.runner
                                   :optimizations :none}}
                       {:id "nashorn-test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/nashorn_out/test.js"
                                   :output-dir "target/nashorn_out"
                                   :main pinpointer.runner
                                   :optimizations :whitespace}}
                       {:id "node-test"
                        :source-paths ["src" "test/cljc" "test/cljs"]
                        :compiler {:output-to "target/node_out/test.js"
                                   :output-dir "target/node_out"
                                   :main pinpointer.runner
                                   :optimizations :none
                                   :target :nodejs}}]}

  :eftest {:report eftest.report.pretty/report}

  :profiles
  {:dev {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]]}}

  :aliases {"test-all" ["do" ["test-clj"] ["test-cljs"]]
            "test-clj" ["eftest"]
            "test-cljs" ["do" ["test-cljs-none" "once"]
                              ["test-cljs-nashorn" "once"]]
            "test-cljs-none" ["doo" "phantom" "test"]
            "test-cljs-nashorn" ["doo" "nashorn" "nashorn-test"]
            "test-cljs-node" ["doo" "node" "node-test"]})
