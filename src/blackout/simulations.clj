(ns blackout.simulations
  (:require [clojure.core.async :refer [chan go >! <! >!! <!!] :as async]
            [blackout.riemann :as r]
            [blackout.target :as target]))

;;
;; Operations
;;

(defn success?
  "Given a request function that returns a
   response channel, return a response channel
   that "
  [ch]
  (async/map (fn [response]
               (cond
                (map? response) (= (:status response) 200)
                (sequential? response) (= (:status (first response)) 200)
                :default false))
             [ch]))

;; PING

(defn ping [_]
  (success? (target/request "/api/v1/status")))


(def ping-simulation
  {:name "Ping simulation"
   :scenarios [{:name "Ping scenario"
                :steps [{:name "Ping Status Endpoint"
;                         :sleep-before (constantly 1000)
                         :request ping}]}]})

;; LOGIN

(defn login
  [context]
  [(success?
    (target/request :post "/api/v1/login" {}
                    {:action "login"
                     :args {:_username (:username context)
                            :_password (:password context)
                            :_app 1}}))
   context])

(def login-simulation
  {:name "Login simulation"
   :scenarios [{:name "Login scenario"
                :steps [{:name "Login to Server"
                         :request login}]}]})

;; LOOKUP SUBJECTS

(defn subjects
  [context]
  (success?
   (target/request :get "/api/v1/subjects" 
                   {:basic-auth [(:username context)
                                 (:password context)]
                    :query-params {:_app 1}})))

(def subject-simulation
  {:name "Subject simulation"
   :scenarios [{:name "Subject scenario"
                :steps [{:name "Subject lookup"
                         :request subjects}]}]})

;; LOAD TEST 1

(def lt1-registry (atom {}))

(defn- register-payload
  [payload result-chan context]
  (swap! lt1-registry assoc (:id payload)
         (assoc payload
                :result-chan result-chan
                :context context)))

(defn- lookup-payload
  [id]
  (@lt1-registry id))

(defn- unregister-payload
  [id]
  (swap! lt1-registry dissoc id))

(defn- write-response
  "Handle writing a payload-id containing response into
   the response channel"
  [chan response context]
  (if (not= (:status response) 200)
    (>!! chan false)
    (>!! chan 
         [true (assoc context
                      :payload (get-in response [:body :result :payload]))])))

(defn- handle-registered-payload
  "When we get a payload callback, and it's in the registry,
   then return the payload and context"
  [request]
  (let [payload (get-in request [:body :payload])]
    (when-let [reg-payload (lookup-payload (:id payload))]
      (let [{:keys [result-chan context]} reg-payload
            signal-response-time (- (System/currentTimeMillis) (:server-callback payload))]
        (r/send-event {:service "signal response time (ms)"
                       :state "OK"
                       :metric signal-response-time})
        (write-response result-chan {:status 200 :body {:result {:payload (assoc payload :signal-time signal-response-time)}}} context)
        (unregister-payload )))))

;;
;; Server for callbacks
;;

(defn load-test-1-setup
  [context]
  (reset! lt1-registry {})
  (target/ensure-server (target/simple-callback #'handle-registered-payload) 8090)
  (success?
   (target/request :post "/api/v1/public/load/1/setup" 
                   {}
                   {:action "setup-load-test-1"
                    :args {:_app 1 :start 0 :end (or (:concurrent context) 1000)}})))

(defn load-test-1-cleanup
  [context]
  (target/stop-server)
  (success?
   (target/request :post "/api/v1/public/load/1/cleanup"
                   {}
                   {:action "cleanup-load-test-1"
                    :args {:_app 1 :start 0 :end (or (:concurrent context) 1000)}})))

(defn load-test-1-write
  [signal? context]
  (let [payload {:signal signal?
                 :id (:user-id context)
                 :url "http://localhost:8090/"
                 :content "This is a test"}
        result-chan (chan)
        write-chan (target/request :post "/api/v1/public/load/1/write"
                                   {}
                                   {:action "simulated-observe"
                                    :args {:_app 1 :payload payload}})]
    ;; Handle the result based on whether we expect this to signal
    (go
      (try
        (let [{:keys [body] :as response} (<! write-chan)]
          (if signal?
            ;; When signaling, wait for the callback by registering it
            (register-payload (get-in body [:result :payload]) result-chan context)
            ;; Otherwise return the payload and id for use by next step
            (write-response result-chan response context)))
        (catch java.lang.Throwable t
          (println "Error in go loop " t)
          (>! result-chan [false (assoc context
                                        :exception t
                                        :error "Error in write go routine")]))))
    result-chan))

(defn load-test-1-read
  [context]
  (let [payload-id (get-in context [:payload :payload-id])]
    (success?
     (target/request :get "/api/v1/public/load/1/read" 
                     {:query-params {:_app 1 :payload-id payload-id}}
                     ))))

(def load-1-simulation
  {:name "Load test 1 simulation"
   :scenarios [{:name "Observation and load test evaluation"
                :weight 10
                :steps [{:name "Write a payload"
                         :sleep-before (fn [& args] (rand-int 1000)) ;; 0-1 sec random delay
                         :request (partial load-test-1-write false)}
                        {:name "Read back the payload"
                         :request load-test-1-read}]}
               {:name "Observation triggering a load-test signal"
                :weight 1
                :steps [{:name "Write a triggering payload"
                         :request (partial load-test-1-write true)}]}]})

(def simulations
  {:ping ping-simulation
;   :metrics metrics-simulation
   :login login-simulation
   :subjects subject-simulation
   :load-1 load-1-simulation
   })
