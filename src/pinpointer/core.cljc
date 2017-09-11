(ns pinpointer.core
  (:require #?(:clj [clansi])
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [fipp.clojure :as fipp]
            [pinpointer.formatter :as formatter]
            [pinpointer.trace :as trace]))

(def ^:dynamic *colorize-fn*)

(defn- colorize [color s]
  (*colorize-fn* color s))

(defn- ansi-colorize [color s]
  #?(:clj (clansi/style s color)
     :cljs s))

(defn- none-colorize [_ s] s)

(def ^:private builtin-colorize-fns
  {:ansi ansi-colorize
   :none none-colorize})

(defn- choose-colorize-fn [colorize]
  (let [colorize (or colorize :none)]
    (if (keyword? colorize)
      (get builtin-colorize-fns colorize none-colorize)
      colorize)))

(defn- times [n c]
  (str/join (repeat n c)))

(defn- space [n]
  (times n \space))

(defn- wavy-line [n]
  (colorize :red (times n \^)))

(defn- format-line [line hiliting?]
  (let [[_ indent line'] (re-matches #"(\s*)(.*)" line)
        parts (str/split line' #"\000")]
    (if (= (count parts) 1)
      (if hiliting?
        [line (str (space (count indent)) (wavy-line (count line))) true]
        [line nil false])
      (loop [parts parts, hiliting? hiliting?, ret [indent], wavy [indent]]
        (if (empty? parts)
          [(str/join ret) (str/join wavy) (not hiliting?)]
          (let [[part & parts] parts]
            (if hiliting?
              (recur parts (not hiliting?)
                     (conj ret (colorize :red part))
                     (conj wavy (wavy-line (count part))))
              (recur parts (not hiliting?)
                     (conj ret part)
                     (conj wavy (space (count part)))))))))))

(defn- format-data [value trace]
  (let [lines (binding [formatter/*highlighting-mark* "\000"]
                (str/split (formatter/format value trace) #"\n"))]
    (loop [[line & more] lines, hiliting? false, ret []]
      (if-not line
        ret
        (let [[line wavy hiliting?] (format-line line hiliting?)]
          (recur more
                 hiliting?
                 (cond-> (conj ret line)
                   wavy (conj wavy))))))))

(defn- print-headline [nproblems]
  (if (= nproblems 1)
    (println " Detected 1 spec error:")
    (println " Detected" nproblems "spec errors:")))

(defn- hline []
  (->> " --------------------------------------------------"
       (colorize :cyan)
       println))

(defn- correct-paths [ed]
  (if (::s/args ed)
    ;; Probably :path in each problem has extra :args key
    (update ed ::s/problems
            (fn [problems] (map #(update % :path subvec 1) problems)))
    ed))

(defn- simplify-spec [spec]
  (walk/postwalk (fn [x]
                   (if (symbol? x)
                     (condp #(= (namespace %2) %1) x
                       "clojure.core" (symbol (name x))
                       "clojure.spec.alpha" (symbol "s" (name x))
                       x)
                     x))
                 spec))

(defn- print-error [total value [i problem trace]]
  (doseq [chunk trace
          :let [[line & lines] (format-data value chunk)]]
    (println (str " (" (inc i) "/" total ")\n"))
    (println "     Input:" line)
    (doseq [line lines]
      (println "          :" line))
    (let [[line & lines] (as-> (:pred problem) it
                           (simplify-spec it)
                           (with-out-str (fipp/pprint it))
                           (str/split it #"\n"))]
      (println "  Expected:" line)
      (doseq [line lines]
        (println "          :" line)))
    (when-let [reason (:reason problem)]
      (println "    Reason:" reason))))

(defn pinpoint-out
  ([ed] (pinpoint-out ed {}))
  ([ed {:keys [colorize]}]
   (if ed
     (let [{:keys [::s/problems ::s/value] :as ed} (correct-paths ed)
           nproblems (count problems)]
       (if-let [traces (try (trace/traces ed)
                            (catch #?(:clj Throwable :cljs js/Error) _))]
         (binding [*colorize-fn* (choose-colorize-fn colorize)]
           (newline)
           (print-headline nproblems)
           (hline)
           (doseq [t (map vector (range) problems traces)]
             (print-error nproblems value t)
             (newline)
             (hline)))
         (do (println "\n(Failed to analyze the spec errors, and will fall back to s/explain-printer)\n")
             (s/explain-printer ed))))
     (println "Success!!"))))

(defn pinpoint
  ([spec x] (pinpoint spec x {}))
  ([spec x opts]
   (pinpoint-out (s/explain-data spec x) opts)))

#?(:clj
   (defn- find-spec-error [^Throwable t]
     (when t
       (let [data (ex-data t)]
         (if (and data (::s/problems data))
           t
           (recur (.getCause t))))))

   :cljs
   (defn- find-spec-error [e]
     (when (some-> e ex-data ::s/problems)
       e)))

(defn ppt []
  (when-let [e (find-spec-error *e)]
    (pinpoint-out (ex-data e))))
