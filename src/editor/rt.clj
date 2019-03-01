(ns editor.rt
  (:require [clojure.core.async :as async]))

(defonce image
  (agent {:master {:topology   {}
                   :namespaces {}}}))

(defn load-code [code]
  (send image update-in [:master :namespaces] #(merge-with merge % code)))

(defn load-up-code [code]
  #_(let [n *ns*]
    (namespace ))
  )

(defn eval-from-code [sym]
  (let [ns (the-ns (symbol (namespace sym)))]
    ))

(defonce system (atom {}))

(defn connect! [system network]
  )

(defn update-topology [topology network]
  (connect! topology network))

(def signal identity)

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
  (send image update :topology #(merge-with merge % network))
  )

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
