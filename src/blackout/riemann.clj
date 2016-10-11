(ns blackout.riemann
  (:require [clojure.string :as str]
            [environ.core :refer [env]]
            [riemann.client :as r]))

(defonce riemann-client (r/tcp-client {:host (env :riemann-host)
                                       :port (env :riemann-port)}))

;;
;; Low-level event transmission
;;

(defn send-event
  ([event]
   (r/send-event riemann-client event))
  ([event ack]
   (r/send-event riemann-client event ack)))

(defn send-events
  ([events]
   (r/send-events riemann-client events))
  ([events ack]
   (r/send-events riemann-client events ack)))

;;
;; Ring handler to sends events on all IO to Reimann server
;;

(defn riemann-trace
  [response]
  (let [{:keys [status body request-time]} response
        {:keys [server-time]} (:body response)
        status (if (== status 200) "OK" "ERROR")]
    (when server-time
      (send-event {:service "server request time (ms)"
                   :state status
                   :metric server-time}))
    (send-event {:service "client request time (ms)"
                 :state status
                 :metric request-time})
    response))

;;
;; Interactive utilities
;;

(defn q
  [query]
  (r/query riemann-client query))

