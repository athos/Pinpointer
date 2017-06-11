(ns pinpointer.printer
  (:refer-clojure :exclude [pr prn pr-str])
  (:require [fipp.edn :as edn]
            [fipp.engine :as fipp]
            [fipp.visit :as visit]))

(def ^:dynamic *trace*)

(defn- wrap-highlighting [f]
  (bound-fn [& args]
    (if (empty? *trace*)
      [:group "<<<" (apply f args) ">>>"]
      (apply f args))))

(defn- with-trace-pop [x f]
  #_(clojure.core/prn :trace *trace*)
  #_(clojure.core/prn :x x)
  (if (= x (:val (first *trace*)))
    (binding [*trace* (rest *trace*)]
      (f))
    (f)))

(defrecord HighlightPrinter [base-printer]
  visit/IVisitor
  (visit-unknown [this x]
    ((wrap-highlighting visit/visit-unknown) base-printer x))
  (visit-nil [this]
    ((wrap-highlighting visit/visit-nil) base-printer))
  (visit-boolean [this x]
    ((wrap-highlighting visit/visit-boolean) base-printer x))
  (visit-string [this x]
    ((wrap-highlighting visit/visit-string) base-printer x))
  (visit-character [this x]
    ((wrap-highlighting visit/visit-character) base-printer x))
  (visit-symbol [this x]
    ((wrap-highlighting visit/visit-symbol) base-printer x))
  (visit-keyword [this x]
    ((wrap-highlighting visit/visit-keyword) base-printer x))
  (visit-number [this x]
    ((wrap-highlighting visit/visit-number) base-printer x))
  (visit-seq [this x]
    (with-trace-pop x
      (wrap-highlighting
        #(edn/pretty-coll this "(" x :line ")" visit/visit))))
  (visit-vector [this x]
    (with-trace-pop x
      (wrap-highlighting
        #(edn/pretty-coll this "[" x :line "]" visit/visit))))
  (visit-map [this x]
    (with-trace-pop x
      (wrap-highlighting
        #(edn/pretty-coll this "{" x [:span "," :line] "}"
           (fn [printer [k v]]
             [:span (visit/visit printer k) " " (visit/visit printer v)])))))
  (visit-set [this x]
    (with-trace-pop x
      (wrap-highlighting
        #(edn/pretty-coll this "#{" x :line "}" visit/visit))))
  (visit-tagged [this x]
    ((wrap-highlighting visit/visit-tagged) base-printer x))
  (visit-meta [this meta x]
    ((wrap-highlighting #(visit/visit-meta %1 meta %2)) base-printer x))
  (visit-var [this x]
    ((wrap-highlighting visit/visit-var) base-printer x))
  (visit-pattern [this x]
    ((wrap-highlighting visit/visit-pattern) base-printer x))
  (visit-record [this x]
    ((wrap-highlighting visit/visit-record) base-printer x))
  )

(defn highlight-printer []
  (let [base-printer (edn/map->EdnPrinter {:symbols {}})]
    (->HighlightPrinter base-printer)))

(defn pprint [x trace]
  (let [printer (highlight-printer)]
    (binding [*trace* trace]
      (fipp/pprint-document (visit/visit printer x) {}))))

(defn pr [x])

(defn prn [x]
  (pr x)
  (newline))

(defn pr-str [x]
  (with-out-str
    (pr x)))
