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

(defn lift [f]
  {:input (fn [_ x] (f x))})

(defn text-renderer [^TextArea node]
  (fn [text]
    (fx/fx-thread
     (let [caret (.getCaretPosition node)]
       (.setText node text)
       (.positionCaret node caret)))))

(def image-signal '...)

(def source-effector rt/source-effector)

;;;;; Exp

(def built-in-code
  (quote
   {:internal.editor.core
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
                         (with-out-str (pprint form)))

     :create-topo-new-editor
     ;; REVIEW: Really verbose, but that might well be the best way. This isn't
     ;; intended for human manipulation.
     (fn [branch ns n]
       (let [{:keys [node event-streams]} (create-code-stage)]

         {:inputs {::image       image-signal
                   ::key-strokes (:key-stroke event-streams)}

          :effectors {::text-render {:in (text-renderer node)}
                      ::code-change {:in (source-effector branch ns n)}}

          :nodes {::code-1 (lift (display branch ns n))
                  ::code-2 (lift format-code-text)
                  ::edits  (lift edits)
                  ::form   form}

          :links #{[::code-1 {:input ::image}]
                   [::code-2 {:input ::code-1}]
                   [::text-render {:in ::code-2}]

                   [::edits {:input ::key-strokes}]
                   [::form {:edit ::edits}]
                   [::code-change {:in ::form}]}}))}}))

(rt/load-code built-in-code)
