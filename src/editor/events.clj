(ns editor.events
  "Respond to javafx events. This shouldn't be in this project."
  (:require [clojure.core.async :as async]
            [clojure.datafy :refer [datafy]]
            [editor.jfx :as fx])
  (:import [javafx.event Event EventHandler]
           javafx.scene.Node
           javafx.scene.Scene))

(defmacro handler
  {:style/indent [1]}
  [binding & body]
  `(proxy [Object EventHandler] []
     (handle [^Event ~@binding]
       ~@body)))

(defmulti datafy-event (fn [^Event ev] (.getName (.getEventType ev))))

(defmethod datafy-event :default
  [ev]
  ev)

(defmethod datafy-event "MOUSE_ENTERED"
  [ev]
  ev)

(defmethod datafy-event "MOUSE_EXITED"
  [ev]
  ev)

(def binding-map
  {:mouse-down 'setOnMousePressed
   :mouse-up   'setOnMouseReleased
   :mouse-in   'setOnMouseEntered
   :mouse-out  'setOnMouseExited
   :click      'setOnMousePressed

   :key-down   'setOnKeyPressed
   :key-up     'setOnKeyReleased
   :key-stroke 'setOnKeyTyped})

(defn ch-handler []
  (let [c (async/chan (async/sliding-buffer 128))
        f (fn [ev] (async/put! c (datafy-event ev)))]
    [(handler [ev] (f ev)) c]))

(defmacro node-binder
  "Binds event handlers to the given JFX object and returns a map from event
  names to channels that will receive events."
  [tag]
  (let [x (with-meta (gensym) {:tag tag})]
    `(fn [~x]
       (into {}
             [~@(map
                 (fn [[k# v#]]
                   (let [hs (gensym)
                         cs (gensym)]
                     `(let [[~hs ~cs] (ch-handler)]
                        (fx/fx-thread (. ~x ~v# ~hs))
                        [~k# ~cs])))
                 binding-map)]))))

(defn bind-text-area!
  {:style/indent [1]}
  [node]
  ((node-binder Node) node))

;; (defmacro bind-canvas!
;;   "This is more than a little ugly."
;;   {:style/indent [1]}
;;   [node handlers]
;;   `((create-binder Scene ~handlers) ~node))
