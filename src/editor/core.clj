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

(defonce q
  (let [ch (async/chan (async/sliding-buffer 100))]
    (async/go-loop []
      (when-let [message (async/<! ch)]
        (when (= :key-stroke (:type message))
          (let [text (:text message)]
            (try
              (binding [*read-eval* false]
                (let [form (read-string text)]
                  (swap! system assoc-in [:namespaces :dumpalump.core]
                         {:base-render form :*base-render* nil})))
              (catch Exception e
                (swap! system assoc-in [:namespaces :dumpalump.core
                                        :*base-render*]
                       text)))))
        (recur)))
    ch))

(defonce ev-map
  (events/bind-text-area! (:area p)))

(defn create-code-window [sym]
  (let [ns (keyword (namespace sym))
        n (keyword (name sym))
        form (get-in @system [:namespaces ns n])
        text (with-out-str (pprint form))]
    ))

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
