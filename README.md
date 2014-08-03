# Bitstamp API

A Clojure library designed to connect to Bitstamp API.

** USE AT YOUR OWN RISK **

## HTTP API

HTTP APIs are provided by the functions in `bitstamp-api.http` namespace.
for a full description of the api see the [Bitstamp page][1]

### Free API

example:          

    (require ['bitstamp-api.http :as 'bs])
    (bs/ticker) ;; returns the current ticker
    
    ;; {:ask 584.2, 
    ;;  :low 578.21, 
    ;;  :volume 3701.42506987, 
    ;;  :vwap 587.15, 
    ;;  :bid 582.43, 
    ;;  :timestamp #inst "2014-08-03T08:48:21.000-00:00", 
    ;;  :last 584.2, 
    ;;  :high 594.97}

Available operations

+ ticker
+ order-book
+ transactions
+ eur-usd

### Account specific API

These API require that the request is signed by a specific account, 
In order to use these calls you have to have a bitcoin account,  
login and access the *Security > API Access* menu and add a new API key.

Example:

    (require ['bitstamp-api.http :as 'bs])
    (bs/with-account {:key "xxxxxxxxxxxxxxx"
                      :secret "yyyyyyyyyyyyyyyy"
                      :client-id 12345678}
      (bs/balance))

Available operations

+ balance
+ user-transactions
+ open-orders
+ cancel-order!
+ buy-limit-order!
+ sell-limit-order!
+ withdrawal-requests
+ bitcoin-withdrawal 
+ bitcoin-deposit-address
+ unconfirmed-bitcoin-deposits 
+ ripple-deposit-address

## Push Notifications API

Bitstamp relies on [Pusher][3] service for pushing data to a client. 
Fortunately Pusher provides a java library.

In any case the namespace `bistamp-api.async` provides an interface that
is usable with `core.async`.

Example:

    (require ['bitstamp-api.async :as 'ba])
    (require ['clojure.core.async :as 'async])

    (def r (atom []))             

    (async/go 
     (let [ch (async/<! (ba/ticker))]
       (loop [d (async/<! ch)]
         (swap! r conj d)
         (recur (async/<! ch)))))
    
    @r
    ;; get a vector containing past exchanges (up to the `ba/ticker` call)

Available channels

+ ticker
+ order-book

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[1]: https://www.bitstamp.net/api/
[2]: https://www.bitstamp.net/websocket/
[3]: http://pusher.com/