(ns pinpointer.core
  (:require #?@(:clj ([clansi]))
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [fipp.clojure :as fipp]
            [pinpointer.printer :as printer]
            [pinpointer.trace :as trace]))

(defn wavy-line [start length]
  (apply str (concat (repeat start \space)
                     (repeat length \^))))

(defn default-colorize-fn [s]
  #?(:clj (clansi/style s :yellow)
     :cljs s))

(defn- colorize-by [colorize-fn s]
  (if colorize-fn
    (if (= colorize-fn :ansi)
      (default-colorize-fn s)
      (colorize-fn s))
    s))

(defn- format-data [value trace]
  (let [lines (str/split (printer/pprint-str value trace) #"\n")]
    (loop [[line & more] lines, highlighting? false, ret []]
      (if-not line
        ret
        (if highlighting?
          (let [indent (count (re-find #"^\s*" line))
                [highlight post] (str/split line #"\001")
                wavy (wavy-line indent (count highlight))]
            (recur more false (conj ret (str highlight post) wavy)))
          (let [[pre highlight] (str/split line #"\000")]
            (if highlight
              (let [[highlight' post] (str/split highlight #"\001")
                    wavy (wavy-line (count pre) (count highlight'))]
                (recur more (not post)
                       (conj ret (str pre highlight' post) wavy)))
              (recur more highlighting? (conj ret line)))))))))

(defn- hline []
  (println "\n --------------------------------------------------\n"))

(defn pinpoint-out
  ([ed] (pinpoint-out ed {}))
  ([{:keys [::s/problems ::s/spec ::s/value] :as ed} {:keys [colorize]}]
   (hline)
   (doseq [[i problem] (map-indexed vector problems)
           :let [trace (trace/trace problem spec value)
                 [line & lines] (format-data value trace)]]
     (when (not= i 0)
       (hline))
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
       (println "    Reason:" reason)))
   (hline)))

(defn pinpoint
  ([spec x] (pinpoint spec x {}))
  ([spec x opts]
   (pinpoint-out (s/explain-data spec x)
                 (merge {:root x} opts))))
