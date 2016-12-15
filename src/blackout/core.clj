(ns blackout.core
  (:require [environ.core :refer [env]]
            [clojure.core.async :refer [chan go >! <! <!!] :as async]
            [clj-gatling.core :as gatling]
            [blackout.riemann :as r]
            [blackout.simulations :as sim]
            [blackout.sb :as sb])
  (:gen-class))


(defn run [app simulation users requests options]
  {:pre (keyword? simulation) (number? users) (number? requests)}
  (let [sim (or (simulation app)
                (throw (Exception. (str "No such simulation " simulation))))]
    (gatling/run sim
      (merge
       {:concurrency users
        :requests requests
        :timeout 5000
        :root "/tmp"
        
;        :reporter {:writer (fn [sim idx results]
;                             (println results))
;                   :generator (fn [sim]
;                                (println "Ran" simulation "without report"))
;                   }
        }
       options))))


