(ns editor.core
  (:require [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [editor.events :as events]
            [editor.jfx :as fx]
            [editor.rt :as rt]
            [falloleen.core :as f])
  (:import javafx.scene.control.TextArea
           javafx.stage.Stage))

(defn create-code-stage []
  (let [p @(fx/code-stage)
        ev-map (events/bind-text-area! (:area p))]
    {:node (:area p) :stage (:stage p) :event-streams ev-map}))

;;;;; Transducers

(defn edits [ev]
  (:text ev))

(def form
  {:edit (fn [prev text]
           (try
             {:emit (read-string text)}
             (catch Exception e {:unreadable text})))})

(defn display [branch ns n]
  (fn [image]
    (get-in image [:code branch ns n])))

(defn format-code-text [form]
  (with-out-str (pprint form)))

;;;;; Topo

;; (defn set-text-area [branch ns n ^TextArea t]
;;   (let [d (display branch ns n)]
;;     (fn [image]
;;       (let [caret (.getCaretPosition t)]
;;         (fx/fx-thread
;;          (.setText t (with-out-str (pprint (d image))))
;;          (.positionCaret t caret))))))

;; (defn wire-it-up! [branch ns n obj]
;;   (let [text-setter (set-text-area branch ns n obj)
;;         source-out (rt/source-effector branch ns n)]

;;     (text-setter @rt/image)

;;     (async/go-loop []
;;       (when-let [m (async/<! rt/image-stream)]
;;         (text-setter m)
;;         (recur)))

;;     (async/go-loop [prev {}]
;;       (when-let [m (async/<! (:key-stroke ev-map))]
;;         (let [m (edits m)
;;               o ((:edit form) prev m)]
;;           (when-let [next (:emit o)]
;;             (source-out next))
;;           (recur o))))))

(defn create-topo-new-editor [branch ns n]
  (let [{:keys [^TextArea node event-streams]} (create-code-stage)
        text-render (fn [text]
                      (fx/fx-thread
                       (let [caret (.getCaretPosition node)]
                         (.setText node text)
                         (.positionCaret node caret))))]
    [[:wire rt/image-stream (display branch ns n) format-code-text text-render]
     [:wire (:key-stroke event-streams) edits form
      (rt/source-effector branch ns n)]]))
