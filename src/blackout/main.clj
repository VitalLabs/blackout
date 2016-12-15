(ns blackout.main
  (:gen-class)
  (:require [clojure.java.shell :refer [sh with-sh-env]]
            [clojure.java.io :as io]
            [blackout.core :as blackout]))

(defn classpath
  []
  {"CLASSPATH" (System/getProperty "java.class.path")})

(defn launch-riemann
  []
  (if-let [cfg (io/resource "riemann.config")]
    (with-sh-env (classpath)
      (let [result (sh "riemann" (.getPath cfg))]
        (if-not (empty? (:err result))
          (println (:err result))
          (println (:out result)))))
    (throw (ex-info "riemann.config not found in classpath" {}))))

(defn launch-riemann-dash
  []
  (with-sh-env (classpath)
    (let [result (sh "riemann-dash")]
      (if-not (empty? (:err result))
        (println (:err result))
        (println (:out result))))))

(defn -main
  [sim-ns simulation users requests & [options]]
  (let [ns (symbol sim-ns)
        _ (require ns)
        app (resolve (symbol sim-ns "simulations"))]
    (when (not (empty? options)) ;; TO DEBUG, on cluster devops launches these already
      (launch-riemann)
      (launch-riemann-dash))
    (blackout/run (var-get app) (keyword simulation) (read-string users) (read-string requests) {})
    (System/exit 0)))

