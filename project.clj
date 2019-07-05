(defproject clojurians-log "0.1.0-SNAPSHOT"
  :description "Clojurians Slack log history app"
  :url "https://github.com/clojureverse/clojurians-slack-log"
  :license {:name "Mozilla Public License 2.0"
            :url "https://www.mozilla.org/en-US/MPL/2.0/"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [com.cognitect/transit-clj "0.8.313"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]
                 [bk/ring-gzip "0.3.0"]
                 [radicalzephyr/ring.middleware.logger "0.6.0"]
                 [clj-logging-config "1.9.12"]
                 [compojure "1.6.1"]
                 [environ "1.1.0"]
                 [com.stuartsierra/component "0.4.0"]
                 [org.danielsz/system "0.4.3"]
                 [org.clojure/tools.namespace "0.3.0" :exclusions [org.clojure/java.classpath]]
                 [http-kit "2.3.0"]
                 [lambdaisland/garden-watcher "0.3.3"]
                 [hiccup "2.0.0-alpha1"]
                 [org.clojure/data.json "0.2.6"]
                 [clojure.java-time "0.3.2"]
                 [prone "1.6.4"]
                 [com.google.guava/guava "23.5-jre"]
                 [aero "1.1.3"]
                 [lambdaisland/repl-tools "0.1.0"]
                 [clj-http "3.10.0"]
                 [org.julienxx/clj-slack "0.6.3"]
                 [reloaded.repl "0.2.4"]
                 [instaparse "1.4.10"]
                 [com.cemerick/url "0.1.1"]
                 [enlive "1.1.6"]
                 [bidi "2.1.6"]]

  :plugins [[lein-environ "1.1.0"]]

  :min-lein-version "2.6.1"

  :source-paths ["src"]

  :test-paths ["test"]

  :uberjar-name "clojurians-log.jar"

  :main clojurians-log.application

  ;; nREPL by default starts in the :main namespace, we want to start in `user`
  ;; because that's where our development helper functions like (go) and
  ;; (browser-repl) live.
  :repl-options {:init-ns user}

  :aliases {"prep" ["garden"] ;; used by the deployment script
            "garden" ["run" "-m" "garden-watcher.main" "clojurians-log.styles"]
            "test" ["run" "-m" "kaocha.runner"]}

  :profiles {:dev
             {:dependencies [[com.datomic/datomic-free "0.9.5697" :exclusions [org.clojure/tools.cli]]
                             [com.cemerick/pomegranate "1.1.0"]
                             [vvvvalvalval/scope-capture "0.3.2"]
                             [alembic "0.3.2"]
                             [cheshire "5.8.1"]
                             [ring/ring-mock "0.4.0"]
                             [hickory "0.7.1"]
                             [lambdaisland/kaocha "RELEASE"]]

              :source-paths ["dev"]}

             :production
             {:aot :all
              :dependencies [;; Set through .lein/profiles.clj, see ansible scripts
                             ;; [com.datomic/datomic-pro ""]

                             ;; JDBC driver for Datomic+PostgreSQL
                             [org.postgresql/postgresql "42.2.6"]]}

             :uberjar
             {:prep-tasks ["compile" ["run" "-m" "garden-watcher.main" "clojurians-log.styles"]]
              :hooks []
              :omit-source true
              :aot :all}})
