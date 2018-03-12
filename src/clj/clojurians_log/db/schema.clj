(ns clojurians-log.db.schema)

(def message-schema
  [#:db{:ident       :message/key
        :cardinality :db.cardinality/one
        :valueType   :db.type/string
        :unique      :db.unique/identity
        :doc         "Key consisting of channel id + ts. Useful for upserts and refs."}
   #:db{:ident       :message/text
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Message text (markdown)"}
   #:db{:ident       :message/channel
        :valueType   :db.type/ref
        :cardinality :db.cardinality/one
        :doc         "Channel the message was posted in."
        :index       true}
   #:db{:ident       :message/user
        :valueType   :db.type/ref
        :cardinality :db.cardinality/one
        :doc         "User who posted the message"}
   #:db{:ident       :message/ts
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Message timestamp (seconds since epoch up to 6 decimals). Stored as string because it is used by slack as a kind of identifier. Unique per channel."}
   #_#:db{:ident       :message/inst
          :valueType   :db.type/instant
          :cardinality :db.cardinality/one
          :doc         "Same as :message/ts, but parsed to java.util.Date. This is lossy: Date has milisecond precision, ts has microseconds."}
   #:db{:ident       :message/thread-ts
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "Thread parent message timestamp (seconds since epoch up to 6 decimals). Stored as string because it is used by slack as a kind of identifier. Unique per channel."}
   #:db{:ident       :message/thread-inst
        :valueType   :db.type/instant
        :cardinality :db.cardinality/one
        :doc         "Same as :message/thread-ts, but parsed to java.util.Date. This is lossy: Date has milisecond precision, ts has microseconds."}
   #:db{:ident       :message/day
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :doc         "The day this message is categorized under, e.g. 2017-11-20."
        :index       true}
   #_#:db{:ident       :message/team
          :valueType   :db.type/ref
          :cardinality :db.cardinality/one}
   #_#:db{:ident       :message/source-team
          :valueType   :db.type/ref
          :cardinality :db.cardinality/one}])

(def event-schema
  [#:db{:ident       :event/subtype
        :valueType   :db.type/string
        :cardinality :db.cardinality/one}
   #:db{:ident       :event/ts
        :valueType   :db.type/string
        :cardinality :db.cardinality/one}])

(def user-schema
  [#:db{:ident       :user/slack-id,
        :valueType   :db.type/string,
        :cardinality :db.cardinality/one,
        :unique      :db.unique/identity,
        :doc         "Internal user identifier used by slack. Alphanumeric, starts with U."}
   #:db{:ident       :user/name,
        :valueType   :db.type/string,
        :cardinality :db.cardinality/one,
        :unique      :db.unique/identity,
        :doc         "A user's public handle."}
   #:db{:ident       :user/real-name,
        :valueType   :db.type/string,
        :cardinality :db.cardinality/one}
   #:db{:ident       :user/admin?,
        :cardinality :db.cardinality/one,
        :valueType   :db.type/boolean}
   #:db{:ident       :user/owner?,
        :cardinality :db.cardinality/one,
        :valueType   :db.type/boolean}
   #:db{:ident       :user/profile,
        :valueType   :db.type/ref,
        :cardinality :db.cardinality/one,
        :doc         "Reference to a user's profile, which contains email, avatar, display name, etc."}])

(def user-profile-schema
  [#:db{:ident       :user-profile/email
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/first-name
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/last-name
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/display-name
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/display-name-normalized
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/real-name
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/real-name-normalized
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/avatar-hash
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/title
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/image-original
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/image-24
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/image-32
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/image-48
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/image-72
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/image-192
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}
   #:db{:ident       :user-profile/image-512
        :cardinality :db.cardinality/one
        :valueType   :db.type/string}])

(def channel-schema
  [#:db{:ident       :channel/slack-id
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :unique      :db.unique/identity}
   #:db{:ident       :channel/name
        :valueType   :db.type/string
        :cardinality :db.cardinality/one
        :unique      :db.unique/identity}
   #:db{:ident       :channel/created
        :valueType   :db.type/long
        :cardinality :db.cardinality/one}
   #:db{:ident       :channel/creator
        :valueType   :db.type/ref
        :cardinality :db.cardinality/one}])

(def full-schema
  (concat message-schema
          event-schema
          user-schema
          user-profile-schema
          channel-schema))
