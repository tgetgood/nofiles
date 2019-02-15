(ns editor.demo
  (:require [clojure.pprint :refer [pprint]]
            [editor.core :refer [push! pull!]]
            [editor.db :as db :refer [conn]]
            [editor.io :refer [clojurise datomify cps]]
            [datomic.api :as d])
  (:import [java.io File]))

;;;;; Queries

(def count-uses
  '[:find (count ?e) .
   :in $ ?v
   :where [?e :symbol/value ?v]])

(d/q count-uses (d/db conn) "conj")

(d/q count-uses (d/db conn) "fn")

(d/q count-uses (d/db conn) "defmethod")

;; Find the ten most common symbols in your codebase.

(->> (d/q '[:find ?e ?v :where [?e :symbol/value ?v]] (d/db conn))
     (map second)
     frequencies
     (sort-by val >)
     (take 10))

;;;;; defns and rules

(def count-defns
  (quote
   [:find (count ?v) .
    :where
    [?v :form/type :type/list]
    [?v :list/head ?h]
    [?h :symbol/value ?sym]
    [(= ?sym "defn")]]))

(d/q count-defns (d/db conn))

;;; A common error

(defn example-1
  "This is where a doc string goes"
  [x y]
  (+ x y))

(defn example-2 [x y]
  "This is not where the docstring goes"
  (+ x y))

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

(d/q documented-defns (d/db conn))

(def defn-rule
  '[
    [(defn? ?v)
      [?v :list/head ?h]
      [?h :symbol/value ?sym]
      [(= ?sym "defn")]]
    ])

(def improperly-documented-fns
  (quote
   [
    ;; :find (count ?v) .
    :find [(pull ?v cps) ...]
    :in $ % cps
    :where
    [defn? ?v]
    [?v :list/tail ?t]
    [?t :list/tail ?t2]
    [?t2 :list/tail ?t3]
    [?t3 :list/head ?doc]
    [?doc :form/type :type/string]]))

(d/q improperly-documented-fns (d/db conn) defn-rule cps)

(map clojurise
 (d/q improperly-documented-fns (d/db conn) defn-rule cps))

;;;;; Linting with queries

(def anonymous-fns-with-bad-arg-names
  "How many functions use single letter variables?"
  (quote
   [:find (count ?f) ?name
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
    [(!= ?name "&")]
    [(= ?len 1)]]))

(d/q anonymous-fns-with-bad-arg-names (d/db conn))

;; Silly example

(fn [f & r]
  (conj r f))

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

(d/q count-vars (d/db conn))

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

;;; Change over time

(def e1
  (push! {:a "map"
          :with 2}))

(def k (push! :new-key))

(def v (push! #{:some :set :of :things}))

(defn db-assoc [m k v]
  (let [tx-data {:db/id (:eid m)
                 :map/element {:db/id "el"
                               :map.element/key (:eid k)
                               :map.element/value (:eid v)}}
        tx @(d/transact conn [tx-data])
        time (d/basis-t (:db-after tx))]
    (assoc m :time time)))

(def e2 (db-assoc e1 k v))

(def e3 (db-assoc e1 (push! :a) (push! {})))
