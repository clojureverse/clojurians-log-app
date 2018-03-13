These files are mini datomic databases that you can use in your test cases, see
`clojurians-log.test-helper/test-db`

They generally contain a specific selection of channels, users, and messages.


## `two-channels-two-days.edn`

All messages in `#clojure` and `#clojurescript` for 2018-02-02 and 2018-02-03,
plus the users who posted in those channels on those days.

## `quiet-channels.edn`

A number of quiet channels, to test cases where a channel does not have messages
on a given day, for days 2018-02-01 through 2018-02-04.

`#jobs`, `#keechma`, `#reitit`, `#yada`, `#dirac`
