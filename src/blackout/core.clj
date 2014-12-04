(ns blackout.core
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [clojure.core.async :refer :all
             :exclude [map into reduce merge take partition partition-by]]
            [cheshire.core :as json]))

(def ^:dynamic *host* "localhost")
(def ^:dynamic *port* 8080)

(defn make-uri
  [route]
  (str "http://" *host* *port* route))

(defn post
  [route body]
  (http/post (make-uri route)
             {:content-type :json
              :accept :json
              :body (json/generate-string body)}))

(defn create-account
  []
  (post "/api/v1/accounts" {:action "create"
                            :args {}
                            :groups {}
                            :password ""}))

(defn -main
  [& {:keys [host port]}]
  (binding [*host* host
            *port* port]
    ))


