(ns editor.demo
  (:require [editor.db :as db :refer [conn]]
            [editor.io :refer [clojurise datomify]]
            [datomic.api :as d])
  (:import [java.io File]))


(def six
  {:form/type :type.number/long
   :long/value 6})

(def test-set
  (datomify #{2 3 7 19 27 6 1 91 1000}))

(def tx
  @(d/transact conn [six test-set]))

(def maps-with-fns
  '[{:type impl
     :construct   (fn [x] x)}
    {:type :collection
     :vals [{:f1 (fn [x y] (+ y (* 3 x))) :f2 conj :key :seven}]}
    {:type unknown
     :construct (fn [] {})}
    {:growth  3.7
     :vanity  :high
     :feed-fn (fn [food] (force (down food)))}])

(def query
  (quote
   [:find ?v .
    :in $ ?e
    :where [?e :long/value ?v]]))

(def bad-query
  (quote
   [:find [?e ...]
    :where [?e :long/value 6]]))

(def anonymous-fns-with-bad-arg-names
  "How many functions use single letter variables?"
  (quote
   [
    ;; :find (count ?f) ?name
    :find (count-distinct ?f) .
    :where
    [?f :form/type :type/list]
    [?f :list/head ?h]
    [?h :symbol/value ?sym]
    [(= ?sym "fn")]
    [?f :list/tail ?t]
    [?t :list/head ?args]
    [?args :vector/element ?el]
    [?el :vector.element/value ?arg]
    [?arg :symbol/value ?name]
    [(count ?name) ?len]
    ;; [(!= ?name "&")]
    [(= ?len 1)]]))

(def count-defns
  (quote
   [:find (count ?v)
    :where
    [?v]])
  )


;;;;; Index the source code of this project in the DB.

(defn datomify-src [fname]
  (let [raw-source (slurp fname)
            sexps (read-string (str "[" raw-source "]"))]
        (datomify sexps)))

(defn reindex-code! []
  (db/reset-db!)
  (let [sources (map str (.listFiles (File. "src/editor/")))]
    (run! #(d/transact conn [(datomify-src %)]) sources)))

(defn example-1
  "This is where a doc string goes"
  [x y]
  (+ x y))

(defn example-2 [x y]
  "This is not where the docstring goes"
  (+ x y))
