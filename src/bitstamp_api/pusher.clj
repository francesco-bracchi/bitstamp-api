(ns bitstamp-api.pusher)

(def ^:dynamic *pusher* nil)

(def ^:dynamic *channel* nil)

(defn connection-listener [callback]
  (reify com.pusher.client.connection.ConnectionEventListener
    (onConnectionStateChange [this change] 
      (callback  {:change true
                  :from (-> change .getCurrentState keyword)
                  :to (-> change .getPreviousState keyword)}))
    (onError [this message code exception] 
      (callback {:error true
                 :message message 
                 :code (keyword code)
                 :exception exception}))))

(defn channel-listener [callback]
  (reify com.pusher.client.channel.ChannelEventListener
    (onSubscriptionSucceeded [this channel-name]
      (callback channel-name))))

(defn subscription-listener [callback]
  (reify com.pusher.client.channel.SubscriptionEventListener
    (onEvent [this channel-name event-name data]
      (callback channel-name event-name data))))

(defn pusher [key]
  (new com.pusher.client.Pusher key))

(def ALL com.pusher.client.connection.ConnectionState/ALL)

(defn connect 
  ([callback] (connect callback *pusher*))
  ([callback pusher]
     (.connect (connection-listener callback) ALL)))

(defn disconnect 
  ([] (disconnect *pusher*))
  ([pusher] (.disconnect pusher)))

(defn channel 
  ([channel-name callback] (channel channel-name *pusher*))
  ([channel-name callback pusher]
     (-> pusher (.subscribe channel-name (channel-listener callback)))))

(defn bind 
  ([event callback] (bind event callback *channel*))
  ([event callback channel]
     (.bind channel (subscription-listener callback))))

