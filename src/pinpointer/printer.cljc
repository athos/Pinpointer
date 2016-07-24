(ns pinpointer.printer
  (:refer-clojure :exclude [pr prn pr-str])
  (:require [clojure.core :as c]))

(declare pr)

(defn- print-sequential [begin print-one sep end sequence]
  (print begin)
  (loop [[x & xs :as xxs] (seq sequence)]
    (when xxs
      (print-one x)
      (when xs
        (print sep)
        (recur xs))))
  (print end))

(defn print-seq [s]
  (print-sequential \( pr \space \) s))

(defn- print-vector [v]
  (print-sequential \[ pr \space \] v))

(defn- print-prefix-map [prefix m print-one]
  (print-sequential
    (str prefix "{")
    (fn [e]
      (print-one (key e))
      (print \space)
      (print-one (val e)))
    ", "
    "}"
    (seq m)))

(defn- print-simple-map [m print-one]
  (print-prefix-map nil m print-one))

(defn- print-map [m]
  (let [[ns lift-map] (#'c/lift-ns m)]
    (if ns
      (print-prefix-map (str "#:" ns) lift-map pr)
      (print-simple-map m pr))))

(defn- print-set [s]
  (print-sequential "#{" pr \} (seq s)))

(defn- print-record [r]
  (print \#)
  (print (.getName (class r)))
  (print-simple-map r pr))

(defn pr [x]
  (cond (record? x) (print-record x)
        (seq? x) (print-seq x)
        (vector? x) (print-vector x)
        (map? x) (print-map x)
        (set? x) (print-set x)
        :else (c/pr x)))

(defn prn [x]
  (pr x)
  (newline))

(defn pr-str [x]
  (with-out-str
    (pr x)))
