(ns pinpointer.core
  (:require [clojure.spec :as s]
            [rewrite-clj.zip :as z]))

(defn extract* [z [k & more :as in]]
  (if (empty? in)
    (let [[_ pos] (z/position z)]
      [pos (+ pos (z/length z))])
    (cond (z/vector? z) (recur (z/get z k) more)
          (z/map? z) (let [z' (z/get z k)]
                       (recur (if (= (first more) 0) (z/left z') z')
                              (rest more))))))

(defn extract [val in]
  (let [z (z/of-string (pr-str val) {:track-position? true})]
    (extract* z in)))

(defn wavy-line [start end]
  (apply str (concat (repeat start \space)
                     (repeat (- end start) \^))))

(defn pinpoint-out [ed & {:keys [root]}]
  (when-let [problems (::s/problems ed)]
    (doseq [[i problem] (map-indexed vector problems)
            :let [{:keys [val in pred reason]} problem
                  val (or root val)
                  [start end] (extract val in)]]
      (when (not= i 0)
        (newline))
      #_(print " Problem: ")
      #_(prn (assoc problem :val val))
      (println "   Input:" (pr-str val))
      (printf  "        :%s\n" (wavy-line start end))
      (print "Expected: ")
      (prn pred)
      (when reason
        (printf "(%s)\n" reason)))))

(defn pinpoint [spec x]
  (pinpoint-out (s/explain-data spec x) :root x))
