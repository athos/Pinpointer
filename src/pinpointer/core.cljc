(ns pinpointer.core
  (:require #?@(:clj ([clansi]))
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [fipp.clojure :as fipp]
            [pinpointer.printer :as printer]
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

(defn- wavy-line [start length]
  (->> (concat (repeat start \space)
               (repeat length \^))
       (apply str)
       (colorize :red)))

(defn- format-data [value trace]
  (let [lines (str/split (printer/pprint-str value trace) #"\n")]
    (loop [[line & more] lines, highlighting? false, ret []]
      (if-not line
        ret
        (if highlighting?
          (let [indent (count (re-find #"^\s*" line))
                [highlight post] (str/split line #"\001")
                wavy (wavy-line indent (count highlight))
                highlight (colorize :red highlight)]
            (recur more false (conj ret (str highlight post) wavy)))
          (let [[pre highlight] (str/split line #"\000")]
            (if highlight
              (let [[highlight' post] (str/split highlight #"\001")
                    wavy (wavy-line (count pre) (count highlight'))
                    highlight' (colorize :red highlight')]
                (recur more (not post)
                       (conj ret (str pre highlight' post) wavy)))
              (recur more highlighting? (conj ret line)))))))))

(defn- print-headline [nproblems]
  (if (= nproblems 1)
    (println " 1 spec error was detected:")
    (println " " nproblems "spec errors were detected:")))

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

(defn pinpoint-out
  ([ed] (pinpoint-out ed {}))
  ([ed {:keys [colorize]}]
   (if ed
     (let [{:keys [::s/problems ::s/value] :as ed} (correct-paths ed)
           nproblems (count problems)]
       (binding [*colorize-fn* (choose-colorize-fn colorize)]
         (newline)
         (print-headline nproblems)
         (hline)
         (doseq [[i problem trace] (map vector
                                        (range)
                                        problems
                                        (trace/traces ed))
                 :let [[line & lines] (format-data value trace)]]
           (printf " (%d/%d)\n\n" (inc i) nproblems)
           (println "     Input:" line)
           (doseq [line lines]
             (println "          :" line))
           (let [[line & lines] (as-> (:pred problem) it
                                  (simplify-spec it)
                                  (with-out-str (fipp/pprint it))
                                  (str/split it #"\n"))]
             (println "  Expected:" line)
             (doseq [line lines]
               (println "           " line)))
           (when-let [reason (:reason problem)]
             (println "    Reason:" reason))
           (newline)
           (hline))))
     (println "Success!!"))))

(defn pinpoint
  ([spec x] (pinpoint spec x {}))
  ([spec x opts]
   (pinpoint-out (s/explain-data spec x) opts)))

(defn ppt []
  (letfn [(find-spec-error [^Throwable t]
            (when t
              (let [data (ex-data t)]
                (if (and data (::s/problems data))
                  t
                  (recur (.getCause t))))))]
    (when-let [e (find-spec-error *e)]
      (pinpoint-out (ex-data e)))))
