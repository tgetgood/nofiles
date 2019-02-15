(ns editor.core
  (:require [clojure.datafy :refer [datafy]]
            [datomic.api :as d]
            [editor.db :as db :refer [conn]]
            [editor.io :refer [clojurise datomify cps]])
  (:import [java.io File])
  )

;;;;; Read/Write forms to datomic

(defn push! [data]
  (let [tx-data (datomify data)
        tx @(d/transact conn [tx-data])]
    {:eid (get-in tx [:tempids (:tempid (meta tx-data))])
     :time (d/basis-t (:db-before tx))}))

(defn pull! [{:keys [eid time] :as accessor}]
  (let [db (if time (d/as-of (d/db conn) time) (d/db conn))
        d  (clojurise (d/pull db cps eid))]
    (if (instance? clojure.lang.IMeta d)
      (with-meta d (assoc (meta d) :db-link accessor))
      d)))

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

(reindex-code!)
