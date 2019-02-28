(ns editor.rt
  (:refer-clojure :exclude [peek])
  (:require [clojure.core.async :as async]))

(defonce image
  (atom {:master {:topology   {}
                  :namespaces {:core {:foo '{:a 4
                                             :b (fn [] 33)}}}}}))

(defonce image-signal
  (let [c (async/chan 32)]
    (add-watch image :stream
               (fn [_ _ _ state]
                 (async/put! c state)))
    (signal c)))

(defonce running-image (atom #{}))

(defn connect-wires [topology]
  (let [new-wires (filter #(contains? @running-image %) topology)]
    (doseq [wire new-wires]
      (async/go-loop []
        (when-let [v (async/<! (second wire))]
          (loop [val v
                 [f & fs] (rest (rest wire))]
            (if f
              (when-let [v' (f val)]
                (recur v' fs))
              val))
          (recur))))
    (swap! running-image into topology)))

(defn graph-merge [g h]
  )

(defn add-to-topology [network]
  (swap! image update :topology #(merge-with merge % network))
  (rewire! topology))

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
