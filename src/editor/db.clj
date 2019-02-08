(ns editor.db
  (:require [datomic.api :as d]))

(def schema
  [
   ;; Clojure data types (partial list))

   {:db/ident :form/type
    :db/valueType :db.type/ref
    :db/isComponent true
    :db/cardinality :db.cardinality/one
    :db/doc "Clojure data type as opposed to datomic schema type."}

   {:db/ident :type.number/long}
   {:db/ident :type.number/double}
   {:db/ident :type/string}
   {:db/ident :type/keyword}
   {:db/ident :type/symbol}
   {:db/ident :type/list}
   {:db/ident :type/vector}
   {:db/ident :type/set}
   {:db/ident :type/map}

   ;; Atomic values

   {:db/ident :long/value
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long}

   {:db/ident :double/value
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/double}

   {:db/ident :keyword/value
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/keyword}

   {:db/ident :string/value
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}

   {:db/ident :symbol/value
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/string}

   ;; Collections

   {:db/ident :set/element
    :db/isComponent true
    :db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}

   {:db/ident :list/head
    :db/isComponent true
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}

   {:db/ident :list/tail
    :db/isComponent true
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}

   {:db/ident :vector/element
    :db/isComponent true
    :db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}

   {:db/ident :vector.element/index
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/long}

   {:db/ident :vector.element/value
    :db/isComponent true
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}

   {:db/ident :map/element
    :db/isComponent true
    :db/cardinality :db.cardinality/many
    :db/valueType :db.type/ref}

   {:db/ident :map.element/key
    :db/isComponent true
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}

   {:db/ident :map.element/value
    :db/isComponent true
    :db/cardinality :db.cardinality/one
    :db/valueType :db.type/ref}

   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
   ;; Functions
   ;;
   ;; Functions are special. There's a fundamental difference between code and
   ;; data, or between code intended to be interpreted by the clojure compiler
   ;; and what is generally referred to as data in the clojure community.
   ;;
   ;; The difference is that code is intended to *do* something whereas data is
   ;; intended to *express* something. Think of it as operational versus
   ;; declarative.
   ;;
   ;; is (fn [x] x) the same as (fn [y] y)? That depends on how you view the
   ;; "data". Interpreted as data they are different, but interpreted as lambdas
   ;; they are alpha-equivalent, which means for all operational intents and
   ;; purposes they are the same. Subtle. Fascinating?
   ;;
   ;; For our present purposes I think it suffices to define a function to be
   ;; data that satisfies certain criteria. To wit:
   ;;
   ;; a function is a list whose first element is the symbol 'fn and whose
   ;; second is a vector. Furthermore any symbols in the function must be either
   ;; bound locals, or part of the environment. Here's where it gets slightly
   ;; weird though: the environment is not a reified thing in the typical
   ;; sense. There's no global mutable variable like a namespace of a typical
   ;; clojure file. Instead, the environment is specific to a single function
   ;; and maps the symbol in the function to a namespace qualified symbol as
   ;; well as an entity id. The entity id refers to the exact code defining the
   ;; data or function referred to, whereas the namespace reference allows you
   ;; to know when you're pointing at something stale. I'm not sure this is the
   ;; right way to do it. We don't want to overwrite namespaces after all; we
   ;; want to update them as immutable structures.
   ;;
   ;; Maybe a better way to think about it would be to have a symbol point at a
   ;; namespace and a name within a version. Versions being linked into chains
   ;; like branches in git so that if we want to compare what a symbol points at
   ;; now to what it would point at at the head of "master" then we can. Now we
   ;; need tooling to quickly update references like that. Or do we? Do we want
   ;; updates like that to be easy, or do we want to encourage people to stick
   ;; with code that works and not modify it unless there's real value to be
   ;; had? Churn is a major problem which is only getting worse.
   ;;
   ;; Macros present some challenges that I haven't even begun to explore.
   ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

   {:db/ident :function/form
    :db/doc "Source sexp of fn"
    :db/isComponent true
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :function/environment
    :db/isComponent true
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :function.environment/binding
    :db/isComponent true
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :function.environment.binding/symbol
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :function.environment.binding/reference
    :db/doc "Namespace binding to which this symbol refers"
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   ;; Namespaces

   {:db/ident :version/tag
    :db/valueType :db.type/keyword
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one}

   {:db/ident :version/topology
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}

   {:db/ident :version/namespace
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :namespace/name
    :db/valueType :db.type/string
    ;; Should this be unique?
    :db/cardinality :db.cardinality/one}

   {:db/ident :namespace/binding
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/many}

   {:db/ident :namespace.binding/symbol
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/many}

   {:db/ident :namespace.binding/form
    :db/valueType :db.type/ref
    :db/cardinality :db.cardinality/one}])

(def db-uri "datomic:mem://test" #_"datomic:free://localhost:4334/dummy")

(def conn
  "Deletes DB, recreates it and adds schema. Only use for early dev."
  (do
    (d/delete-database db-uri)
    (d/create-database db-uri)
    (let [c (d/connect db-uri)]
      (d/transact c schema)
      (d/transact c [{:version/tag :master}])
      c)))
