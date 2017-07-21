(ns pinpointer.formatter
  #?(:clj (:refer-clojure :exclude [format]))
  (:require [clojure.spec.alpha :as s]
            [fipp.edn :as edn]
            [fipp.engine :as fipp]
            [fipp.visit :as visit]))

(defmulti render
  (fn [{:keys [spec]} f printer x] (when (seq? spec) (first spec))))
(defmethod render :default [_ f printer x]
  (f (:base-printer printer) x))

(defn- highlight [x]
  [:span [:escaped "\000"] x [:escaped "\001"]])

(defn- wrap [f {:keys [trace] :as printer} x]
  (cond (or (empty? trace)
            ;;FIXME: this condition should be removed
            (and (= (count trace) 1) (empty? (:steps printer))))
        (highlight (f (:base-printer printer) x))

        (= x (:val (first trace)))
        (render (first trace) f printer x)

        :else (f (:base-printer printer) x)))

(defn pop-trace [printer]
  (update printer :trace rest))

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

(defn highlight-printer [trace]
  (let [base-printer (edn/map->EdnPrinter {:symbols {}})]
    (->HighlightPrinter base-printer trace)))

(defn format [x trace]
  (let [printer (highlight-printer trace)]
    (with-out-str
      (fipp/pprint-document (visit/visit printer x) {}))))

;;
;; Method implementations of `render`
;;

(defn- pretty-coll [printer open xs sep close f]
  (let [xform (comp (map-indexed #(f printer %1 %2))
                    (interpose sep))
        ys (sequence xform xs)]
    [:group open ys close]))

(defn render-coll
  ([frame printer x]
   (render-coll frame printer x nil))
  ([{[n] :steps} printer x each-fn]
   (let [[open close] (cond (seq? x) ["(" ")"]
                            (vector? x) ["[" "]"]
                            (map? x) ["{" "}"]
                            (set? x) ["#{" "}"])
         sep (if (map? x) [:span "," :line] :line)
         each-fn (if each-fn
                   #(each-fn %2 %3)
                   (fn [_ i x]
                     (visit/visit (cond-> printer (= i n) pop-trace) x)))]
     (pretty-coll printer open x sep close each-fn))))

(defmethod render `s/tuple [frame _ printer x]
  (render-coll frame printer x))

(defn- render-every [{[n] :steps :as frame} printer x]
  (if (map? x)
    (render-coll frame printer x
      (fn [i [k v]]
        (let [printer (cond-> printer (= i n) pop-trace)]
          [:span (visit/visit printer k) " " (visit/visit printer v)])))
    (render-coll frame printer x)))

(defmethod render `s/every [frame _ printer x]
  (render-every frame printer x))

(defmethod render `s/coll-of [frame _ printer x]
  (render-every frame printer x))

(defn- render-every-kv [{[key k-or-v] :steps :as frame} printer x]
  (render-coll frame printer x
    (fn [i [k v]]
      (let [kprinter (cond-> printer
                       (and (= k key) (= k-or-v 0))
                       pop-trace)
            vprinter (cond-> printer
                       (and (= k key) (= k-or-v 1))
                       pop-trace)]
        [:span (visit/visit kprinter k) " " (visit/visit vprinter v)]))))

(defmethod render `s/every-kv [frame _ printer x]
  (render-every-kv frame printer x))

(defmethod render `s/map-of [frame _ printer x]
  (render-every-kv frame printer x))

(defmethod render `s/keys [{[key] :steps :as frame} _ printer x]
  (render-coll frame printer x
    (fn [i [k v]]
      (let [vprinter (cond-> printer (= k key) pop-trace)]
        [:span (visit/visit printer k) " " (visit/visit vprinter v)]))))

(defmethod render `s/cat [frame _ printer x]
  (render-coll frame printer x))

(defmethod render `s/& [frame _ printer x]
  (render-coll frame printer x))

(defmethod render `s/alt [frame _ printer x]
  (render-coll frame printer x))

(defmethod render `s/? [frame _ printer x]
  (render-coll frame printer x))

(defmethod render `s/* [frame _ printer x]
  (render-coll frame printer x))

(defmethod render `s/+ [frame _ printer x]
  (render-coll frame printer x))
