(defproject bitstamp-api "0.1.0.beta"
  :description "FIXME: write description"
  :url "https://github.com/francesco-bracchi/bitstamp-api"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.10"]
                 [clj-json "0.5.3"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [com.pusher/pusher-java-client "0.3.1"]
                 ]
  :plugins[[lein-marginalia "0.7.1"]]
  :profiles {:uberjar {:aot :all}}
)

