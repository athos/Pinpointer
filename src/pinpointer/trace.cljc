(ns pinpointer.trace
  (:require [spectrace.core :as strace]))

(defn- partition-trace [t]
  (loop [t t, chunk [], prev nil, ret []]
    (if (empty? t)
      (conj ret chunk)
      (let [{:keys [val snapshots] :as frame} (first t)]
        (if (and (seq snapshots)
                 prev
                 (not= prev (peek snapshots)))
          (recur (rest t) [frame] nil (conj ret chunk))
          (recur (rest t) (conj chunk frame) val ret))))))

(defn- trace [t]
  (letfn [(diff-steps [steps1 steps2]
            (let [index (max 0 (- (count steps1) (count steps2)))
                  suffix (subvec steps1 index)]
              (if (= suffix steps2)
                (subvec steps1 0 index)
                steps1)))
          (add-to-chunk [chunk [curr next]]
            (conj chunk
                  (cond-> {:spec (:spec curr)
                           :val (:val curr)
                           :steps (if next
                                    (diff-steps (:in curr) (:in next))
                                    (:in curr))}
                    (:reason curr) (assoc :reason (:reason curr)))))]
    (mapv (fn [chunk]
            (reduce add-to-chunk [] (partition 2 1 [nil] chunk)))
          (partition-trace t))))

(defn traces [ed]
  (mapv trace (strace/traces ed)))
