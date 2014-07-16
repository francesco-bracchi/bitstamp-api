(ns bitstamp-api.async
  (:require [bitstamp-api.pusher :as pusher])
  (:use clojure.core.async))

(defn pusher [key]
  (let [ch (chan 0)
        pusher (pusher/pusher key)]
    (pusher/connect #(>! ch pusher) pusher)))

(defn channel [pusher channel-name]
  (let [ch (chan 0)
        pc (pusher/channel channel-name #(>! ch @pc) pusher)]
    {:channel ch
     :pusher pc}))

(defn bind
  ([channel event]
     (bind channel event nil))
  ([channel event channel-size]
     (let [ch (chan channel-size)
           event (if (vector? event) (into-array event) event)]
       (pusher/bind event #(>! ch %) channel)
       ch)))

;; (go (let [p (<! (pusher "key"))
;;           c (<! (channel p "channel"))
;;           d (<! (bind channel "event-name"))]
;;       (loop []
;;         (let [v (<! d)]
;;           (do-something-with-d d)
;;           (when (continue?) (recur))))
;;       (.disconnect c)))

;; (go (let [p (<! (pusher "key"))
;;           c (<! (channel p "channel"))
;;           d (channel->lazy-sequence (<! (bind channel "event-name")))]
;;       (loop []
;;         (let [v (<! d)]
;;           (do-something-with-d d)
;;           (when (continue?) (recur))))
;;       (.disconnect c)))
