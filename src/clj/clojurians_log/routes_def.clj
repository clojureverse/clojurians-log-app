(ns clojurians-log.routes-def
  "Contains only the route definitions.

  This breaks circular dependency between clojurians-log.routes and clojurians-log.views when
  both needs access to the definition data."
  (:require [bidi.bidi :as bidi]))

(def routes
  ["/" {"healthcheck" (fn [endpoint req]
                        {:headers {"Content-Type" "text/plain"}
                         :body "OK"})

        ;; Note that the symbol here needs to match some var that clojure-log.routes has access to.
        ;; When dispatching on the route, the symbol will be resolved symbol->var->func.
        "" (-> 'index-route (bidi/tag :index))

        [:channel] (-> 'channel-history-route (bidi/tag :channel-history))
        [:channel "/" :date ".html"] (-> 'log-route (bidi/tag :log-old-url))
        [:channel "/" :date]         (-> 'log-route (bidi/tag :log))
        [:channel "/" :date "/" :ts] (-> 'log-route (bidi/tag :log-target-message))}])
