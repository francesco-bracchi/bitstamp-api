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
;; `pusher` and `channel` functions work as "synchronous" procedure call,
;; while bind is used directly returning a (potentially infinite) async channel
;;
;; ## TODO
;;
;; + if a pusher or a channel is closed then the corresponding async channels have to be closed as well
;;   probably this implies having a bookeeper for connections/events.
;; + is it possible to have a function that maps an async channel to a lazy sequence?
;;
(ns bitstamp-api.async
  (:require [bitstamp-api.pusher :as pusher]
            [clj-json.core :as json]
            [clojure.core.async :as async]))

(def ^{:dynamic true
      :doc "Bitstamp key @ pusher.com"}
  *bitstamp-key*  "de504dc5763aeef9ff52")

(defn pusher 
  "opens a new connection with the pusher server and returns a channel, that will be fed with
  The Pusher object once the connection is ready"
  ([] (pusher *bitstamp-key*))
  ([key]
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
       ch)))

(defn channel 
  "creates a new Channel object, returns an async channel that will be fed with the channel 
  itself when the channel is actually set up."
  [pusher name]
  (let [ch (async/chan 0)]
    (pusher/channel pusher name 
                    (fn [channel]
                      (async/go (async/>! ch channel)
                                (async/close! ch))))
    ch))

(defn bind 
  "binds an event name to a pusher channel. Returns a channel that will be fed with the 
  subscribed events."
  ([channel event] (bind channel event 1))
  ([channel event size]
     (let [ch (async/chan size)]
       (pusher/bind channel event
                    (fn [channel-name event-name data]
                      (async/go (async/>! ch data))))
       ch)))
  
(defn data 
  ([channel-name event-name]
     (data channel-name event-name 1))
  ([channel-name event-name queue-size]
     (let [ch (async/chan 0)]
       (async/go
         (let [p (async/<! (pusher *bitstamp-key*))
               c (async/<! (channel p channel-name))
               d (bind c event-name queue-size)]
           (async/>! ch d)
           (async/close! ch)))
       ch)))
         
(defn ticker []
  (data "live_trades" "trade"))

(defn order-book []
  (data "order_book" "data"))

;; (defn -ticks []
;;   (let [r (atom [])]
;;     (async/go 
;;       (let [ch (async/<! (ticker))]
;;         (loop [d (async/<! ch)]
;;           (swap! r conj d)
;;           (recur (async/<! ch)))))
;;     r))

  
