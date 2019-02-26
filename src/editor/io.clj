(ns editor.io
  (:require [clojure.datafy :refer [datafy]]
            [datomic.api :as d]
            editor.db))

(defprotocol Datomify
  (tx [this tempid]))

(defn datomify
  "Return Datomic tx object for given clojure data. Tempid of data is attached
  as metadata."
  [data]
  (let [id (str (gensym))]
    (with-meta (tx data id) {:tempid id})))

(extend-protocol Datomify
  java.lang.Long
  (tx [v id]
    {:db/id      id
     :form/type  :type.number/long
     :long/value v})

  java.lang.Double
  (tx [v id]
    {:db/id        id
     :form/type    :type.number/double
     :double/value v})

  java.lang.String
  (tx [v id]
    {:db/id        id
     :form/type    :type/string
     :string/value v})

  java.lang.Boolean
  (tx [v id]
    {:db/id         id
     :form/type     :type/boolean
     :boolean/value v})

  java.util.regex.Pattern
  (tx [v id]
    {:db/id        id
     :form/type    :type/regex
     :regex/value (str v)})

  clojure.lang.Symbol
  (tx [v id]
    {:db/id        id
     :form/type    :type/symbol
     :symbol/value (str v)})

  clojure.lang.Keyword
  (tx [v id]
    {:db/id         id
     :form/type     :type/keyword
     :keyword/value v})

  clojure.lang.PersistentHashSet
  (tx [v id]
    {:db/id       id
     :form/type   :type/set
     :set/element (map datomify v)})

  clojure.lang.PersistentVector
  (tx [v id]
    {:db/id     id
     :form/type :type/vector
     :vector/element
     (map-indexed (fn [i x]
                    {:db/id                (d/tempid :db.part/user)
                     :vector.element/index i
                     :vector.element/value (datomify x)})
                  v)})

  clojure.lang.PersistentHashMap
  (tx [v id]
    {:db/id       id
     :form/type   :type/map
     :map/element (map (fn [[k v]]
                         {:db/id             (d/tempid :db.part/user)
                          :map.element/key   (datomify k)
                          :map.element/value (datomify v)})
                       v)})

  ;; FIXME: Identical implementations... use extend
  clojure.lang.PersistentArrayMap
  (tx [v id]
    {:db/id       id
     :form/type   :type/map
     :map/element (map (fn [[k v]]
                         {:db/id             (d/tempid :db.part/user)
                          :map.element/key   (datomify k)
                          :map.element/value (datomify v)})
                       v)})

  clojure.lang.PersistentList
  (tx [[h & t] id]
    (merge {:db/id     id
            :form/type :type/list
            :list/head (datomify h)}
           (when t
             {:list/tail (datomify t)})))

  clojure.lang.Cons
  (tx [[h & t] id]
    (merge {:db/id     id
            :form/type :type/list
            :list/head (datomify h)}
           (when t
             {:list/tail (datomify t)})))

  nil
  (tx [_ _]
    {:db/id     (d/tempid :db.part/user)
     :form/type :type/nil}))

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

(defmethod clojurise :type/boolean
  [{:keys [:boolean/value]}]
  value)

(defmethod clojurise :type/symbol
  [{:keys [:symbol/value]}]
  (symbol value))

(defmethod clojurise :type/nil
  [_]
  nil)

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

(def cps
  '[:form/type
    :long/value
    :double/value
    :string/value
    :keyword/value
    :symbol/value
    :boolean/value
    :vector.element/index
    {:set/element          ...
     :list/head            ...
     :list/tail            ...
     :vector/element       ...
     :vector.element/value ...
     :map/element          ...
     :map.element/key      ...
     :map.element/value    ...}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Read-write data to datomic.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def conn editor.db/conn)

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
