(ns pinpointer.trace
  (:require [spectrace.core :as strace]))

(defn- trace [t]
  (letfn [(diff-steps [steps1 steps2]
            (reverse (drop (count steps1) (rseq steps2))))]
    (if (= (count t) 1)
      ;;FIXME: this case should be removed
      (let [{:keys [spec val in]} (first t)]
        [{:spec spec :val val :steps in}])
      (reduce (fn [t [curr next]]
                (cond-> t
                  (not= (:val curr) (:val next))
                  (conj {:spec (:spec curr)
                         :val (:val curr)
                         :steps (diff-steps (:in next) (:in curr))})))
              []
              (partition 2 1 t)))))

(defn traces [ed]
  (mapv trace (strace/traces ed)))
