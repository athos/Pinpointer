(ns pinpointer.core
  (:require #?@(:clj [[clojure.spec :as s]
                      [rewrite-clj.zip :as z]
                      clansi])))

(defn extract* [z [k & more :as in]]
  (if (empty? in)
    (let [pos (dec (second (z/position z)))]
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

(defn colorize-by [colorize-fn s]
  (if colorize-fn
    (if (= colorize-fn :ansi)
      (clansi/style s :yellow)
      (colorize-fn s))
    s))

(defn pinpoint-out
  ([ed] (pinpoint-out ed {}))
  ([ed {:keys [root colorize]}]
   (when-let [problems (::s/problems ed)]
     (doseq [[i probs] (->> problems
                            (group-by #(select-keys % [:val :in]))
                            (map-indexed vector))
             :let [[{:keys [val in]} probs] probs
                   val (or root val)
                   [start end] (extract val in)
                   sval (pr-str val)]]
       (when (not= i 0)
         (newline))
       #_(doseq [prob probs]
         (print " Problem: ")
         (prn (assoc prob :val val)))
       (println "   Input:" (str (subs sval 0 start)
                                 (colorize-by colorize (subs sval start end))
                                 (when (> (count sval) end)
                                   (subs sval end))))
       (print  "        : ")
       (println (colorize-by colorize (wavy-line start end)))
       (print "Expected: ")
       (doseq [[j {:keys [pred reason]}] (map-indexed vector probs)]
         (when (not= j 0)
           (print "          "))
         (pr pred)
         (when reason
           (print (str " (" reason ")")))
         (newline))))))

(defn pinpoint
  ([spec x] (pinpoint spec x {}))
  ([spec x opts]
   (pinpoint-out (s/explain-data spec x)
                 (merge {:root x} opts))))
