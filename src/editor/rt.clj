(ns editor.rt
  (:require [clojure.core.async :as async]))

(defonce image
  (atom {:topology {}
         :code {:master {:core {:foo '{:a 4
                                       :b (fn [] 33)}}}}}))

(defn graph-merge [g h]
  )

(defn add-to-topology [network]
  (swap! image update :topology graph-merge network))

(defn topology-effector []
  (fn [m]
    (add-to-topology m)))

(defn source-effector [branch ns n]
  (fn [code]
    (swap! image assoc-in [:code branch ns n] code)))

(defn init-signals! [system]
  (map (fn [[k v]]
         (let [{:keys [in def]} v
               out (async/chan)]))
       (:topology system)))
