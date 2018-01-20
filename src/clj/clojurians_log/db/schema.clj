(ns clojurians-log.db.schema)

(def slack-meta-schema
  [#:db{:ident :slack/ts
        :valueType :db.type/string
        :cardinality :db.cardinality/one}])

(def message-schema
  [#:db{:ident :message/text
        :valueType :db.type/string
        :cardinality :db.cardinality/one}
   #:db{:ident :message/user
        :valueType :db.type/ref
        :cardinality :db.cardinality/one}
   #_#:db{:ident :message/team
          :valueType :db.type/ref
          :cardinality :db.cardinality/one}
   #_#:db{:ident :message/source-team
          :valueType :db.type/ref
          :cardinality :db.cardinality/one}])

(def event-schema
  [#:db{:ident :event/type
        :valueType :db.type/string
        :cardinality :db.cardinality/one}])

(def user-schema
  [{:db/ident :user/slack-id
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :user/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :user/real-name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident :user/is-admin
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/boolean}
   {:db/ident :user/is-owner
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/boolean}
   {:db/ident :user/profile
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def user-profile-schema
  [#:db{:ident :user-profile/email
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/first-name
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/real-name-normalized
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/image-48
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/image-192
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/real-name
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/image-original
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/image-72
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/image-24
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/avatar-hash
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/title
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/team
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/image-32
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/last-name
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/display-name
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}
   #:db{:ident :user-profile/display-name-normalized
        :cardinality :db.cardinality/one
        :valueType :db.type/boolean}])

(def full-schema
  (concat
   slack-meta-schema
   message-schema
   event-schema
   user-schema
   user-profile-schema))
