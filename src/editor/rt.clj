(ns editor.rt
  (:refer-clojure :exclude [peek])
  (:require [clojure.core.async :as async]))

(defprotocol ISignal
  (subscribe [this key] [this key opts]
    "Returns a channel which receives messages emitted by this signal")
  (peek [this])
  (disconnect [this key]))

(deftype Signal [input ^:volatile-mutable _last ^:volatile-mutable _listeners]
  ISignal
  (peek [this]
    _last)
  (subscribe [this k]
    (subscribe this k {}))
  (subscribe [this k opts]
    (let [ch (async/chan (async/sliding-buffer 128))]
      (assert (not (contains? _listeners k))
              (str "Listener named " k " already registered. "
                   "I can't let you clobber that."))
      (set! _listeners (assoc _listeners k ch))
      (when-not (= _last ::uninitialised)
        (async/put! ch _last))
      ch))
  (disconnect [this k]
    (when-let [ch (get _listeners k)]
      (async/close! ch)
      (set! _listeners (dissoc _listeners k)))))

(defn signal
  ([]
   (signal (async/chan (async/sliding-buffer 128))))
  ([ch]
   (Signal. ch ::uninitialised {})))

(defprotocol T
  (running? [this])
  (start! [this])
  (stop! [this]))

(defrecord Transducer [methods inputs output running?])

(defn wire [methodmap input-signals]
  (if (and (fn? methodmap) (satisfies? ISignal input-signals))
    (recur {::token methodmap} {::token input-signals})
    (let [output (signal)
          t (Transducer. methodmap input-signals output)]
      ;; TODO: Check that everything is connected

      )))

(defonce image
  (atom {:topology []
         :code {:master {:core {:foo '{:a 4
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
