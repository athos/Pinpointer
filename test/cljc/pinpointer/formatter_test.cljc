(ns pinpointer.formatter-test
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
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
      #?(:clj "(\\a \\b !!!\\c!!! \\a \\b)\n"
         :cljs "(\"a\" \"b\" !!!\"c\"!!! \"a\" \"b\")\n")]]

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

    (s/coll-of (s/spec integer?))
    #{1 :foo 3}
    [["!!!#{1 3 :foo}!!!\n"
      "(1 3 !!!:foo!!!)\n"]]

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

    (s/cat :i integer? :s string?)
    42
    [["!!!42!!!\n"]]

    (s/cat :i integer? :s string?)
    [1]
    [["[1 !!!...!!!]\n"]]

    (s/cat :i integer? :s string?)
    [1 "foo" 3 4]
    [["[1 \"foo\" !!!3!!! !!!4!!!]\n"]]

    (s/cat :i integer? :s string?)
    [1 :foo]
    [["[1 !!!:foo!!!]\n"]]

    (s/alt :i integer? :s string?)
    42
    [["!!!42!!!\n"]]

    (s/alt :i integer? :s string?)
    []
    [["[!!!...!!!]\n"]]

    (s/alt :i integer? :s string?)
    [:foo]
    [["[!!!:foo!!!]\n"]
     ["[!!!:foo!!!]\n"]]

    (s/alt :one integer? :two (s/cat :first integer? :second integer?))
    [1 2 3 4]
    [["[1 2 !!!3!!! !!!4!!!]\n"]]

    (s/alt :two (s/cat :first integer? :second integer?)
           :three (s/cat :first integer? :second integer? :third integer?))
    [1]
    [["[1 !!!...!!!]\n"]]

    (s/? integer?)
    42
    [["!!!42!!!\n"]]

    (s/? integer?)
    [:foo]
    [["[!!!:foo!!!]\n"]]

    (s/? integer?)
    [1 2 3]
    [["[1 !!!2!!! !!!3!!!]\n"]]

    (s/? (s/cat :int integer? :str string?))
    [1 "foo" 'bar]
    [["[1 \"foo\" !!!bar!!!]\n"]]

    (s/* integer?)
    42
    [["!!!42!!!\n"]]

    (s/* (s/cat :i integer? :s string?))
    [1 "foo" 2]
    [["[1 \"foo\" 2 !!!...!!!]\n"]]

    (s/* (s/cat :i integer? :s string?))
    [1 "foo" 2 :bar]
    [["[1 \"foo\" 2 !!!:bar!!!]\n"]]

    (s/+ integer?)
    42
    [["!!!42!!!\n"]]

    (s/+ integer?)
    []
    [["[!!!...!!!]\n"]]

    (s/+ integer?)
    [:foo]
    [["[!!!:foo!!!]\n"]]

    (s/+ (s/cat :i integer? :s string?))
    [1 "foo" 2]
    [["[1 \"foo\" 2 !!!...!!!]\n"]]

    (s/+ (s/cat :i integer? :s string?))
    [1 "foo" 2 :bar]
    [["[1 \"foo\" 2 !!!:bar!!!]\n"]]

    (s/coll-of (s/int-in 0 5))
    [0 :a 2]
    [["[0 !!!:a!!! 2]\n"]]

    (s/coll-of (s/int-in 0 5))
    [0 5 2]
    [["[0 !!!5!!! 2]\n"]]

    (s/coll-of (s/double-in :min 0 :max 5))
    [0.5 :a 4.5]
    [["[0.5 !!!:a!!! 4.5]\n"]]

    (s/coll-of (s/double-in :min 0 :max 5))
    [0.5 10.5 4.5]
    [["[0.5 !!!10.5!!! 4.5]\n"]]

    (s/coll-of (s/inst-in #inst "2016-01-01" #inst "2017-01-01"))
    [#inst "2016-01-01" nil]
    [["[#inst \"2016-01-01T00:00:00.000-00:00\" !!!nil!!!]\n"]]

    (s/coll-of (s/inst-in #inst "2016-01-01" #inst "2017-01-01"))
    [#inst "2018-01-01"]
    [["[!!!#inst \"2018-01-01T00:00:00.000-00:00\"!!!]\n"]]

    (s/fspec :args (s/cat :x int?) :ret keyword?)
    identity
    #?(:clj [["!!!#function[core/identity]!!!\n"]]
       :cljs [["!!!#function[cljs/core/identity]!!!\n"]])

    (s/fspec :args (s/cat :x int?) :ret string?)
    name
    #?(:clj [["!!!#function[core/name]!!!\n"]]
       :cljs [["!!!#function[cljs/core/name]!!!\n"]])

    ::shape
    {:type :rectangle}
    [["!!!{:type :rectangle}!!!\n"]]

    ::shape
    {:type :circle :radius "100"}
    [["{:type :circle, :radius !!!\"100\"!!!}\n"]]

    (s/coll-of (s/with-gen (s/spec integer?) #(gen/return 1)))
    [0 :foo 2]
    [["[0 !!!:foo!!! 2]\n"]]))
