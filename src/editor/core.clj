(ns editor.core
  (:require [datomic.api :as d]))

(def db-uri "datomic:free://localhost:4334/dummy")

(def schema
  [{:db/ident :version/tag
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one}

   {:db/ident :version/topology
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :version/namespace
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :namespace/name
    :db/valueType :db.type/keyword
    ;; Should this be unique?
    :db/cardinality :db.cardinality/one}

   {:db/ident :namespace/binding
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :namespace.binding/name
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/many}

   {:db/ident :namespace.binding/form
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   ;; forms

   {:db/ident :fn/string
    :db/doc "Only fns and values can be named at present."
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}

   {:db/ident :fn/valid?
    :db/doc "Is this fn a valid sexp (reads)?"
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one}

   {:db/ident :value/string
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one}


   ]
  )

(defn init-schema [uri]
  (d/transact (d/connect uri) schema))
