(ns pinpointer.core
  (:require #?@(:clj ([clansi]))
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [fipp.clojure :as fipp]
            [pinpointer.printer :as printer]
            [pinpointer.trace :as trace]))

(defn default-colorize-fn [color s]
  #?(:clj (clansi/style s color)
     :cljs s))

(def ^:dynamic *colorize-fn* default-colorize-fn)

(defn- colorize [color s]
  (*colorize-fn* color s))

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

(defn- hline []
  (->> "\n --------------------------------------------------\n"
       (colorize :cyan)
       println))

(defn pinpoint-out
  ([ed] (pinpoint-out ed {}))
  ([{:keys [::s/problems ::s/spec ::s/value] :as ed} {:keys [colorize]}]
   (if ed
     (do (println "Some spec errors were detected:")
         (hline)
         (doseq [problem problems
                 :let [trace (trace/trace problem spec value)
                       [line & lines] (format-data value trace)]]
           (println "     Input:" line)
           (doseq [line lines]
             (println "          :" line))
           (let [[line & lines] (-> (with-out-str
                                      (fipp/pprint (:pred problem)))
                                    (str/split #"\n"))]
             (println "  Expected:" line)
             (doseq [line lines]
               (println "           " line)))
           (when-let [reason (:reason problem)]
             (println "    Reason:" reason))
           (hline)))
     (println "Success!"))))

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
