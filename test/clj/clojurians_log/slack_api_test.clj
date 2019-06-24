(ns clojurians-log.slack-api-test
  (:require [clojurians-log.slack-api :as slack]
            [clojurians-log.db.queries :as queries]
            [clojurians-log.test-helper :refer [test-conn]]
            [clojure.test :refer :all]
            [datomic.api :as d]))

(deftest import-channels!-test
  (testing "it handles renames"
    (let [conn (test-conn)]
      (with-redefs [slack/users (constantly [{:id "U03RZGPFT"}])
                    slack/channels (constantly [{:id "C03RZGPG1"
                                                 :name "announcements"
                                                 :creator "U03RZGPFT"
                                                 :created 1425231209}])]

        (slack/import-users! conn)
        (slack/import-channels! conn)
        (is (= (select-keys (queries/channel (d/db conn) "announcements")
                            [:channel/slack-id :channel/name])
               {:channel/slack-id "C03RZGPG1"
                :channel/name "announcements"})))

      (with-redefs [slack/users (constantly [{:id "U03RZGPFT"}])
                    slack/channels (constantly [{:id "C03RZGPG1"
                                                 :name "admin-announcements"
                                                 :creator "U03RZGPFT"
                                                 :created 1425231209}])]

        (slack/import-channels! conn)
        (is (= (select-keys (queries/channel (d/db conn) "admin-announcements")
                            [:channel/slack-id :channel/name])
               {:channel/slack-id "C03RZGPG1"
                :channel/name "admin-announcements"}))))))
