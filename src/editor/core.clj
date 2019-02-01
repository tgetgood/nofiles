(ns editor.core
  (:require [datomic.api :as d]
            [clojure.datafy :refer [datafy]]
            [clojure.walk :as walk]))

(def db-uri "datomic:mem://test" #_"datomic:free://localhost:4334/dummy")

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
    :db/cardinality :db.cardinality/one}])

(defn tempid [v]
  (if-let [tag (:nofiles/tag (meta v))] tag (d/tempid :db.part/user)))

(defprotocol Datomify
  (tx [this]))

(extend-protocol Datomify
  java.lang.Long
  (tx [v]
    {:db/id (tempid v)
     :form/type :type.number/long
     :long/value v})

  java.lang.Double
  (tx [v]
    {:db/id (tempid v)
     :form/type :type.number/double
     :double/value v})

  java.lang.String
  (tx [v]
    {:db/id (tempid v)
     :form/type :type/string
     :string/value v})

  clojure.lang.Symbol
  (tx [v]
    {:db/id (tempid v)
     :form/type :type/symbol
     :symbol/value (str v)})

  clojure.lang.Keyword
  (tx [v]
    {:db/id (tempid v)
     :form/type :type/keyword
     :keyword/value v})

  clojure.lang.PersistentHashSet
  (tx [v]
    {:db/id (tempid v)
     :form/type :type/set
     :set/element (map tx v)})

  clojure.lang.PersistentVector
  (tx [v]
    {:db/id (tempid v)
     :form/type :type/vector
     :vector/element (map-indexed (fn [i x]
                                    {:db/id (d/tempid :db.part/user)
                                     :vector.element/index i
                                     :vector.element/value (tx x)})
                                  v)})

  clojure.lang.PersistentHashMap
  (tx [v]
    {:db/id (tempid v)
     :form/type :type/map
     :map/element (map (fn [[k v]]
                         {:db/id (d/tempid :db.part/user)
                          :map.element/key (tx k)
                          :map.element/value (tx v)})
                       v)})

  ;; FIXME: Identical implementations... use extend
  clojure.lang.PersistentArrayMap
  (tx [v]
    {:db/id (tempid v)
     :form/type :type/map
     :map/element (map (fn [[k v]]
                         {:db/id (d/tempid :db.part/user)
                          :map.element/key (tx k)
                          :map.element/value (tx v)})
                       v)})

  clojure.lang.PersistentList
  (tx [[h & t :as w]]
    (merge {:db/id (tempid w)
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

(defn init-schema [uri]
  (d/create-database uri)
  (d/transact (d/connect uri) schema))

;;;;; Read/Write forms to datomic

(defn save-to-datomic! [conn data]
  (let [tag (str (gensym))]
    (get (:tempids @(d/transact conn [(tx (with-meta data {:nofiles/tag tag}))])) tag)))

(defn pull-from-datomic! [db eid]
  (clojurise (d/pull db '[*] eid)))

;;;;; Dealing with execution

(defn builtin? [x]
  (contains? (:publics (datafy (the-ns 'clojure.core))) x))

(defn write-fn! [conn form]
  (get-in @(d/transact conn [{:db/id "the-fn"
                          :function/form (tx form)}])
          [:tempids "the-fn"]))

(defn load-fn! [db fid]
  (let [eid (d/q [:find '?e '. :where [fid :function/form '?e]] db)
        form (pull-from-datomic! db eid)]
    (with-meta (eval form) {:source-form form
                            :db/id fid})))
