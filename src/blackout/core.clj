(ns blackout.core
  (:require [clj-http.client :as http]
            [clojure.core.async :refer :all
             :exclude [map into reduce merge take partition partition-by]]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as c]
            [riemann.client :as r]))

(defonce ^:const +threads+ (.availableProcessors (Runtime/getRuntime)))

(defn make-uri
  [route]
  (str "http://" (env :switchboard-host) ":" (env :switchboard-port) route))

(defonce cookie-store (clj-http.cookies/cookie-store))

(defn post
  [url body]
  (http/post (make-uri url) {:content-type :json
                             :accept :json
                             :body (json/generate-string body)
                             :cookie-store cookie-store}))

(defn login
  [username password]
  (post "/api/v1/login" {:action "login"
                         :args {:username username
                                :password password}}))

(defrecord Root [account agents]
  c/Lifecycle
  (start [this]
    (if agents
      this
      (let [res (login (env :switchboard-admin-username)
                       (env :switchboard-admin-password))
            body (slurp (:body res))]
        (assert (== (:status res) 200) body)
        (assoc this
          :account (json/parse-string body true)
          :agents (repeatedly +threads+ #(agent nil))))))
  (stop [this]
    (if agents
      (do (shutdown-agents)
          (assoc this :agents nil))
      this)))
