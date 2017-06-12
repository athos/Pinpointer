(ns pinpointer.printer
  (:refer-clojure :exclude [pr prn pr-str])
  (:require [fipp.edn :as edn]
            [fipp.engine :as fipp]
            [fipp.visit :as visit]))

(defn- with-highlighting [printer f & args]
  (if (empty? (:trace printer))
    [:group "<<<" (apply f (:base-printer printer) args) ">>>"]
    (apply f (:base-printer printer) args)))

(defn- pop-trace [printer]
  (update printer :trace rest))

(defn- pop-trace-if-matches [printer x]
  (if (= x (:val (first (:trace printer))))
    (pop-trace printer)
    printer))

(defn- pretty-coll [printer open xs sep close f]
  (let [xform (comp (map #(f printer %))
                    (interpose sep))
        ys (sequence xform xs)]
    [:group open ys close]))

(defrecord HighlightPrinter [base-printer trace]
  visit/IVisitor
  (visit-unknown [this x]
    (with-highlighting this visit/visit-unknown x))
  (visit-nil [this]
    (with-highlighting this visit/visit-nil))
  (visit-boolean [this x]
    (with-highlighting this visit/visit-boolean x))
  (visit-string [this x]
    (with-highlighting this visit/visit-string x))
  (visit-character [this x]
    (with-highlighting this visit/visit-character x))
  (visit-symbol [this x]
    (with-highlighting this visit/visit-symbol x))
  (visit-keyword [this x]
    (with-highlighting this visit/visit-keyword x))
  (visit-number [this x]
    (with-highlighting this visit/visit-number x))
  (visit-seq [this x]
    (with-highlighting this
      (fn [_]
        (let [printer (pop-trace-if-matches this x)]
          (edn/pretty-coll printer "(" x :line ")" visit/visit)))))
  (visit-vector [this x]
    (with-highlighting this
      (fn [_]
        (let [printer (pop-trace-if-matches this x)]
          (edn/pretty-coll printer "[" x :line "]" visit/visit)))))
  (visit-map [this x]
    (with-highlighting this
      (fn [_]
        (let [printer (pop-trace-if-matches this x)]
          (edn/pretty-coll printer "{" x [:span "," :line] "}"
            (fn [printer [k v]]
              [:span (visit/visit printer k) " " (visit/visit printer v)]))))))
  (visit-set [this x]
    (with-highlighting this
      (fn [_]
        (let [printer (pop-trace-if-matches this x)]
          (edn/pretty-coll printer "#{" x :line "}" visit/visit)))))
  (visit-tagged [this x]
    (with-highlighting this visit/visit-tagged x))
  (visit-meta [this meta x]
    (with-highlighting this #(visit/visit-meta %1 meta %2) x))
  (visit-var [this x]
    (with-highlighting this visit/visit-var x))
  (visit-pattern [this x]
    (with-highlighting this visit/visit-pattern x))
  (visit-record [this x]
    (with-highlighting this visit/visit-record x))
  )

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
