(ns editor.core
  (:require [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [editor.events :as events]
            [editor.jfx :as fx]
            [falloleen.core :as f])
  (:import javafx.scene.control.TextArea
           javafx.stage.Stage))

(defonce system
  (atom
   {:topology {:render {:state {}}}
    :namespaces {:dumpalump.core {:test '(fn [] "I do nothing.")
                                  :base-render '(fn [frame]
                                                  [(assoc f/circle :radius 100)])}}
    :code-views {:primary :dumpalump.core/base-render}
    :messages []}))

(defonce host
  (falloleen.hosts/default-host {:size [1000 1000]}))

(defonce p
  @(fx/code-stage))

(defonce ev-map
  (events/bind-text-area! (:area p)))

#_(def example-xform
  {:key-down (fn [previous event]
               (assoc previous :alt? (= (.keyCode event) "Alt")))
   :edit (fn [previous {:keys [text]}]
           (if (:alt? previous)
             previous
             (assoc previous :emit text)))})

(defn code-edit-sub-network
  "Creates a new editor window, returns the transduction network that connects
  its events and the codebase to the rendered code and the code edit effector."
  [branch name]
  )
