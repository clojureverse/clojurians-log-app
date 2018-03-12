(defproject clojurians-log "0.1.0-SNAPSHOT"
  :description "Clojurians Slack log history app"
  :url "https://github.com/clojureverse/clojurians-slack-log"
  :license {:name "Mozilla Public License 2.0"
            :url "https://www.mozilla.org/en-US/MPL/2.0/"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [com.cognitect/transit-clj "0.8.300"]
                 [ring "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [bk/ring-gzip "0.2.1"]
                 [radicalzephyr/ring.middleware.logger "0.6.0"]
                 [clj-logging-config "1.9.12"]
                 [compojure "1.6.0"]
                 [environ "1.1.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [org.danielsz/system "0.4.2-SNAPSHOT"]
                 [org.clojure/tools.namespace "0.3.0-alpha4"]
                 [http-kit "2.3.0-alpha4"]
                 [re-frame "0.10.3-SNAPSHOT"]
                 [lambdaisland/garden-watcher "0.3.2"]
                 [hiccup "2.0.0-alpha1"]
                 [org.clojure/data.json "0.2.6"]
                 [clojure.java-time "0.3.1"]
                 [prone "1.1.4"]
                 [com.google.guava/guava "23.5-jre"]
                 [aero "1.1.2"]
                 [lambdaisland/repl-tools "0.1.0"]
                 [clj-http "3.7.0"]
                 [org.julienxx/clj-slack "0.5.5"]
                 [reloaded.repl "0.2.4"]
                 [instaparse "1.4.8"]
                 [com.cemerick/url "0.1.1"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-environ "1.1.0"]]

  :min-lein-version "2.6.1"

  :source-paths ["src/clj" "src/cljs" "src/cljc"]

  :test-paths ["test/clj" "test/cljc"]

  :clean-targets ^{:protect false} [:target-path :compile-path "resources/public/js" "dev-target"]

  :uberjar-name "clojurians-log.jar"

  ;; Use `lein run` if you just want to start a HTTP server, without figwheel
  :main clojurians-log.application

  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (go) and
  ;; (browser-repl) live.
  :repl-options {:init-ns user}

  :cljsbuild {:builds
              [{:id "app"
                :source-paths ["src/cljs" "src/cljc" "src/clj" "dev"]

                :figwheel {:on-jsload "clojurians-log.system/reset"}

                :compiler {:main cljs.user
                           :asset-path "js/compiled/out"
                           :output-to "dev-target/public/js/compiled/clojurians_log.js"
                           :output-dir "dev-target/public/js/compiled/out"
                           :source-map-timestamp true}}

               {:id "test"
                :source-paths ["src/cljs" "test/cljs" "src/cljc" "test/cljc"]
                :compiler {:output-to "dev-target/public/js/compiled/testable.js"
                           :main clojurians-log.test-runner
                           :optimizations :none}}

               {:id "min"
                :source-paths ["src/cljs" "src/cljc"]
                :jar true
                :compiler {:main clojurians-log.system
                           :output-to "resources/public/js/compiled/clojurians_log.js"
                           :output-dir "target"
                           :source-map-timestamp true
                           :optimizations :advanced
                           :closure-defines {goog.DEBUG false}
                           :pretty-print false}}]}

  ;; When running figwheel from nREPL, figwheel will read this configuration
  ;; stanza, but it will read it without passing through leiningen's profile
  ;; merging. So don't put a :figwheel section under the :dev profile, it will
  ;; not be picked up, instead configure figwheel here on the top level.

  :figwheel {;; :http-server-root "public"       ;; serve static assets from resources/public/
              :server-port 3459                ;; default
             ;; :server-ip "127.0.0.1"           ;; default
             :css-dirs ["resources/public/css"]  ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process. We
             ;; don't do this, instead we do the opposite, running figwheel from
             ;; an nREPL process, see
             ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
             ;; :nrepl-port 7888

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             :server-logfile "log/figwheel.log"}

  :doo {:build "test"}

  :aliases {"prep" ["do"
                    #_"compile"
                    #_["cljsbuild" "once" "min"]
                    ["run" "-m" "garden-watcher.main" "clojurians-log.styles"]]}

  :profiles {:dev
             {:dependencies [[figwheel "0.5.15-SNAPSHOT"]
                             [figwheel-sidecar "0.5.15-SNAPSHOT"]
                             [com.cemerick/piggieback "0.2.2"]
                             [org.clojure/tools.nrepl "0.2.13"]
                             [lein-doo "0.1.8"]
                             [com.datomic/datomic-free "0.9.5656"]
                             [com.cemerick/pomegranate "1.0.0"]
                             [alembic "0.3.2"]]

              :plugins [[lein-figwheel "0.5.15-SNAPSHOT"]
                        [lein-doo "0.1.8"]]

              :source-paths ["dev"]
              :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}

             :production
             {:aot :all
              :source-paths ^:replace ["src/clj" "src/cljc"]
              :dependencies [;; Set through .lein/profiles.clj, see ansible scripts
                             ;; [com.datomic/datomic-pro ""]

                             ;; JDBC driver for Datomic+PostgreSQL
                             [org.postgresql/postgresql "9.3-1102-jdbc41"]]}

             :uberjar
             {:source-paths ^:replace ["src/clj" "src/cljc"]
              :prep-tasks ["compile"
                           ["cljsbuild" "once" "min"]
                           ["run" "-m" "garden-watcher.main" "clojurians-log.styles"]]
              :hooks []
              :omit-source true
              :aot :all}})
