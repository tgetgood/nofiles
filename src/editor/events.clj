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

(defn bind-canvas! [^Scene node]
  (doto node
    (.setOnMousePressed (handler [e] (println :md)))
    (.setOnMouseReleased (handler [e] (println :mu)))
    (.setOnMouseMoved (handler [e] (println :mu)))

    (.setOnMouseEntered (handler [e] (println :mouse-in)))
    (.setOnMouseExited (handler [e] (println :mouse-out)))


    (.setOnKeyTyped ph)
    (.setOnKeyPressed (handler [e] (println :kd)))
    (.setOnKeyReleased (handler [e] (println :ku)))
   ))

(defn bind-text-area! [^Node node]
  (doto node
    (.setOnMouseClicked (handler [e] (println :click)))

    (.setOnMouseMoved (handler [e] (println :mu)))
    (.setOnMouseEntered (handler [e] (println :mouse-in)))
    (.setOnMouseExited (handler [e] (println :mouse-out)))

    (.setOnKeyTyped ph)
    (.setOnKeyPressed (handler [e] (println :kd)))
    (.setOnKeyReleased (handler [e] (println :ku)))
    ))
