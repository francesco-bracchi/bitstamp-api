(ns bitstamp-api.pusher)

(defn connection-listener [callback]
  (reify com.pusher.client.connection.ConnectionEventListener
      (println "connection-state-change")
      (callback :change
                :from (-> change .getCurrentState keyword)
                :to (-> change .getPreviousState keyword)))
    (onError [this message code exception] 
      (callback :error
                :message message 
                :code (keyword code)
                :exception exception))))

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

(def states
  {:all com.pusher.client.connection.ConnectionState/ALL
   :connecting com.pusher.client.connection.ConnectionState/CONNECTING
   :connected com.pusher.client.connection.ConnectionState/CONNECTED
   :disconnecting com.pusher.client.connection.ConnectionState/DISCONNECTING
   :disconnected com.pusher.client.connection.ConnectionState/DISCONNECTED
   })

(defn connect 
  ([pusher]
     (.connect pusher))
  ([pusher callback] 
     (connect pusher callback :connected))
  ([pusher callback & ss]
     (println (vec (into-array (map states ss))))
     (.connect pusher (connection-listener callback) (into-array (map states ss)))))
  
(defn disconnect 
  [pusher] (.disconnect pusher))

(defn channel
  [pusher channel-name callback]
  (.subscribe pusher channel-name (channel-listener callback) (into-array String [])))

;; (defn bind
;;   ([channel event callback]
;;      (.bind channel event (subscription-listener callback))))

