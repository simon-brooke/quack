(defproject dog-and-duck/quack "0.1.0-SNAPSHOT"
  :aliases {"test-cljs" ["with-profile" "test" "doo" "rhino" "test" "once"]
            "test-all"  ["do" ["test"] ["test-cljs"]]}
  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]

                ;; The presence of a :figwheel configuration here
                ;; will cause figwheel to inject the figwheel client
                ;; into your build
                :figwheel {:on-jsload "froboz.core/on-js-reload"
                           ;; :open-urls will pop open your application
                           ;; in the default browser once Figwheel has
                           ;; started and compiled your application.
                           ;; Comment this out once it no longer serves you.
                           :open-urls ["http://localhost:3449/index.html"]}

                :compiler {:main froboz.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/froboz.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true
                           ;; To console.log CLJS data-structures make sure you enable devtools in Chrome
                           ;; https://github.com/binaryage/cljs-devtools
                           :preloads [devtools.preload]}}
               ;; This next build is a compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/froboz.js"
                           :main froboz.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :cloverage {:output "docs/cloverage"
              :codecov? true
              :emma-xml? true}
  :codox {:metadata {:doc "**TODO**: write docs"
                     :doc/format :markdown}
          :output-path "docs/codox"
          :source-uri "https://github.com/simon-brooke/quack/blob/master/{filepath}#L{line}"}
  :dependencies [[com.taoensso/timbre "6.0.4"]
                 [hiccup "1.0.5"]
                 [mvxcvi/clj-pgp "1.1.0"]
                 [org.clojars.simon_brooke/internationalisation "1.0.5"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.773"]
                 [org.clojure/core.async  "0.4.500"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.cli "1.0.214"]
                 [trptr/java-wrapper "0.2.3"]]
  :description "A validator for ActivityStreams."
  :doo {:paths {:rhino "lein run -m org.mozilla.javascript.tools.shell.Main"}}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this

             ;; doesn't work for you just run your own server :) (see lein-ring)

             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you are using emacsclient you can just use
             ;; :open-file-command "emacsclient"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"

             ;; to pipe all the output to the repl
             ;; :server-logfile false
             }
  :license {:name "GPL-2.0-or-later"
            :url "https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"}
  :main ^:skip-aot dog-and-duck.quack.core
  :plugins [[lein-cljsbuild "1.1.3"]
            [lein-doo "0.1.6"]]
  :profiles {:dev {:dependencies [[binaryage/devtools "1.0.0"]
                                  [figwheel-sidecar "0.5.20"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src/cljs" "src/cljc" "dev"]
                   ;; need to add the compiled assets to the :clean-targets
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}
             :test {:dependencies [[org.mozilla/rhino "1.7.7"]]
                    :cljsbuild
                    {:builds
                     {:test
                      {:source-paths ["src" "test"]
                       :compiler {:output-to "target/main.js"
                                  :output-dir "target"
                                  ;; :main {{top-namespace}}.test-runner
                                  :optimizations :simple}}}}}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :repl-options {:init-ns dog-and-duck.quack.core}
  :target-path "target/%s"
  :url "http://example.com/FIXME")
