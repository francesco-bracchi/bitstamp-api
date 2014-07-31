(ns bitstamp-api.pusher
  (:require [clj-json.core :as json]))

(def keyword->state
  {:all com.pusher.client.connection.ConnectionState/ALL
   :connecting com.pusher.client.connection.ConnectionState/CONNECTING
   :connected com.pusher.client.connection.ConnectionState/CONNECTED
   :disconnecting com.pusher.client.connection.ConnectionState/DISCONNECTING
   :disconnected com.pusher.client.connection.ConnectionState/DISCONNECTED
   })

(def state->keyword 
  (into {} (map (fn [[a b]] [b a]) keyword->state)))

(defn connection-listener [pusher callback]
  (reify com.pusher.client.connection.ConnectionEventListener
    (onConnectionStateChange [this change]
      (println "connection-state-change")
      (callback pusher :change
                {:current (-> change .getCurrentState state->keyword)
                 :previous (-> change .getPreviousState state->keyword)}))
    (onError [this message code exception] 
      (callback pusher :error
                {:message message 
                 :code (keyword code)
                 :exception exception}))))

(defn channel-listener [ch callback]
  (reify com.pusher.client.channel.ChannelEventListener
    (onSubscriptionSucceeded [this channel-name]
      (callback @ch))))

(defn subscription-listener [callback]
  (reify com.pusher.client.channel.SubscriptionEventListener
    (onEvent [this ^:string channel-name ^:string event-name ^:string data]
      (callback channel-name event-name (json/parse-string data true)))))


(defn new-pusher 
  [key] (new com.pusher.client.Pusher key))

(defn connect 
  ([pusher callback] 
     (connect pusher callback :connected))
  ([pusher callback & states]
     (.connect pusher (connection-listener pusher callback) (into-array (map keyword->state states)))))

(defn disconnect 
  [pusher] (.disconnect pusher))

(defn pusher [key callback]
  (let [pusher (new-pusher key)]
    (connect pusher callback)))

(defn channel
  [p channel-name callback]
  (let [ch (atom nil)]
    (swap! ch (fn [_] 
                (.subscribe p (name channel-name) (channel-listener ch callback) (into-array String []))))
    @ch))

(defn bind
  [channel event callback]
  (.bind channel (name event) (subscription-listener callback)))
