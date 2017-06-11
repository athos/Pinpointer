(ns pinpointer.trace
  (:require [spectrace.trace :as trace]))

(defn trace [t]
  (letfn [(diff-steps [steps1 steps2]
            (reverse (drop (count steps1) (rseq steps2))))]
    (reduce (fn [t [curr next]]
              (cond-> t
                (not= (:val curr) (:val next))
                (conj {:spec (:spec curr)
                       :val (:val curr)
                       :steps (diff-steps (:in next) (:in curr))})))
            []
            (partition 2 1 t))))

(defn traces [ed]
  (mapv trace (trace/traces ed)))
