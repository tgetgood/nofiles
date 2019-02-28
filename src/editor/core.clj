(ns editor.core
  (:require [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [editor.events :as events]
            [editor.jfx :as fx]
            [editor.rt :as rt]
            [falloleen.core :as f])
  (:import javafx.scene.control.TextArea
           javafx.stage.Stage))

;;;;; Builtins

(defn create-code-stage []
  (let [p @(fx/code-stage)
        ev-map (events/bind-text-area! (:area p))]
    {:node (:area p) :stage (:stage p) :event-streams ev-map}))

;;;;; Exp

(def built-in-code
  {:editor
   {:core
    {:edits (fn [ev]
              (:text ev))

     :form {:edit (fn [prev text]
                    (try
                      {:emit (read-string text)}
                      (catch Exception e {:unreadable text})))}

     :display (fn [branch ns n]
                (fn [image]
                  (get-in image [:code branch ns n])))

     :format-code-text (fn [form]
                         (with-out-str (pprint form)))}}})

;; REVIEW: Really verbose, but that might well be the best way. This isn't
;; intended for human manipulation.
(defn create-topo-new-editor [branch ns n]
  (let [{:keys [^TextArea node event-streams]} (create-code-stage)

        text-render (fn [text]
                      (fx/fx-thread
                       (let [caret (.getCaretPosition node)]
                         (.setText node text)
                         (.positionCaret node caret))))]
    {:inputs    {::image       {:in rt/image-signal}
                 ::key-strokes {:in (:key-stroke event-streams)}}
     :effectors {::text-render text-render
                 ::code-change (rt/source-effector branch ns n)}
     :nodes     {::code-1 {:input (display branch ns n)}
                 ::code-2 {:input format-code-text}
                 ::edits  {:input edits}
                 ::form   form}
     :links     #{[::code-1 {:input ::key-strokes}]
                  [::code-2 {:input ::code-1}]
                  [::text-render {:in ::code-2}]
                  [::form {:edit ::edits}]
                  [::edits {:input ::key-strokes}]
                  [::image {:in ::form}]}}))
