(ns pinpointer.trace-test
  (:require [clojure.test :refer [deftest are]]
            [clojure.spec.alpha :as s]
            [pinpointer.trace :as trace]))

(defmulti shape-spec :type)
(s/def ::shape (s/multi-spec shape-spec :type))

(deftest trace-test
  (are [spec input expected]
      (= expected (trace/traces (s/explain-data spec input)))
    (s/map-of keyword? (s/coll-of (s/spec integer?)))
    {:a [1 2] :b ["3"]}
    [[[{:spec `(s/map-of keyword? (s/coll-of (s/spec integer?)))
        :val {:a [1 2] :b ["3"]}
        :steps [:b 1]}
       {:spec `(s/coll-of (s/spec integer?)) :val ["3"] :steps [0]}
       {:spec `(s/spec integer?) :val "3" :steps []}
       {:spec `integer? :val "3" :steps []}]]]

    (s/cat :first integer? :second integer?)
    [1]
    [[[{:spec `(s/cat :first integer? :second integer?) :val [1] :steps []}
       {:spec `integer? :val [1] :steps [] :reason "Insufficient input"}]]]

    (s/cat :first integer? :second integer?)
    [1 2 3]
    [[[{:spec `(s/cat :first integer? :second integer?)
        :val [1 2 3]
        :steps [2]
        :reason "Extra input"}]]]

    ::shape
    {:type :circle}
    [[[{:spec `(s/multi-spec shape-spec :type)
        :val {:type :circle}
        :steps []
        :reason "no method"}]]]))
