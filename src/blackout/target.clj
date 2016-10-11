(ns blackout.target
  (:require [environ.core :refer [env]]
            [clojure.string :as str]
            [cheshire.core :as json]
            [clojure.core.async :refer [chan go >!] :as async]
            [org.httpkit.client :as http]
            [org.httpkit.server :as server]
            [blackout.riemann :as r]))

;(defonce cookie-store (clj-http.cookies/cookie-store))


(def config
  {:base-uri "https://localhost:8080"})

(defn make-uri
  [route]
  (str (:base-uri config) route))

(defn json-body
  [response]
  (if (= (:status response) 200)
    (update response :body json/parse-string true)
    response))

(defn response-time
  [response]
  (-> response
      (assoc :request-time
             (- (System/currentTimeMillis) (:request-start (:opts response))))
      (update-in [:opts] dissoc :request-start)))

(defn request
  ([path] (request :get path))
  ([method path] (request method path nil))
  ([method path options] (request method path options nil))
  ([method path options body]
   (let [response (chan)
         success-fn (fn [{:keys [status] :as resp}]
                      (let [resp2 (-> (response-time resp)
                                      (json-body)
                                      (r/riemann-trace))]
                        (go (>! response resp2))))
         options (merge
                  {:headers
                   (merge {"Content-Type" "application/json"}
                          (:headers options))
                   :request-start (System/currentTimeMillis)
                   :insecure? true}
                  (dissoc options :headers))]
     (case method
       :get (http/get (make-uri path) options success-fn)
       :post (let [body (json/generate-string body)]
               (http/post (make-uri path) (assoc options :body body) success-fn)))
     response)))


;;
;; Server
;;

(defonce server (atom nil))

(defn start-server
  [callback port]
  (reset! server (server/run-server callback {:port port})))

(defn stop-server
  []
  (when-not (nil? @server)
    (@server :timeout 100)
    (reset! server nil)))

(defn ensure-server
  [callback port]
  (stop-server)
  (start-server callback port))

(defn simple-callback
  [handler]
  (fn [request]
    (try
      (do
        (let [body (json/parse-string (slurp (:body request)) true)]
          (handler (assoc request :body body)))
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:success true})})
      (catch java.lang.Throwable t
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (str "ERROR " t)}))))

