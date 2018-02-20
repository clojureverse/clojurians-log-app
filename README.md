# Clojurians Log App

Clojure web app that serves up Slack chat history. This app was created to
replace the old scripts that generated the site at
[clojurians-log.clojureverse.org](https://clojurians-log.clojureverse.org). This
app isn't live yet, but you can see it in action at
[clojurians-log-staging.clojureverse.org](http://clojurians-log-staging.clojureverse.org).

Besides serving its utalitarian function of serving logs, this also serves as an
example open-source Clojure web app. It's small enough to easily understand and
contribute to, while at the same time having a real-world use. This makes it a
perfect match for people who want to get started with Clojure open source
development. Together we can learn from each other, and make this an example of
good Clojure practices.

There are still many small things that need doing, have a look at the
[issues](https://github.com/clojureverse/clojurians-log-app/issues) if you're
curious to get started.

## Running the app

To run the app on your own laptop, you should first grab the [demo
data](https://github.com/clojureverse/clojurians-log-demo-data), so you have
some Slack history to look at.

``` shell
git clone https://github.com/clojureverse/clojurians-log-demo-data.git
```

Now you can start a REPL, import the data, and start the app:

``` clojure
lein repl
user=> (go)
...
Started clojurians-log on http://localhost:4983
:started
user=> (use 'clojurians-log.repl)
user=> (load-demo-data! "/path/to/clojurians-log-demo-data")
/path/to/clojurians-log-demo-data/2018-02-01.txt
/path/to/clojurians-log-demo-data/2018-02-02.txt
/path/to/clojurians-log-demo-data/2018-02-03.txt
...
```

You can see it in action at [http://localhost:4983](http://localhost:4983)

## License

Copyright © 2018 Arne Brasseur and contributors.

Distributed under the Mozilla Public License version 2.0.

## Chestnut

Created with [Chestnut](http://plexus.github.io/chestnut/) 0.15.3-SNAPSHOT (6ef3c4bc).
