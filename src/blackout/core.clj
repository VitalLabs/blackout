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

(defn -main
  [& args]
  (binding [*account* nil
            *session-cookie* nil]
    (let [res @(login (env :switchboard-admin-username)
                      (env :switchboard-admin-password))
          body (slurp (:body res))]
      (assert (== (:status res) 200) body)
      (set! *account* (:result (json/parse-string body true)))
      (set! *session-cookie* (get (:headers res) :set-cookie))
      (log/info (slurp (:body @(create-account)))))))
