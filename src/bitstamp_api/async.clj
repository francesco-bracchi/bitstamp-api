(ns bitstamp-api.async
  (:require [bitstamp-api.pusher :as pusher]
            [clj-json.core :as json]
            [clojure.core.async :as async]))

(defn pusher [key]
  (let [ch (async/chan)
        pusher (pusher/pusher key)]
    (pusher/connect pusher 
                    (fn [type & args]
                      (println "async/pusher")
                      (async/go
                        (case type
                          :change (async/>! ch pusher)
                          :error  (async/>! ch (assoc args :error true))))
                      (async/close! ch)))
    ch))
    

(defn channel [pusher channel-name]
  (let [ch (async/chan 0)
        pc (ref nil)]
    (ref-set pc (pusher/channel pusher
                                channel-name 
                                (fn [name] (async/go (async/>! ch @pc)))))
    ch))

(defn bind
  ([channel event]
     (bind channel event nil))
  ([channel event channel-size]
     (let [ch (async/chan channel-size)
           event (if (vector? event) (into-array event) event)]
       (pusher/bind event #(async/go (async/>! ch (json/parse-string %))) channel)
       ch)))

;; (go (let [p (<! (pusher "key"))
;;           c (<! (channel p "channel"))
;;           d (<! (bind channel "event-name"))]
;;       (loop []
;;         (let [v (<! d)]
;;           (do-something-with-d d)
;;           (when (continue?) (recur))))
;;       (.disconnect c)))

(async/go (let [p (async/<! (pusher "de504dc5763aeef9ff52"))
                _ (println 0)
                c (async/<! (channel p "order_book"))
                __ (println 0)
                d (async/<! (bind channel "data"))]
            
            (println 2)
            (loop [n 100]
              (println 3)
              (let [v (async/<! d)]
                (print "data: ")
                (println d)
                (when (> n 0) (recur (- n 1)))))
            (pusher/disconnect p)))
