(ns blackout.core
  (:require [clj-http.client :as http]
            [clojure.core.async :refer :all
             :exclude [map into reduce merge take partition partition-by]]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as c]
            [riemann.client :as r]
            [riemann.codec :as codec]
            [riemann.jvm-profiler :as jvm]
            [clojure.string :as str]))

(defonce ^:const +threads+ (.availableProcessors (Runtime/getRuntime)))

(defonce cookie-store (clj-http.cookies/cookie-store))

(defonce riemann-client (r/tcp-client {:host (env :riemann-host)
                                       :port (env :riemann-port)}))

(defn send-event
  ([event] (r/send-event riemann-client event))
  ([event ack] (r/send-event riemann-client event ack)))

(defn make-uri
  [route]
  (str "http://" (env :switchboard-host) ":" (env :switchboard-port) route))

(defn wrap-riemann-trace
  [handler]
  (fn [req]
    (let [{:keys [status body request-time] :as res} (handler req)
          {:keys [server-time]} (:body res)
          route (str/replace (:url req) (re-pattern (make-uri "")) "")]
      (send-event {:server-time (str server-time)
                   :request-time (str request-time)
                   :route route})
      res)))

(defn wrap-json-body
  [handler]
  (fn [req]
    (update (handler req) :body json/parse-string true)))

(defn request
  ([url] (request :get url))
  ([method url] (request method url nil))
  ([method url body]
   (http/with-middleware (conj http/*current-middleware*
                               wrap-json-body
                               wrap-riemann-trace)
     (let [options {:content-type :json
                    :accept :json
                    :cookie-store cookie-store}]
       (case method
         :get (http/get (make-uri url) options)
         :post (let [body (json/generate-string body)]
                 (http/post (make-uri url) (assoc options :body body))))))))

(defn login
  [username password]
  (request :post "/api/v1/login" {:action "login"
                                  :args {:username username
                                         :password password
                                         :app 1}}))

(defrecord Root [account agents profiler]
  c/Lifecycle
  (start [this]
    (if agents
      this
      (let [res (login (env :switchboard-admin-username)
                       (env :switchboard-admin-password))
            body (:body res)]
        (assert (== (:status res) 200) body)
        (assoc this
          :account (:result body)
          :agents (repeatedly +threads+ #(agent nil))
          :profiler (jvm/start-global! {:host (env :riemann-host)
                                        :port (env :riemann-port)
                                        :prefix "switchboard"
                                        :load 0.05})))))
  (stop [this]
    (if agents
      (do (shutdown-agents)
          (assoc this :agents nil))
      this)))
