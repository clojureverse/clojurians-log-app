# Clojurians Log App [![CircleCI](https://circleci.com/gh/clojureverse/clojurians-log-app.svg?style=svg)](https://circleci.com/gh/clojureverse/clojurians-log-app)

Clojure web app that serves up Slack chat history. This app was created to
replace the old scripts that generated the site at
[clojurians-log.clojureverse.org](https://clojurians-log.clojureverse.org). This
app isn't live yet, but you can see it in action at
[clojurians-log-staging.clojureverse.org](http://clojurians-log-staging.clojureverse.org).

Besides serving its utilitarian function of serving logs, this also serves as an
example open-source Clojure web app. It's small enough to easily understand and
contribute to, while at the same time having a real-world use. This makes it a
perfect match for people who want to get started with Clojure open source
development. Together we can learn from each other, and make this an example of
good Clojure practices.

## Contributing

All kinds of contributions are welcome, code, documentation, tests, bug reports,
... We especially welcome first time contributors!

There are still many small things that need doing, have a look at the
[issues](https://github.com/clojureverse/clojurians-log-app/issues) if you're
curious to get started.

This project mostly follows the
[Clojure Community Style Guide](https://github.com/bbatsov/clojure-style-guide),
but also has some
[guidelines of its own](https://github.com/clojureverse/clojurians-log-app/blob/master/docs/STYLE.md).

## Running the app

To run the app on your own laptop, you should first grab the [demo
data](https://github.com/clojureverse/clojurians-log-demo-data), so you have
some Slack history to look at.

``` shell
git clone https://github.com/clojureverse/clojurians-log-demo-data.git
```

Now you can start a REPL, import the data, and start the app:

``` clojure
clj -A:dev:datomic-free
user=> (go)
...
Started clojurians-log on http://localhost:4983
:started
user=> (require '[clojurians-log.repl :as r])
user=> (r/load-demo-data! "/path/to/clojurians-log-demo-data")
/path/to/clojurians-log-demo-data/2018-02-01.txt
/path/to/clojurians-log-demo-data/2018-02-02.txt
/path/to/clojurians-log-demo-data/2018-02-03.txt
...

Note for WSL users - please enter the below command in your terminal before running any of the above commands:

```
export CLOJURIANS_LOG_HOST="0.0.0.0"
```

You can see it in action at [http://localhost:4983](http://localhost:4983), or start a browser with

```
user=> (browse)
```

## Some tips on development

The code is roughly split in three parts: queries, views, and routes (you can
think of it as MVC if you like). So the main namespaces to look at are

- clojurians-log.views
- clojurians-log.routes
- clojurians-log.db.queries

These are all under `src/clj`.

In `user.clj` you'll find some useful helpers for use during development, so
have a look at what's there!


``` clojure
user> (db)                               ;; Datomic db
user> (conn)                             ;; Datomic connection
user> (add-dependency [foo/bar "1.2.3"]) ;; Add a dependency without having to restart
user> (reset)                            ;; Reload modified namespaces and restart the app
user> (reset-all)                        ;; Reload all namespaces and restart the app
user> (last-request)                     ;; See the last ring request handled
user> (last-response)                    ;; See the last response the app generated
```

### Tests

We're using [Kaocha](https://github.com/lambdaisland/kaocha/) for our testing needs.

You can run the tests by running the following command from the project root directory:

```
./bin/kaocha
```

You can also run tests from the repl. For example to run the tests inside `test/clojurians_log/views_test.clj`:

```
(require '[kaocha.repl :as k])
(k/run 'clojurians-log.views-test)
```

## License

Copyright © 2018-2020 Brasseur and contributors.

Distributed under the Mozilla Public License version 2.0.
