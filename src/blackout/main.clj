(ns blackout.main
  (:gen-class)
  (:require [clojure.java.shell :refer [sh]]))

(defn classpath
  []
  (System/getProperty "java.class.path"))

(defn -main
  [& args]
  (println (classpath)))
