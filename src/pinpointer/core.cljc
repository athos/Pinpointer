(ns pinpointer.core
  (:require #?(:clj [clansi])
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as t]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [fipp.clojure :as fipp]
            [pinpointer.formatter :as formatter]
            [pinpointer.trace :as trace]
            [spectrace.core :as strace]))

(def ^:dynamic ^:private *colorize-fn*)

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

(defn- pad-left [s n]
  (str (space (- n (count s))) s))

(defn- wavy-line [n]
  (colorize :red (times n \^)))

(defn- format-line [line hiliting?]
  (let [[_ indent line'] (re-matches #"(\s*)(.*)" line)
        parts (str/split line' #"\u0000")]
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

(defn- format-data [value trace opts]
  (let [lines (binding [formatter/*highlighting-mark* "\u0000"]
                (str/split (formatter/format value trace opts) #"\n"))]
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
    (println "Detected 1 spec error:")
    (println "Detected" nproblems "spec errors:")))

(defn- hline [width]
  (->> (str (times width \-))
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
                       #?(:clj "clojure.core" :cljs "cljs.core")
                       (symbol (name x))

                       #?(:clj "clojure.spec.alpha" :cljs "cljs.spec.alpha")
                       (symbol "s" (name x))

                       x)
                     x))
                 spec))

(defn- print-error [total value [i problem trace] opts]
  (letfn [(print-with-caption [caption s]
            (println (str (pad-left caption 9) \:) s))
          (print-data [caption chunk]
            (let [val (:val (first chunk))
                  [line & lines] (format-data val chunk opts)]
              (print-with-caption caption line)
              (doseq [line lines]
                (println "          " line))))
          (print-spec [caption spec]
            (let [[line & lines] (as-> spec it
                                   (simplify-spec it)
                                   (with-out-str (fipp/pprint it opts))
                                   (str/split it #"\n"))]
              (print-with-caption caption line)
              (doseq [line lines]
                (println "          " line))))]
   (let [[chunk & more] (rseq trace)]
     (println (str "(" (inc i) "/" total ")\n"))
     (print-data "Input" chunk)
     (print-spec "Expected" (:pred problem))
     (when-let [reason (:reason problem)]
       (println (pad-left "Failure " 9))
       (print-with-caption "Reason" reason))
     (when (seq more)
       (doseq [chunk more]
         (->> "\n   --- This comes originally from ---\n"
              (colorize :cyan)
              println)
         (print-data "Original" chunk)
         (println (pad-left "Spec" 9))
         (print-spec "Applied" (:spec (peek chunk))))))))

(defonce ^:private last-explain-data (atom nil))

(defn pinpoint-out
  "A plugin implementation of s/*explain-out*.

  Takes the same options as pinpoint."
  ([ed] (pinpoint-out ed {}))
  ([ed {:keys [width colorize fallback-on-error eval] :as opts
        :or {width 70, fallback-on-error true}}]
   (if ed
     (let [{:keys [::s/problems ::s/value] :as ed'} (correct-paths ed)
           nproblems (count problems)
           traces (try
                    (binding [strace/*eval-fn* (or eval strace/*eval-fn*)]
                      (trace/traces ed'))
                    (catch #?(:clj Throwable :cljs :default) e e))]
       (cond (vector? traces)
             (let [opts (-> opts
                            (assoc :width (max (- width 11) 0))
                            (dissoc :colorize :fallback-on-error))]
               (reset! last-explain-data ed)
               (binding [*colorize-fn* (choose-colorize-fn colorize)]
                 (print-headline nproblems)
                 (hline width)
                 (doseq [t (map vector (range) problems traces)]
                   (print-error nproblems value t opts)
                   (newline)
                   (hline width))
                 (when-let [caller (::t/caller ed')]
                   (println (str "  " (:var-scope caller)
                                 " (" (:file caller)
                                 ":" (:line caller) ")")))))

             fallback-on-error
             (do (println "[PINPOINTER] Failed to analyze the spec errors, and will fall back to s/explain-printer\n")
                 (s/explain-printer ed))

             :else (throw traces)))
     (println "Success!!"))))

(defn pinpoint
  "Given a spec and a value that fails to conform, reports the spec
error(s) in a human-friendly manner.

The opts map may have the following keys:
  :width - Number of columns to try to wrap the report at.
    Defaults to 70.
  :colorize - Can either be a keyword :ansi, :none or a fn that
    takes a color keyword and a string, and returns the colorized
    string. Defaults to :none.
  :fallback-on-error - If set to true, falls back to
    s/explain-printer in case of an error during the analysis.
    Otherwise, rethrows the error. Defaults to true.
  :eval - eval fn to be used to analyze spec errors. Defaults to
    clojure.core/eval in Clojure, nil in ClojureScript."
  ([spec x] (pinpoint spec x {}))
  ([spec x opts]
   (pinpoint-out (s/explain-data spec x) opts)))

(defn replay
  ([] (replay {}))
  ([opts]
   (pinpoint-out @last-explain-data opts)))

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

(defn ppt
  ([] (ppt {}))
  ([opts]
   (when-let [e (find-spec-error *e)]
     (pinpoint-out (ex-data e) opts))))
