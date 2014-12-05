(ns blackout.core
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [clojure.core.async :refer :all
             :exclude [map into reduce merge take partition partition-by]]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]
            [clojure.test.check.generators :as gen]))

(def ^:dynamic *account* nil)
(def ^:dynamic *session-cookie* nil)
(def ^:dynamic *accounts* nil)

(defn make-uri
  [route]
  (str "http://" (env :switchboard-host) ":" (env :switchboard-port) route))

(defn post
  [route body]
  (http/post (make-uri route)
             (merge {:content-type :json
                     :accept :json
                     :body (json/generate-string body)}
                    (when *session-cookie*
                      {:headers {:cookie *session-cookie*}}))))

(defn gen-account
  []
  (let [username (last (gen/sample gen/string-alphanumeric 20))]
    {:role (rand-nth ["admin" "user"])
     :email (str username "@vitallabs.co")
     :username username}))

(defn create-account
  []
  (post "/api/v1/accounts" {:action "create"
                            :args (gen-account)
                            :groups {}
                            :password ""}))

(defn login
  [username password]
  (post "/api/v1/login" {:action "login"
                         :args {:username username
                                :password password
                                :app 1}}))

(defn gen-agents
  []
  (repeatedly (.availableProcessors (Runtime/getRuntime)) #(agent [])))

(defn aggregate-request-stats
  [stats res]
  (let [stats (cond-> (-> stats
                          (update :num-requests inc)
                          (update :total-time + (:server-time (:body res))))
                (== (:status res) 200)
                (update :num-success inc)

                (not= (:status res) 200)
                (update :num-error inc))]
    (when (not= (:status res) 200)
      (log/error (:body res)))
    (-> stats
        (assoc :success-rate (/ (:num-success stats) (:num-requests stats)))
        (assoc :success-rate (/ (:num-success stats) (:num-requests stats))))))

(defn simulate-population
  [n]
  )

(defn chunk-requests
  [f]
  (comp (partition-all (.availableProcessors (Runtime/getRuntime)))
        (map (fn [& xs]
               (let [responses (mapv (fn [_] (f))
                                     (range (count xs)))]
                 (mapv deref responses))))
        cat))

(defn -main
  [& args]
  (binding [*account* nil
            *session-cookie* nil
            *accounts* (atom #{})]
    (let [res @(login (env :switchboard-admin-username)
                      (env :switchboard-admin-password))
          body (slurp (:body res))]
      
      (assert (== (:status res) 200) body)
      
      (set! *account* (:result (json/parse-string body true)))
      (set! *session-cookie* (get (:headers res) :set-cookie))
      
      (let [responses (into [] (chunk-requests create-account) (range 32))]
        (transduce (comp (map (fn [res]
                                (update res :body json/parse-string))))
                   (completing aggregate-request-stats)
                   {:total-time 0
                    :mean-latency 0
                    :num-success 0
                    :num-error 0
                    :num-requests 0
                    :success-rate 1.0} responses)))))
