(ns editor.core
  (:require [clojure.core.async :as async]
            [clojure.datafy :refer [datafy]]
            [clojure.pprint :refer [pprint]]
            [datomic.api :as d]
            editor.db
            [editor.events :as events]
            [editor.io :refer [clojurise datomify]]
            [falloleen.core :as f])
  (:import [javafx.application Application Platform]
           [javafx.scene.canvas Canvas GraphicsContext]
           javafx.scene.layout.StackPane
           javafx.scene.Scene
           javafx.scene.Group
           javafx.scene.Node
           javafx.scene.control.TextArea
           javafx.stage.Stage))

(def conn editor.db/conn)

;;;;; Read/Write forms to datomic

(defn save-to-datomic! [data]
  (let [tx-data (datomify data)
        tx @(d/transact conn [tx-data])]
    {:eid (get-in tx [:tempids (:tempid (meta tx-data))])
     :time (d/next-t (:db-before tx))}))

(defn pull-from-datomic! [{:keys [eid time] :as accessor}]
  (let [db (d/db conn)
        d  (clojurise (d/pull (d/as-of db time) '[*] eid))]
    (if (instance? clojure.lang.IMeta d)
      (with-meta d (assoc (meta d) :db-link accessor))
      d)))

;;;;; Dealing with execution

(defn builtin? [x]
  (contains? (:publics (datafy (the-ns 'clojure.core))) x))

(defn write-fn! [form]
  (let [tx-data [{:db/id "the-fn"
                  :function/form (datomify form)}]
        tx @(d/transact conn tx-data)]
    {:eid (get-in tx [:tempids "the-fn"]) :time (d/next-t (:db-before tx))}))

(defn load-fn! [{:keys [eid time] :as a}]
  (let [db   (d/db conn)
        fid  (d/q [:find '?e '. :where [eid :function/form '?e]]
                  (d/as-of db time))
        form (pull-from-datomic! (assoc a :eid fid))]
    (with-meta (eval form) {:source-form form
                            :db-link     a})))

(defn db-lift [f]
  (fn [& args]
    (let [res (apply f (map pull-from-datomic! args))]
      (save-to-datomic! res))))

(defonce system
  (atom
   {:topology {:renderer {:in #{:animation-frame} :def 'dumpalump.core/base-render}}
    :namespaces {:dumpalump.core {:test '(fn [] "I do nothing.")
                                  :base-render '(fn [frame]
                                                  [(assoc f/circle :radius 100)])}}
    :code-views {:primary :dumpalump.core/base-render}
    :messages []}))

(defn init-signals! [system]
  (map (fn [[k v]]
         (let [{:keys [in def]} v
               out (async/chan)]))
       (:topology system)))

;; TODO: Move this into Falloleen. This isn't the place to deal with the platform.
(defmacro fx-thread [& body]
  `(let [p# (promise)]
    (Platform/runLater (proxy [Runnable] []
                          (run []
                            (let [res# (do ~@body)]
                              (deliver p# res#)))))
    p#))

(defonce hosts (atom #{}))

(defonce host
  (let [h (falloleen.hosts/default-host {:size [1000 1000]})]
    (run! #(fx-thread (f/close! %)) @hosts)
    (reset! hosts #{h})
    h))


(fx-thread (events/bind-canvas! (.getScene ^Stage (:stage host)) {}))

(defonce code-stages (atom []))

(defn code-stage []
  (fx-thread
   (let [s (Stage.)
         t (TextArea.)]
     (.setBorder t nil)
     (doto s
       #_(.setAlwaysOnTop true)
       (.setScene (Scene. t))
       .show)
     (swap! code-stages conj s)
     {:stage s :area t})))

(defn clear-stages! []
  (run! #(fx-thread (.close ^Stage %)) @code-stages)
  (reset! code-stages []))

(defonce p
  (do
    (clear-stages!)
    @(code-stage)))

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

(fx-thread
 (events/bind-text-area! (:area p)
   {:key-stroke (events/handler [e]
                  (async/put! q {:type :key-stroke
                                 :text (.getText ^TextArea (:area p))}))}))
