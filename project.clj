(defproject dog-and-duck/quack "0.1.0-SNAPSHOT"
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
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/tools.cli "1.0.214"]
                 [trptr/java-wrapper "0.2.3"]]
  :description "A validator for ActivityStreams."
  :license {:name "GPL-2.0-or-later"
            :url "https://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"}
  :main ^:skip-aot dog-and-duck.quack.core
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :repl-options {:init-ns dog-and-duck.quack.core}
  :target-path "target/%s"
  :url "http://example.com/FIXME")
