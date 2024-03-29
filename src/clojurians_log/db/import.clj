(ns clojurians-log.db.import
  (:require [clojurians-log.time-util :as time-util]
            [java-time :as jt]
            [clojurians-log.datomic :as d])
  (:import java.io.BufferedReader))

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

(defmulti event->tx (juxt :type :subtype))

(defmethod event->tx :default [_]
  ;; return nil by default, this will let us skip events we don't (yet) care
  ;; about
  nil)

(defn message->tx [{:keys [ts text channel user thread_ts] :as message}]
  (when (= \C (first channel)) ;; ignore direct and private group, can be C, D, G
    (let [inst    (time-util/ts->inst ts)
          message #:message {:key      (message-key message)
                             :ts       ts
                             #_#_:inst (jt/to-java-date inst)
                             :day      (time-util/format-inst-day inst)
                             :text     text
                             :channel  [:channel/slack-id channel]
                             :user     [:user/slack-id user]}]

      (if (nil? thread_ts)
        message
        (let [thread-inst (time-util/ts->inst thread_ts)]
          (merge message
                 #:message{:thread-ts   thread_ts
                           :thread-inst (jt/to-java-date thread-inst)
                           :day         (time-util/format-inst-day thread-inst)}))))))

(defmethod event->tx ["message" nil] [message]
  (message->tx message))

(defmethod event->tx ["message" "message_deleted"] [{:keys [deleted_ts channel] :as message}]
  [(if d/cloud?
     :db/retractEntity
     :db.fn/retractEntity) [:message/key (message-key {:channel channel :ts deleted_ts})]])

(defmethod event->tx ["message" "message_changed"] [{:keys [message channel]}]
  (event->tx (assoc message :channel channel)))

(defmethod event->tx ["message" "thread_broadcast"] [message]
  (let [msg (message->tx message)]
    (when msg
      (assoc msg :message/thread-broadcast? true))))

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
             :name     name
             :created  created
             :creator  [:user/slack-id creator]})

(defn emoji->tx [[shortcode url]]
  #:emoji {:shortcode (name shortcode)
           :url       url})

;; TODO: deal with reactions on files (I guess this will depend on us actually dealing with files in the first place :))
;; {:reaction "joy", :event_ts "1521818444.000850", :item {:type "file", :file "F9W0B0LHM"}, :user "U06P56UUB", :item_user "U4E5W80P7", :type "reaction_added"}

(defn- reaction-entity [{:keys [user item reaction ts type]}]
  (let [msg-key (message-key item)]
    {:reaction/emoji {:emoji/shortcode reaction}
     :reaction/ts ts
     :reaction/user [:user/slack-id user]
     :reaction/message {:message/key msg-key}
     :reaction/key (str message-key "--" user "--" ts "--" reaction "--" type)}))

(defmethod event->tx ["reaction_added" nil] [{:keys [item] :as msg}]
  (when (and (:channel item) (:ts item)) ; exclude reactions on things other than messages
    (assoc
      (reaction-entity msg)
      :reaction/type "reaction_added")))

(defmethod event->tx ["reaction_removed" nil] [{:keys [item] :as msg}]
  (when (and (:channel item) (:ts item)) ; exclude reactions on things other than messages
    (assoc
      (reaction-entity msg)
      :reaction/type "reaction_removed")))

(defn lines-reducible [^BufferedReader rdr]
  (reify clojure.lang.IReduceInit
    (reduce [this f init]
      (try
        (loop [state init]
          (if (reduced? state)
            state
            (if-let [line (.readLine rdr)]
              (recur (f state line))
              state)))
        (finally (.close rdr))))))

(defn partition-messages
  "Transducer which chunks message events into partitions, so they can be
  transacted, but avoiding inconsitent transactions by never having the same
  message key twice in the same partition. Retraction events are always given
  their own one-element partition."
  [partition-size]
  (fn [rf]
    (let [part (volatile! (transient []))
          part-keys (volatile! (transient #{}))
          flush (fn [acc]
                  (when (> (count @part) 0)
                    (rf acc (persistent! @part))
                    (vreset! part (transient []))
                    (vreset! part-keys (transient #{}))))
          message-key #(or (:message/key %)
                           (:message/key (:reaction/message %)))
          append (fn [msg]
                   (conj! @part msg)
                   (conj! @part-keys (message-key msg)))]
      (fn
        ([]
         (rf))
        ([acc]
         (flush acc)
         (rf acc))
        ([acc x]
         (cond
           (vector? x)
           (do (flush acc)
               (rf acc [x]))

           (contains? @part-keys (:message/key x))
           (do
             (flush acc)
             (append x)
             (when (= (count @part) partition-size)
               (flush acc))
             acc)

           :else
           (do
             (append x)
             (when (= (count @part) partition-size)
               (flush acc))
             acc)))))))
