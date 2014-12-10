(ns blackout.main
  (:gen-class)
  (:require [clojure.java.shell :refer [sh with-sh-env]]
            [clojure.java.io :as io]))

(defn classpath
  []
  {"CLASSPATH" (System/getProperty "java.class.path")})

(defn -main
  [& args]
  (if-let [cfg (io/resource "riemann.config")]
    (with-sh-env (classpath)
      (let [result (sh "riemann" (.getPath cfg))]
        (if-not (empty? (:err result))
          (println (:err result))
          (println (:out result)))))
    (throw (ex-info "riemann.config not found in classpath" {})))
  (System/exit 0))
