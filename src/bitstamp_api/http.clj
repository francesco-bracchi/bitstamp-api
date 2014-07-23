(ns bitstamp-api.http
  (:require [org.httpkit.client :as http]
            [clojure.string :as string]
            [clj-json.core :as json]
            ))

(def ^{:dynamic true
      :doc "Bitstamp URL"}
  *bitstamp-url* "https://www.bitstamp.net/api/")

(def ^{:dynamic true
      :doc "Bitstamp protocol version"}
  *version* "1.0")

;; ## Bitstamp API keys
;;
;; These 3 dynamic variables are the ones used to interact with the private API.

(def ^{:dynamic true
       :doc "Bitstamp API key"}
  *key* (System/getenv "BITSTAMP_KEY"))

(def ^{
       :dynamic true
       :doc "Bitstamp API secret"}
  *secret* (System/getenv "BITSTAMP_SECRET"))

(def ^{
       :dynamic true
       :doc "Client identifier, Client ID or Customer ID"}
  *client-id* (System/getenv "BITSTAMP_CLIENTID"))

(defn bitstamp-action-url 
  "generate the action url"
  [action]
  (str *bitstamp-url* (name action) "/"))

(defn nonce 
  "generate a nonce using the local machine timestamp"
  []
  (->(java.util.Date.) .getTime))

(defn new-secret-key [key mac]
    (javax.crypto.spec.SecretKeySpec. (.getBytes key) (.getAlgorithm mac)))

(defn toHexString [bytes]
  (apply str (map #(format "%02X" %) bytes)))

(defn hmac-sha256 [message key]
  (let [mac (javax.crypto.Mac/getInstance "HMACSHA256")
        secret-key (new-secret-key key mac)]
    (-> (doto mac 
          (.init secret-key)
          (.update (.getBytes message)))
        .doFinal
        toHexString)))

(defn signature 
  "called with 1 parameter, that is nonce, create the signature according to 
   the bitstamp API.
   otherwise API keys has to be explicitely passed, and client id as well."
  ([nonce] (signature nonce *key* *secret* *client-id*))
  ([nonce key secret client-id]
     (-> (str nonce client-id key)
         (hmac-sha256 secret))))

(defn sign
  "adds nonce and signature to the params, params are 
   the params that will actually passed to the API"
  [params]
  (let [nonce (nonce)]
    (assert (string? *key*))
    (assoc params
      :nonce nonce
      :key *key*
      :signature (signature nonce))))

(defn json-parse 
  "transforms the response body from string to a clojure array/map" 
  [res]
  (assoc @res
    :body 
    (if (= (:status @res) 200)
      (json/parse-string (:body @res) true)
      nil)))

(defn check-status 
  "check that everything went ok"
  ([res] (check-status res (:status @res) (:error @res)))
  ([res status error]
     (cond
      error (throw error)
      ;; (not (= status 200)) (throw (new Error "wrong response code"))
      :else res)))

(defn hget 
  "utility function that calls bitstamp api with a GET request, 
   key is the api that is one of `#{:ticker :transactions :order_book ...}`
   params is a map that contains parameters to pass to the request.
   returns the response."
  ([key] (hget key {}))
  ([key params]
     (-> (bitstamp-action-url (name key))
         (http/get {:query-params params})
         check-status
         json-parse)))

(defn hpost
  "the equivalent of `hget` but with POST, used for authenticated requests"
  ([key] (hpost key {}))
  ([key params]
     (-> (bitstamp-action-url (name key))
         (http/post {:form-params (sign params)})
         json-parse
         )))

;; ## Json adaptors
;;
;; The json object returned by Bitstamp doesn't carry type information, 
;; so, I implemented a system that associate a type to a specific key.
;; if for example the key is `"timestamp"` and I want a Date instead, I'll do
;; 
;;     (transform :timestamp "123") -> (java.util.Date. (read-string "123"))
;; 
;; the function `(jsonify o)` applies `transform` to all the pairs that forms the 
;; `o` map.

(defn ident [x & rest] x)

(defmulti transform
  "this multi is used to transform the plain json object returned by Bitstamp 
   in a typed object, i.e. transforming timestamps in java.util.Date, numberical
   values stored as strings in numbers, etc"
  ident)

(defmethod transform :default
  [key val] val)

(defmethod transform :high 
  [key val] (read-string val))

(defmethod transform :last 
  [key val] (read-string val))

(defmethod transform :timestamp
  [key val] (-> val read-string (* 1000) java.util.Date.))

(defmethod transform :date 
  [key val] (-> val read-string (* 1000) java.util.Date.))

(defmethod transform :amount
  [key val] (read-string val))

(defmethod transform :price 
  [key val] (read-string val))

(defmethod transform :bid
  [key val] (read-string val))

(defmethod transform :volume
  [key val] (read-string val))

(defmethod transform :low
  [key val] (read-string val))

(defmethod transform :ask
  [key val] (read-string val))

(defmethod transform :buy
  [key val] (read-string val))

(defmethod transform :sell
  [key val] (read-string val))

(defmethod transform :usd_available
  [key val] (read-string val))

(defmethod transform :usd_balance
  [key val] (read-string val))

(defmethod transform :usd_reserved
  [key val] (read-string val))

(defmethod transform :btc_available
  [key val] (read-string val))

(defmethod transform :btc_balance
  [key val] (read-string val))

(defmethod transform :btc_reserved
  [key val] (read-string val))

(defmethod transform :fee
  [key val] (read-string val))

(defmethod transform :btc_usd
  [key val] (read-string val))

(defmethod transform :btc
  [key val] (read-string val))

(defmethod transform :usd
  [key val] (read-string val))

(defmethod transform :datetime
  [key val] 
  (-> (new java.text.SimpleDateFormat "yyyy-mm-dd kk:mm:ss")
      (.parse val)))

(defn extend-transform 
  "from time to time, for keys that has different meaning in different context (namely :type) 
  it is useful having an ad-hoc function builder that adds ad-hoc transformers"
  ([key0 fun] (extend-transform key0 fun transform))
  ([key0 fun trans0]
  (fn [key val]
    (if (= key key0)
      (fun val) 
      (trans0 key val)))))

;; (defmethod transform :type
;;   [key val] (case val 
;;               0 :deposit
;;               1 :withdrawal
;;               2 :trade))
               
(defn jsonify
  "applies type transformations to the obj object"
  ([obj] (jsonify obj transform))
  ([obj trans]
     (let [ks (keys obj)
           vs (vals obj)]
       (zipmap ks (map trans ks vs)))))

;; ## exports 
;; these are the API exports.

(defn ticker 
  "API: get the current ticker, see https://www.bitstamp.net/api/"
  [] (-> :ticker hget :body (jsonify transform)))

(defn order-book
  "API: get the order book. group? set to true groups orders for the same price, see https://www.bitstamp.net/api/"
  ([] (order-book true))
  ([group?] 
     (-> :order_book
         (hget {:group (if group? 1 0)})
         :body 
         (jsonify transform))))

(defn transactions 
  "API: get transactions timeframe can be :hour or :minute. See https://www.bitstamp.net/api/"
  ([] (transactions :hour))
  ([timeframe] 
     (assert (contains? #{:minute :hour} timeframe))
     (map #(jsonify %1 transform)
          (-> :transactions
              (hget {:time (name timeframe)})
              :body))))

(defn eur-usd
  "get euro to usd conversion rate. See https://www.bitstamp.net/api/"
  [] (-> :eur_usd hget :body (jsonify transform)))

;; # Private functions

(defn balance
  "get the balance of the current account"
  []
  (-> :balance hpost :body (jsonify transform)))

(defn user-transactions
  "get user transactions, parameters can be passed as keys (that are :limit :offset :order)
  i.e. (user-transactions :limit 10 :offest 10 :sort :asc)"
  [& {:keys [:limit :offset :sort] :as params}]
  (map #(jsonify % (extend-transform :type {0 :deposit 1 :withdrawal 2 :market-trade}))
       (-> :user_transactions 
           (hpost (if (:sort params)
                    (assoc params :sort (name (:sort params)))
                    params))
           :body)))

(defn open-orders 
  "get the open orders of the current user"
  []
  (map #(jsonify % (extend-transform :type {0 :buy 1 :sell}))
       (-> :open-orders
           hpost
           :body)))

(defn cancel-order! 
  "cancel an order id is the order id"
  [id]
  (-> :cancel_order
      (hpost {:id id})
      :body
      json/parse-string))
       
(defn buy-limit-order! 
  "creates a new buy order limited to a specific price parameters are :amount and :price
  returns the order object"
  [& {:keys [:amount :price] :as params}]
  (-> :buy_limit_order
      (hpost params)
      :body
      (jsonify (extend-transform :type {0 :buy 1 :sell}))))

(defn sell-limit-order!
  "creates a new sell order limited to a specific price parameters are :amount and price
  returns the order object"
  [& {:keys [:amount :price] :as params}]
  (-> :sell_limit_order
      (hpost params)
      :body 
      (jsonify (extend-transform :type {0 :buy 1 :sell}))))

(defn withdrawal-requests 
  []
  (map #(jsonify % (extend-transform 
                    :type {0 :sepa 1 :bitcoin 2 :wire-transfer}
                    (extend-transform
                     :status {0 :open 1 :in-process 2 :finished 3 :canceled 4 :failed})))
       (-> :withdrawal_requests
           hpost
           :body)))

(defn bitcoin-withdrawal 
  [& {:keys [:amount :address] :as params}]
  (-> :bitcoin_withdrawal
      (hpost params)
      :body 
      (jsonify transform)))

(defn bitcoin-deposit-address []
  (-> :bitcoin_deposit_address
      hpost 
      :body 
      (jsonify transform)))

(defn unconfirmed-bitcoin-deposits []
  (-> :unconfirmed_btc
      hpost 
      :body 
      (jsonify transform)))

(defn bitcoin-withdrawal 
  [& {:keys [:amount :address :currency] :as params}]
  (-> :bitcoin_withdrawal
      (hpost params)
      :body 
      (jsonify transform)))

(defn ripple-deposit-address []
  (-> :ripple_deposit_address
      hpost 
      :body 
      (jsonify transform)))

;; (def ^{:dynamic true
;;        :doc "Pusher key of for the bitstamp exchange"}
;;   *pusher-api-key* "de504dc5763aeef9ff52")

;; (def pusher
;;   ([] (pusher *pusher-api-key*))
;;   ([key] (pusher/connect key)))

;; (def ^:dynamic *order-book* 
;;   {:channel "order_book"
;;    :events ["data"]
;;    })

;; (defn order-book [pusher]
;;   (pusher/subscribe pusher 
;;                     (:channel *order-book*) 
;;                     (:events *order-book*)))

;; (defn call-with-account [acc thunk]
;;   (binding [*key* (:key acc)
;;             *secret* (:secret acc)
;;             *client-id* (:client-id acc)]
;;     (thunk)))

;; (defmacro with-account [account & body]
;;   `(call-with-account acc (fn [] ~@body)))




