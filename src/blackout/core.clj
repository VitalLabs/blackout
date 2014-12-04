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

(defn make-uri
  [route]
  (str "http://" (env :switchboard-host) ":" (env :switchboard-port) route))

(defn post
  [route body]
  (http/post (make-uri route)
             {:content-type :json
              :accept :json
              :body (json/generate-string (assoc-in body [:args :app] 1))}))

(defn create-account
  []
  (d/on-realized (post "/api/v1/accounts" {:action "create"
                                           :args {:account ""
                                                  :demog ""
                                                  :auth ""
                                                  :settings {}}
                                           :groups {}
                                           :password ""})
                 (fn [x]
                   (log/info (slurp (:body x))))
                 (fn [x]
                   (log/error x))))

(defn login
  [username password]
  (post "/api/v1/login" {:action "login"
                         :args {:username username
                                :password password}}))

(defn -main
  [& args]
  (binding [*account* nil]
    (let [res @(login (env :switchboard-admin-username)
                      (env :switchboard-admin-password))
          body (bs/to-string (:body res))]
      (assert (== (:status res) 200) body)
      (set! *account* (json/parse-string body true)))))
