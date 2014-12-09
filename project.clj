(defproject blackout "0.1.0-SNAPSHOT"
  :description ""
  :url "https://github.com/VitalLabs/blackout"
  :license {:name "Proprietary"
            :url "http://vitallabs.co/"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/test.check "0.6.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [riemann-clojure-client "0.2.12"]
                 [clj-http "1.0.1"]
                 [cheshire "5.3.1"]
                 [environ "1.0.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.7"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.7"]]
                   :source-paths ["dev"]
                   :env {:switchboard-host "127.0.0.1"
                         :switchboard-port 8080}}}
  :plugins [[lein-environ "1.0.0"]])
