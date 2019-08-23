;; This script is used as part of the Ansible provisioning of new servers. Do
;; not edit unless you are sure of what you are doing.
(use 'clojurians-log.repl)
(load-slack-data!)
(def result (load-files! (log-files)))
@(second result)
(prn result)
