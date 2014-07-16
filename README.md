# bitstamp-api

A Clojure library designed to connect to Bitstamp API

## Usage

FIXME

### Free API
these API do not need an account to be called 

    (require ['bitstamp-api.core :as 'bitstamp]))
    (bitstamp:ticker) ;; returns the current ticker

### Account specific API
these API require that the request is signed by a specific account.

    (require ['bitstamp-api.core :as 'bitstamp]))
    (bistamp:with-account {:key "xxxxxxxxxxxxxxx"
                   :secret "yyyyyyyyyyyyyyyy"
                   :client-id 12345678}
      (bitstamp:balance))

### Push Notifications API

Bitstamp relies on pusher services for pushing data to the client

     (require ['bitstamp-api.pusher :as 'pusher])
     (def trade-events (bitstamp-trades))
     (<! (:channel trade-events)) ;; gets the first event
     (stop! 
     
## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
