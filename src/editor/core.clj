(ns editor.core
  (:require [clojure.datafy :refer [datafy]]
            [clojure.pprint :refer [pprint]]
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

(def empty-codebase
  {:topology {}
   :namespaces {:dumpalump.core {:test '(fn [] "I do nothing.")}}})

(def create-master-tx
  [{:version/tag :master
    :version/namespace (datomify empty-codebase)}])

(defn pull-var [sym]
  (get-in
   (clojurise
    (d/q '[:find (pull ?x [*]) .
           :where
           [?e :version/tag :master]
           [?e :version/namespace ?x]]
         (d/db conn)))
   [:namespaces (keyword (namespace sym)) (keyword (name sym))]))

(defn str-v [sym]
  (with-out-str (pprint (pull-var sym))))
