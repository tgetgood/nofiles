(ns editor.demo
  (:require [editor.core :refer [push! pull!]]
            [editor.db :as db :refer [conn]]
            [editor.io :refer [clojurise datomify cps]]
            [datomic.api :as d])
  (:import [java.io File]))

;;;;; Queries

(def count-uses
  (quote
   [:find (count ?e) .
    :in $ ?v
    :where [?e :symbol/value ?v]]))

(d/q count-uses (d/db conn) "conj")
(d/q count-uses (d/db conn) "fn")
(d/q count-uses (d/db conn) "defmethod")

;; Find the ten most common symbols in your codebase.

(->> (d/q '[:find ?e ?v :where [?e :symbol/value ?v]] (d/db conn))
     (map second)
     frequencies
     (sort-by val >)
     (take 10))

(def anonymous-fns-with-bad-arg-names
  "How many functions use single letter variables?"
  (quote
   [
    :find (count ?f) ?name
    ;; :find [(count ?f) (count-distinct ?f)]
    ;; :find [(pull ?f cps) ...]
    ;; :in $ cps
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

;;;;; defns and rules

(def count-defns
  (quote
   [:find (count ?v) .
    :where
    [?v :list/head ?h]
    [?h :symbol/value ?sym]
    [(= ?sym "defn")]]))

(def documented-defns
  (quote
   [:find (count ?v) .
    :where
    [?v :list/head ?h]
    [?h :symbol/value ?sym]
    [(= ?sym "defn")]
    [?v :list/tail ?t]
    [?t :list/tail ?t2]
    [?t2 :list/head ?doc]
    [?doc :form/type :type/string]]))

(def improperly-documented-fns
  (quote
   [:find (count ?v) .
    :where
    [?v :list/head ?h]
    [?h :symbol/value ?sym]
    [(= ?sym "defn")]
    [?v :list/tail ?t]
    [?t :list/tail ?t2]
    [?t2 :list/head ?doc]
    [?doc :form/type :type/string]]))

;;;;; OR and query inputs

(def count-vars
  (quote
   [:find (count ?v) .
    :where
    [?v :list/head ?h]
    [?h :symbol/value ?sym]
    (or
     [(= ?sym "defn")]
     [(= ?sym "def")]
     [(= ?sym "defmacro")]
     [(= ?sym "defmulti")]
     [(= ?sym "defprotocol")])]))

(def count-vars-smart
  (quote
   [:find (count ?v) .
    :in $ [?def ...]
    :where
    [?v :list/head ?h]
    [?h :symbol/value ?sym]
    [(= ?sym ?def)]]))

(d/q count-vars-smart (d/db conn)
     ["defn"
      "def"
      "defmacro"
      "defmulti"
      "defprotocol"])

;;;;; Index the source code of this project in the DB.

(defn datomify-src [fname]
  (let [raw-source (slurp fname)
            sexps (read-string (str "[" raw-source "]"))]
        (datomify sexps)))

(defn reindex-code! []
  (db/reset-db!)
  (let [sources (->> "src/editor/"
                     #_"../reference/clojure/src/clj/clojure"
                     File.
                     .listFiles
                     (map str)
                     (filter (partial re-seq #"\.clj$")))]
    (run! #(d/transact conn [(datomify-src %)]) sources)))

(defn example-1
  "This is where a doc string goes"
  [x y]
  (+ x y))

(defn example-2 [x y]
  "This is not where the docstring goes"
  (+ x y))

(reindex-code!)
