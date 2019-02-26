(ns editor.events
  "Respond to javafx events. This shouldn't be in this project."
  (:require [clojure.core.async :as async]
            [clojure.datafy :refer [datafy]]
            [editor.jfx :as fx])
  (:import [javafx.event Event EventHandler]
           [javafx.scene.input KeyEvent]
           javafx.scene.control.TextArea
           javafx.scene.Node
           javafx.scene.Scene))

(defmacro handler
  {:style/indent [1]}
  [binding & body]
  `(proxy [Object EventHandler] []
     (handle [^Event ~@binding]
       ~@body)))

(defmulti datafy-event (fn [x ^Event ev] (.getName (.getEventType ev))))

(defmethod datafy-event :default
  [x ev]
  ev)

(defmethod datafy-event "MOUSE_ENTERED"
  [x ev]
  ev)

(defmethod datafy-event "MOUSE_EXITED"
  [x ev]
  ev)

(defmethod datafy-event "KEY_TYPED"
  [^TextArea x ^KeyEvent ev]
  {:caret (.getCaretPosition x)
   :char (.getCharacter ev)
   :text (.getText x)})

(def binding-map
  {:mouse-down 'setOnMousePressed
   :mouse-up   'setOnMouseReleased
   :mouse-in   'setOnMouseEntered
   :mouse-out  'setOnMouseExited
   :click      'setOnMousePressed

   :key-down   'setOnKeyPressed
   :key-up     'setOnKeyReleased
   :key-stroke 'setOnKeyTyped})

(defn ch-handler [event-xform]
  (let [c (async/chan (async/sliding-buffer 128))]
    [(handler [ev] (async/put! c (event-xform ev))) c]))

(defmacro text-area-binder
  "Binds event handlers to the given JFX object and returns a map from event
  names to channels that will receive events."
  []
  (let [x (gensym)]
    `(fn [^Node ~x]
       (into {}
             [~@(map
                 (fn [[k# v#]]
                   (let [hs (gensym)
                         cs (gensym)]
                     `(let [[~hs ~cs] (ch-handler (partial datafy-event ~x))]
                        (fx/fx-thread (. ~x ~v# ~hs))
                        [~k# ~cs])))
                 binding-map)]))))

(defmacro bind-text-area!
  [node]
  `((text-area-binder) ~node))

;; (defmacro bind-canvas!
;;   "This is more than a little ugly."
;;   {:style/indent [1]}
;;   [node handlers]
;;   `((create-binder Scene ~handlers) ~node))
