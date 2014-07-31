(ns bitstamp-api.async
  (:require [bitstamp-api.pusher :as pusher]
            [clj-json.core :as json]
            [clojure.core.async :as async]))

;; # Async API
;; 
;; This is a thin wrapper around the Pusher library that makes use of clojure async.
;; 
;;     (async/go
;;       (let [pusher (async/<! (pusher "key"))
;;             channel (async/<! (channel pusher "channel"))
;;             events (bind channel "event")]
;;         (loop [c (async/<! events)]
;;           (do-something-with c)
;;           (recur (async/<! events)))))
;;
;; Mainly the `pusher` and `channel` functions work as "synchronous" builders
;; while bind is used directly returning a potentially infinite channel
;;
;; ## TODO
;;
;; + if a pusher channel is closed the corresponding async channels have to be closed as well
;; + provide an interface that transforms a channel in a lazy sequence

(defn pusher 
  "opens a new connection with the pusher server and returns a channel, that will be fed with
  The Pusher object once the connection is ready"
  [key]
  (let [ch (async/chan 0)]
    (pusher/pusher key 
                   (fn [pusher action data]
                     (when (and (= action :change) (= (:current data) :connected))
                       (async/go
                         (async/>! ch pusher)
                         (async/close! ch)))
                     (when (= action :error)
                       (async/go
                         (async/>! ch nil)
                         (async/close! ch)))))
    ch))

(defn channel 
  "creates a new Channel object, returns an async channel that will be fed with the channel 
  itself when the channel is actually setup"
  [pusher name]
  (let [ch (async/chan 0)]
    (pusher/channel pusher name 
                    (fn [channel]
                      (async/go (async/>! ch channel)
                                (async/close! ch))))
    ch))

(defn bind 
  "binds an event name to a pusher channel. returns a channel that will be fed with the 
  subscribed events"
  ([channel event] (bind channel event 1))
  ([channel event size]
     (let [ch (async/chan size)]
       (pusher/bind channel event
                    (fn [channel-name event-name data]
                      (async/go (async/>! ch data))))
       ch)))
