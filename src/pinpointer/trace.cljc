(ns pinpointer.trace
  (:require [clojure.spec.alpha :as s]
            [spectrace.trace :as trace]))

(defn trace [problem spec value]
  (let [diff-steps (fn [steps1 steps2]
                     (reverse (drop (count steps1) (rseq steps2))))
        trace (trace/trace problem spec value)]
    (reduce (fn [t [curr next]]
              (cond-> t
                (not= (:val curr) (:val next))
                (conj {:spec (:spec curr)
                       :val (:val curr)
                       :steps (diff-steps (:in next) (:in curr))})))
            []
            (partition 2 1 trace))))

(defn traces [{:keys [::s/problems ::s/spec ::s/value]}]
  (mapv #(trace % spec value) problems))
