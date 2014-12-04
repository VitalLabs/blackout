(ns blackout.core
  (:require [aleph.http :as http]
            [manifold.deferred :as d]
            [byte-streams :as bs]
            [clojure.core.async :refer :all
             :exclude [map into reduce merge take partition partition-by]]
            [cheshire.core :as json]
            [environ.core :refer [env]]
            [clojure.tools.logging :as log]))

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
                     :body (-> (assoc-in body [:args :app] 1)
                               (json/generate-string))}
                    {:headers (when *session-cookie*
                                {"set-cookie" *session-cookie*})})))

(defn create-account
  []
  (post "/api/v1/accounts" {:action "create"
                            :args {:account ""
                                   :demog ""
                                   :auth (:auth *account*)
                                   :settings {}}
                            :groups {}
                            :password ""}))

(defn login
  [username password]
  (post "/api/v1/login" {:action "login"
                         :args {:username username
                                :password password}}))

(defn -main
  [& args]
  (binding [*account* nil
            *session-cookie* nil]
    (let [res @(login (env :switchboard-admin-username)
                      (env :switchboard-admin-password))
          body (bs/to-string (:body res))]
      (assert (== (:status res) 200) body)
      (set! *account* (:result (json/parse-string body true)))
      (set! *session-cookie* (get (:headers res) "set-cookie"))
      (bs/to-string (:body @(create-account))))))
