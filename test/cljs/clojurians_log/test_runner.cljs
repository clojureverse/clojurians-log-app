(ns clojurians-log.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [clojurians-log.core-test]
   [clojurians-log.common-test]))

(enable-console-print!)

(doo-tests 'clojurians-log.core-test
           'clojurians-log.common-test)
