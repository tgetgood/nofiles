(ns editor.core
  (:require [clojure.datafy :refer [datafy]]
            [datomic.api :as d]
            [editor.db :refer [conn]]
            [editor.io :refer [datomify clojurise]]))

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

(defn get-ns [db branch ns]
  (d/q '[:find ?ns . :in $ ?branch ?ns-name
         :where
         [?vid :version/tag ?branch]
         [?vid :version/namespace ?ns]
         [?ns :namespace/name ?ns-name]]
       db branch ns))

(defn get-eid [db branch ns sym]
  (d/q '[:find ?eid .
         :in $ ?branch ?ns-name ?sym-name
         :where
         [?vid :version/tag ?branch]
         [?vid :version/namespace ?ns]
         [?ns :namespace/name ?ns-name]
         [?ns :namespace/binding ?b]
         [?b :namespace.binding/symbol ?sym-name]
         [?b :namespace.binding/form ?eid]]]
  db branch ns sym))

(defn create-sym-and-ns-tx [branch ns-name sym]
  [{:version/tag branch
    :version/namespace
    {:namespace/name ns-name
     :namespace/binding
     {:namespace.binding/name ns-name
      :namespace.binding/form
      {:db/id "new-form"}}}}])

(defn create-)

(defn create-or-load-sym [branch sym]
  (let [ns (namespace sym)
        n (name sym)
        db (d/db conn)
        time (dec (d/next-t db))]
    (cond
      {:eid eid :time time}
      )
    ))
