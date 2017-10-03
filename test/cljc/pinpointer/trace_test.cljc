(ns pinpointer.trace-test
  (:require [clojure.test :refer [deftest are]]
            [clojure.spec.alpha :as s]
            [pinpointer.trace :as trace]))

(defmulti shape-spec :type)
(s/def ::shape (s/multi-spec shape-spec :type))

(s/def ::id integer?)

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
        :reason "no method"}]]]

    (s/and string?
           (s/conformer seq)
           (s/coll-of #{\a \b \c}))
    "abcdab"
    [[[{:spec `(s/and string?
                      (s/conformer seq)
                      (s/coll-of #{\a \b \c}))
        :val "abcdab"
        ;; TODO: the following field value looks somewhat weird,
        ;; so we may need to reconsider the result spec in the future
        :steps [3]}]
      [{:spec `(s/and string?
                      (s/conformer seq)
                      (s/coll-of #{\a \b \c}))
        :val '(\a \b \c \d \a \b)
        :steps []}
       {:spec `(s/coll-of #{\a \b \c})
        :val '(\a \b \c \d \a \b)
        :steps [3]}
       {:spec #{\a \b \c} :val \d :steps []}]]]

    ;; eval is necessary to test the following case, but I don't
    ;; know how we can prepare it for CLJS at the moment.
    #?@(:clj
        ((s/and (s/map-of ::id (s/keys :req-un [::id]))
                (s/coll-of (s/spec (fn [[id m]] (= id (:id m))))))
         {1 {:id 1} 2 {:id 3}}
         [[[{:spec `(s/and (s/map-of ::id (s/keys :req-un [::id]))
                           (s/coll-of
                            (s/spec (fn [[~'id ~'m]] (= ~'id (:id ~'m))))))
             :val {1 {:id 1} 2 {:id 3}}
             :steps []}
            {:spec `(s/coll-of (s/spec (fn [[~'id ~'m]] (= ~'id (:id ~'m)))))
             :val {1 {:id 1} 2 {:id 3}}
             :steps [1]}]
           [{:spec `(s/coll-of (s/spec (fn [[~'id ~'m]] (= ~'id (:id ~'m)))))
             :val [[1 {:id 1}] [2 {:id 3}]]
             :steps [1]}
            {:spec `(s/spec (fn [[~'id ~'m]] (= ~'id (:id ~'m))))
             :val [2 {:id 3}]
             :steps []}
            {:spec `(fn [[~'id ~'m]] (= ~'id (:id ~'m)))
             :val [2 {:id 3}]
             :steps []}]]]))

    ))
