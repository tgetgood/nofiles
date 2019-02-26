(ns editor.rt
  (:require [clojure.core.async :as async]))

(defonce image
  (atom {:topology []
         :code {:master {:core {:foo '{:a 4
                                       :b (fn [] 33)}}}}}))

(defonce image-stream
  (let [c (async/chan 32)]
    (add-watch image :stream
               (fn [_ _ _ state]
                 (async/put! c state)))
    c))



(defn graph-merge [g h]
  )

(defn add-to-topology [network]
  (swap! image update :topology into network))

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
