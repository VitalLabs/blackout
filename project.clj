(defproject blackout "0.2.0-SNAPSHOT"
  :description "A distributed load testing infrastructure"
  :url "https://github.com/VitalLabs/blackout"
  :license {:name "Proprietary"
            :url "http://vitallabs.co/"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.391"]
                 [org.clojure/test.check "0.9.0"]
                 [org.slf4j/slf4j-log4j12 "1.7.7"]
                 [log4j/log4j "1.2.17" :exclusions [javax.mail/mail
                                                    javax.jms/jms
                                                    com.sun.jmdk/jmxtools
                                                    com.sun.jmx/jmxri]]                 
                 [org.clojure/tools.logging "0.3.1"]
                 [com.stuartsierra/component "0.2.2"]
                 [environ "1.0.0"]
                 [cheshire "5.3.1"]
                 [clj-time "0.11.0"]
                 [http-kit "2.1.19"]
                 [com.hazelcast/hazelcast-all "3.7.2"]
                 [org.clojurecast/clojurecast "0.1.3"]
                 [clj-gatling "0.8.3"]
                 [riemann-clojure-client "0.4.2"]
                 #_[riemann-jvm-profiler "0.1.0"]]
;  :main ^:skip-aot blackout.main
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.7"]]
                   :source-paths ["dev"]
                   :env {:switchboard-host "127.0.0.1"
                         :switchboard-port 8080
                         :riemann-host "127.0.0.1"
                         :riemann-port 5555}}
             :uberjar {:aot :all}}
  :plugins [[lein-environ "1.0.0"]])
