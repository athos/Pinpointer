(ns pinpointer.printer
  (:refer-clojure :exclude [pr prn pr-str])
  (:require [clojure.spec.alpha :as s]
            [fipp.edn :as edn]
            [fipp.engine :as fipp]
            [fipp.visit :as visit]))

(defmulti render
  (fn [{:keys [spec]} f printer x] (when (seq? spec) (first spec))))
(defmethod render :default [_ f printer x]
  (f (:base-printer printer) x))

(defn- wrap [f printer x]
  (cond (empty? (:trace printer))
        [:group "<<<" (f (:base-printer printer) x) ">>>"]

        (= x (:val (first (:trace printer))))
        (render (first (:trace printer)) f printer x)

        :else (f (:base-printer printer) x)))

(defn pop-trace [printer]
  (update printer :trace rest))

(defn- pretty-coll [printer open xs sep close f]
  (let [xform (comp (map-indexed #(f printer %1 %2))
                    (interpose sep))
        ys (sequence xform xs)]
    [:group open ys close]))

(defrecord HighlightPrinter [base-printer trace]
  visit/IVisitor
  (visit-unknown [this x]
    (wrap visit/visit-unknown this x))
  (visit-nil [this]
    (wrap (fn [printer _] (visit/visit-nil printer)) this nil))
  (visit-boolean [this x]
    (wrap visit/visit-boolean this x))
  (visit-string [this x]
    (wrap visit/visit-string this x))
  (visit-character [this x]
    (wrap visit/visit-character this x))
  (visit-symbol [this x]
    (wrap visit/visit-symbol this x))
  (visit-keyword [this x]
    (wrap visit/visit-keyword this x))
  (visit-number [this x]
    (wrap visit/visit-number this x))
  (visit-seq [this x]
    (wrap visit/visit-seq this x))
  (visit-vector [this x]
    (wrap visit/visit-vector this x))
  (visit-map [this x]
    (wrap visit/visit-map this x))
  (visit-set [this x]
    (wrap visit/visit-set this x))
  (visit-tagged [this x]
    (wrap visit/visit-tagged this x))
  (visit-meta [this meta x]
    (wrap #(visit/visit-meta %1 meta %2) this x))
  (visit-var [this x]
    (wrap visit/visit-var this x))
  (visit-pattern [this x]
    (wrap visit/visit-pattern this x))
  (visit-record [this x]
    (wrap visit/visit-record this x))
  )

(defmethod render `s/tuple [{[n] :steps} _ printer x]
  (pretty-coll printer "[" x :line "]"
    (fn [printer i x]
      (visit/visit (cond-> printer (= i n) pop-trace) x))))

(defmethod render `s/map-of [{[key k-or-v] :steps} _ printer x]
  (pretty-coll printer "{" x [:span "," :line] "}"
    (fn [printer i [k v]]
      (let [kprinter (cond-> printer
                       (and (= k key) (= k-or-v 0))
                       pop-trace)
            vprinter (cond-> printer
                       (and (= k key) (= k-or-v 1))
                       pop-trace)]
        [:span (visit/visit kprinter k) " " (visit/visit vprinter v)]))))

(defn highlight-printer [trace]
  (let [base-printer (edn/map->EdnPrinter {:symbols {}})]
    (->HighlightPrinter base-printer trace)))

(defn pprint [x trace]
  (let [printer (highlight-printer trace)]
    (fipp/pprint-document (visit/visit printer x) {})))

(defn pr [x])

(defn prn [x]
  (pr x)
  (newline))

(defn pr-str [x]
  (with-out-str
    (pr x)))
