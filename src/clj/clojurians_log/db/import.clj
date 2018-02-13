(ns clojurians-log.db.import
  (:require [clojurians-log.time-util :as time-util]
            [java-time :as jt]))

(defn message-key
  "Message unique identifier.

  Slack doesn't expose a primary key for messages, instead the API refers to
  messages by timestamp. These are *not* globally unique. The Slack docs are
  phrased ambiguously, but they seem to imply that channel+ts is unique, so we
  store that. This will allow us to refer to messages later on, e.g. with lookup
  refs.

  Example: C073DKH9P--1512384953.000000"
  [{:keys [channel ts]}]
  {:pre [(string? ts) (string? channel)]}
  (str channel "--" ts))

(defmulti event->tx :subtype)

(defmethod event->tx :default [_]
  ;; return nil by default, this will let us skip events we don't (yet) care
  ;; about
  nil)

(defmethod event->tx nil [{:keys [ts text channel user] :as message}]
  (let [inst (time-util/ts->inst ts)]
    #:message {:key (message-key message)
               :ts ts
               #_#_:inst (jt/to-java-date inst)
               :day (time-util/format-inst-day inst)
               :text text
               :channel [:channel/slack-id channel]
               :user [:user/slack-id user]}))

(defn user->tx [{:keys [id name real_name is_admin is_owner profile]}]
  (let [{:keys [image_512 email first_name real_name_normalized image_48 image_192
                real_name image_72 image_24 avatar_hash team image_32 last_name
                display_name display_name_normalized]} profile]
    (->> (merge #:user {:slack-id id
                        :name name
                        :real-name real_name
                        :admin? is_admin
                        :owner? is_owner}
                #:user-profile {:email email,
                                :avatar-hash avatar_hash,
                                :image-32 image_32,
                                :image-24 image_24,
                                :image-192 image_192,
                                :image-48 image_48,
                                :real-name-normalized real_name_normalized,
                                :display-name-normalized display_name_normalized,
                                :display-name display_name,
                                :image-72 image_72,
                                :real-name real_name,
                                :image-512 image_512})
         (remove (comp nil? val))
         (into {}))))

(defn channel->tx [{:keys [id name created creator]}]
  #:channel {:slack-id id
             :name name
             :created created
             :creator [:user/slack-id creator]})
