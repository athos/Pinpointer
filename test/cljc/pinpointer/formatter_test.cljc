(ns pinpointer.formatter-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest is are]]
            [pinpointer.formatter :as formatter]
            [pinpointer.trace :as trace]))

(s/def ::x integer?)
(s/def ::y string?)

(defmulti shape-spec :type)
(s/def ::shape (s/multi-spec shape-spec :type))

(s/def ::radius number?)
(defmethod shape-spec :circle [_]
  (s/keys :req-un [::radius]))

(deftest format-test
  (are [spec input result]
      (= result
         (for [trace (trace/traces (s/explain-data spec input))]
           (map #(formatter/format (:val (first %)) %) trace)))
    (s/spec integer?)
    :foo
    [["!!!:foo!!!\n"]]

    (s/and integer? even?)
    3
    [["!!!3!!!\n"]]

    (s/and string? (s/conformer seq) (s/* #{\a \b}))
    "abcab"
    [["!!!\"abcab\"!!!\n"
      "(\\a \\b !!!\\c!!! \\a \\b)\n"]]

    (s/or :i integer? :s string?)
    :foo
    [["!!!:foo!!!\n"] ["!!!:foo!!!\n"]]

    (s/nilable integer?)
    :foo
    [["!!!:foo!!!\n"] ["!!!:foo!!!\n"]]

    (s/tuple integer? string?)
    42
    [["!!!42!!!\n"]]

    (s/tuple integer? string?)
    [1 :foo]
    [["[1 !!!:foo!!!]\n"]]

    (s/every (s/spec integer?))
    42
    [["!!!42!!!\n"]]

    (s/every (s/spec integer?))
    [1 :foo 'bar]
    [["[1 !!!:foo!!! bar]\n"]
     ["[1 :foo !!!bar!!!]\n"]]

    (s/coll-of (s/spec integer?))
    42
    [["!!!42!!!\n"]]

    (s/coll-of (s/spec integer?))
    [1 :foo 'bar]
    [["[1 !!!:foo!!! bar]\n"]
     ["[1 :foo !!!bar!!!]\n"]]

    #?@(:clj
        ((s/coll-of (s/spec (fn [[id m]] (= id (:id m)))))
         {1 {:id 1} 2 {:id 3}}
         [["!!!{1 {:id 1}, 2 {:id 3}}!!!\n"
           "([1 {:id 1}] !!![2 {:id 3}]!!!)\n"]]
         ))

    (s/every-kv keyword? integer?)
    42
    [["!!!42!!!\n"]]

    (s/every-kv keyword? integer?)
    {:a 1 2 :b}
    [["{:a 1, !!!2!!! :b}\n"]
     ["{:a 1, 2 !!!:b!!!}\n"]]

    (s/map-of keyword? integer?)
    42
    [["!!!42!!!\n"]]

    (s/map-of keyword? integer?)
    {:a 1 2 :b}
    [["{:a 1, !!!2!!! :b}\n"]
     ["{:a 1, 2 !!!:b!!!}\n"]]

    (s/keys :req-un [::x])
    42
    [["!!!42!!!\n"]]

    (s/keys :req-un [::x ::y])
    {:y 42}
    [["!!!{:y 42}!!!\n"]
     ["{:y !!!42!!!}\n"]]

    (s/merge (s/keys :req-un [::x]) (s/keys :req-un [::y]))
    42
    [["!!!42!!!\n"]
     ["!!!42!!!\n"]]

    (s/merge (s/keys :req-un [::x]) (s/keys :req-un [::y]))
    {:y 42}
    [["!!!{:y 42}!!!\n"]
     ["{:y !!!42!!!}\n"]]

    ::shape
    {:type :rectangle}
    [["!!!{:type :rectangle}!!!\n"]]

    ::shape
    {:type :circle :radius "100"}
    [["{:type :circle, :radius !!!\"100\"!!!}\n"]]

    ))
