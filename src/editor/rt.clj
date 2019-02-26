(ns editor.rt
  (:require [clojure.core.async :as async]))

(defonce image
  (atom {}))

(defn topology-effector [])

(defn source-effector [])

(defn init-signals! [system]
  (map (fn [[k v]]
         (let [{:keys [in def]} v
               out (async/chan)]))
       (:topology system)))
