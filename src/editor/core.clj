(ns editor.core
  (:require [datomic.api :as d]))

(def db-uri "datomic:mem://test" #_"datomic:free://localhost:4334/dummy")

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

   {:db/ident :map
    :db/valueType :db.type/double
    :db/cardinality :db.cardinality/one}

   ;; types

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
    :db/valueType :db.type/ref}])

(defprotocol Datomify
  (tx [this]))

(extend-protocol Datomify
  java.lang.Long
  (tx [v]
    {:db/id (d/tempid :db.part/user)
     :form/type :type.number/long
     :long/value v})

  java.lang.Double
  (tx [v]
    {:db/id (d/tempid :db.part/user)
     :form/type :type.number/double
     :double/value v})

  java.lang.String
  (tx [v]
    {:db/id (d/tempid :db.part/user)
     :form/type :type/string
     :string/value v})

  clojure.lang.Symbol
  (tx [v]
    {:db/id (d/tempid :db.part/user)
     :form/type :type/symbol
     :symbol/value (str v)})

  clojure.lang.Keyword
  (tx [v]
    {:db/id (d/tempid :db.part/user)
     :form/type :type/keyword
     :keyword/value v})

  clojure.lang.PersistentHashSet
  (tx [v]
    {:db/id (d/tempid :db.part/user)
     :form/type :type/set
     :set/element (map tx v)})

  clojure.lang.PersistentVector
  (tx [v]
    {:db/id (d/tempid :db.part/user)
     :form/type :type/vector
     :vector/element (map-indexed (fn [i x]
                                    {:db/id (d/tempid :db.part/user)
                                     :vector.element/index i
                                     :vector.element/value (tx x)})
                                  v)})

  clojure.lang.PersistentHashMap
  (tx [v]
    {:db/id (d/tempid :db.part/user)
     :form/type :type/map
     :map/element (map (fn [[k v]]
                         {:db/id (d/tempid :db.part/user)
                          :map.element/key (tx k)
                          :map.element/value (tx v)})
                       v)})

  ;; FIXME: Identical implementations... use extend
  clojure.lang.PersistentArrayMap
  (tx [v]
    (println (meta v))
    (let [tid (if (:top (meta v)) "top-level" (d/tempid :db.part/user))]
      {:db/id tid
       :form/type :type/map
       :map/element (map (fn [[k v]]
                           {:db/id (d/tempid :db.part/user)
                            :map.element/key (tx k)
                            :map.element/value (tx v)})
                         v)}))

  clojure.lang.PersistentList
  (tx [[h & t]]
    (merge {:db/id (d/tempid :db.part/user)
            :form/type :type/list
            :list/head (tx h)}
           (when t
             {:list/tail (tx t)}))))

(defmulti clojurise (fn [x] (-> x :form/type :db/ident)))

(defmethod clojurise :type.number/long
  [{:keys [:long/value]}]
  value)

(defmethod clojurise :type.number/double
  [{:keys [:double/value]}]
  value)

(defmethod clojurise :type/string
  [{:keys [:string/value]}]
  value)

(defmethod clojurise :type/keyword
  [{:keys [:keyword/value]}]
  value)

(defmethod clojurise :type/symbol
  [{:keys [:symbol/value]}]
  (symbol value))

(defmethod clojurise :type/set
  [{:keys [:set/element]}]
  (into #{} (map clojurise element)))

(defmethod clojurise :type/list
  [{:keys [:list/head :list/tail] :as x}]
  (conj (when tail (clojurise tail)) (clojurise head)))

(defmethod clojurise :type/vector
  [{:keys [:vector/element]}]
  (into [] (comp (map :vector.element/value) (map clojurise))
        (sort-by :vector.element/index element)))

(defmethod clojurise :type/map
  [{:keys [:map/element]}]
  (into {} (map (fn [{:keys [:map.element/key :map.element/value]}]
                  [(clojurise key) (clojurise value)]))
        element))

(defn save-to-datomic! [conn data]
  ;; TODO: return id of new entity, not entire tx log
  (get (:tempids @(d/transact conn [(tx (with-meta data {:top true}))])) "top-level"))

(defn pull-from-datomic! [db eid]
  (clojurise (d/pull db '[*] eid)))

(defn init-schema [uri]
  (d/create-database uri)
  (d/transact (d/connect uri) schema))
