(ns clojurians-log.db.schema)

(def message-schema
  [{:db/ident       :message/key
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string
    :db/unique      :db.unique/identity
    :db/doc         "Key consisting of channel id + ts. Useful for upserts and refs."}
   {:db/ident       :message/text
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Message text (markdown)"}
   {:db/ident       :message/channel
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Channel the message was posted in."
    :db/index       true}
   {:db/ident       :message/user
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "User who posted the message"}
   {:db/ident       :message/ts
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Message timestamp (seconds since epoch up to 6 decimals). Stored as string because it is used by slack as a kind of identifier. Unique per channel."}
   {:db/ident       :message/thread-ts
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "Thread parent message timestamp (seconds since epoch up to 6 decimals). Stored as string because it is used by slack as a kind of identifier. Unique per channel."
    :db/index       true}
   {:db/ident       :message/thread-inst
    :db/valueType   :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc         "Same as :message/thread-ts, but parsed to java.util.Date. This is lossy: Date has milisecond precision, ts has microseconds."}
   {:db/ident       :message/day
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc         "The day this message is categorized under, e.g. 2017-11-20."
    :db/index       true}
   {:db/ident       :message/thread-broadcast?
    :db/valueType   :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc         "Should this message be shown in the main channel, as well as in the thread, or only in the thread?"}])

(def event-schema
  [{:db/ident       :event/subtype
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :event/ts
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(def user-schema
  [{:db/ident       :user/slack-id
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "Internal user identifier used by slack. Alphanumeric starts with U."}
   {:db/ident       :user/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity
    :db/doc         "A user's public handle."}
   {:db/ident       :user/real-name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :user/admin?
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/boolean}
   {:db/ident       :user/owner?
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/boolean}
   {:db/ident       :user/profile
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one
    :db/doc         "Reference to a user's profile which contains email, avatar, display name, etc."}])

(def user-profile-schema
  [{:db/ident       :user-profile/email
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/first-name
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/last-name
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/display-name
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/display-name-normalized
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/real-name
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/real-name-normalized
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/avatar-hash
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/title
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/image-original
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/image-24
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/image-32
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/image-48
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/image-72
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/image-192
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}
   {:db/ident       :user-profile/image-512
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}])

(def channel-schema
  [{:db/ident       :channel/slack-id
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}
   {:db/ident       :channel/name
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}
   {:db/ident       :channel/created
    :db/valueType   :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident       :channel/creator
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def emoji-schema
  [{:db/ident       :emoji/shortcode
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}
   {:db/ident       :emoji/url
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}])

(def reaction-schema
  [{:db/ident       :reaction/type
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :reaction/emoji
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :reaction/ts
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one}
   {:db/ident       :reaction/user
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :reaction/message
    :db/valueType   :db.type/ref
    :db/cardinality :db.cardinality/one}
   {:db/ident       :reaction/key
    :db/doc         "Unique key for a given reaction event, so inserts are idempotent."
    :db/valueType   :db.type/string
    :db/cardinality :db.cardinality/one
    :db/unique      :db.unique/identity}])

(def full-schema
  (concat message-schema
          event-schema
          user-schema
          user-profile-schema
          channel-schema
          emoji-schema
          reaction-schema))
