(ns editor.db
  (:require [datomic.api :as d]))

(def schema
  [
   ;; Clojure data types (partial list))

   {:db/ident       :form/type
    :db/valueType   :db.type/ref
    :db/isComponent true
    :db/index       true
    :db/cardinality :db.cardinality/one
    :db/doc         "Clojure data type as opposed to datomic schema type."}

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

   {:db/ident       :long/value
    :db/doc         "Value of a Long"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/long}

   {:db/ident       :double/value
    :db/doc         "Value of a Double"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/double}

   {:db/ident       :keyword/value
    :db/doc         "Value of a Keyword"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/keyword}

   {:db/ident       :string/value
    :db/doc         "Value of a String"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}

   {:db/ident       :symbol/value
    :db/doc         "Name of a Symbol"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/string}

   ;; Collections

   {:db/ident       :set/element
    :db/doc         "An element of a set"
    :db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}

   {:db/ident       :list/head
    :db/doc         "First element of a list"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/ref}

   {:db/ident       :list/tail
    :db/doc         "Rest of a list"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/ref}

   {:db/ident       :vector/element
    :db/doc         "Element of a vector"
    :db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}

   {:db/ident       :vector.element/index
    :db/doc         "Vector index of an element"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/long}

   {:db/ident       :vector.element/value
    :db/doc         "Value stored in a vector element"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/ref}

   {:db/ident       :map/element
    :db/doc         "Key value pair"
    :db/cardinality :db.cardinality/many
    :db/valueType   :db.type/ref}

   {:db/ident       :map.element/key
    :db/doc         "A map key"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/ref}

   {:db/ident       :map.element/value
    :db/doc         "A map value"
    :db/cardinality :db.cardinality/one
    :db/valueType   :db.type/ref}])

(def db-uri "datomic:mem://demo")

(def conn
  "Deletes DB, recreates it and adds schema. Only use for early dev."
  (do
    (d/delete-database db-uri)
    (d/create-database db-uri)
    (let [c (d/connect db-uri)]
      (d/transact c schema)
      c)))
