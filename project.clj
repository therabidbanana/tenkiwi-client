(defproject tenkiwi-rn "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.866"]

                 ;; [com.bhauman/figwheel-main "0.2.13"]

                 [binaryage/oops "0.7.0"]
                 [reagent "0.10.0" :exclusions [cljsjs/react cljsjs/react-dom cljsjs/react-dom-server cljsjs/create-react-class]]

                 [re-frame "1.1.2"]

                 [com.stuartsierra/component "0.3.2"]
                 [org.danielsz/system "0.4.6"]

                 [com.taoensso/sente "1.16.0"]
                 [com.cognitect/transit-cljs "0.8.269"]
                 ]
  :plugins [[lein-cljsbuild "1.1.4"]
            #_[lein-figwheel "0.5.20"]]
  :clean-targets ["target/" "main.js"]
  :aliases {"figwheel"        ["trampoline" "run" "-m" "figwheel.main" "-b" "dev" "--repl"]
                                        ; TODO: Remove custom extern inference as it's unreliable
                                        ;"externs"         ["do" "clean"
                                        ;                   ["run" "-m" "externs"]]
            "rebuild-modules" ["run" "-m" "user" "--rebuild-modules"]
            "prod-build"      ^{:doc "Recompile code with prod profile."}
            ["with-profile" "prod" "cljsbuild" "once" "main"]}
  :profiles {:dev  {:dependencies [#_[figwheel-sidecar "0.5.20"]
                                   [cider/piggieback "0.5.2"]
                                   [com.bhauman/figwheel-main "0.2.13"]
                                   ;; optional but recommended
                                   [com.bhauman/rebel-readline-cljs "0.1.4"]
                                   ]
                    :source-paths ["src"]
                    #_:cljsbuild  #_ {:builds [{:id           "main"
                                                :source-paths ["src" "env/dev"]
                                                :figwheel     true
                                                :compiler     {:output-to     "target/expo/index.js"
                                                               :main          "env.expo.main"
                                                               :output-dir    "target/expo"
                                                               :nodejs-rt     false
                                                               :optimizations :none
                                                               :target        :bundle}}]}
                    :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}
             :prod {:cljsbuild {:builds [{:id           "main"
                                          :source-paths ["src" "env/prod" "js"]
                                          :compiler     {:output-to      "index.js"
                                                         :main           "env.expo.main"
                                                         :output-dir     "target/expo"
                                                         :static-fns     true
                                                         ;; :verbose           true
                                                         :externs        ["js/externs.js"]
                                                         :preamble       ["assets.js" "worklets.js"]
                                                         :infer-externs  true
                                                         :parallel-build true
                                                         :pseudo-names   true
                                                         :pretty-print   true
                                                         ;; :optimize-constants true
                                                         ;; :optimizations      :simple
                                                         :optimizations  :advanced
                                                         ;; :nodejs-rt false

                                                         :closure-defines {"goog.DEBUG" false}
                                                         :target          :bundle}}]}}})
