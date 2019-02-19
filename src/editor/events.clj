(ns editor.events
  "Respond to javafx events. This shouldn't be in this project."
  (:require [clojure.datafy :refer [datafy]])
  (:import javafx.event.EventHandler
           javafx.scene.Node
           javafx.scene.Scene))

(defmacro handler
  {:style/indent [1]}
  [binding & body]
  `(proxy [Object EventHandler] []
     (handle [^Event ~@binding]
       ~@body)))

(def ph
  (handler [e]
    (println e)))

(def binding-map
  {:mouse-down 'setOnMouseDown
   :mouse-up   'setOnMouseReleased
   :mouse-in   'setOnMouseEntered
   :mouse-out  'setOnMouseExited
   :click      'setOnMousePressed

   :key-down   'setOnKeyPressed
   :key-up     'setOnKeyReleased
   :key-stroke 'setOnKeyTyped})

(defmacro create-binder
  [type handlers]
  (let [x (with-meta (gensym) {:tag type})]
    `(fn [~x]
       (do
         ~@(map
            (fn [[k# v#]]
              `(. ~x ~(get binding-map k#) ~v#))
            handlers)))))

(defmacro bind-text-area!
  {:style/indent [1]}
  [node handlers]
  `((create-binder Node ~handlers) ~node))

(defmacro bind-canvas!
  "This is more than a little ugly."
  {:style/indent [1]}
  [node handlers]
  `((create-binder Scene ~handlers) ~node))
