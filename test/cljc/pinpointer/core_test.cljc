(ns pinpointer.core-test
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer [deftest are]]
            [pinpointer.core :as p]))

(s/def ::x integer?)
(s/def ::y string?)

(deftest pinpoint-out-test
  (are [spec input result]
      (= result
         (with-out-str
           (p/pinpoint-out (s/explain-data spec input))))
    (s/tuple integer? string?)
    [1 :foo]
    "Detected 1 spec error:
----------------------------------------------------------------------
(1/1)

    Cause: [1 :foo]
              ^^^^ 
 Expected: string?

----------------------------------------------------------------------
"

    (s/cat :first integer? :second integer?)
    [1 2 3 4]
    "Detected 1 spec error:
----------------------------------------------------------------------
(1/1)

    Cause: [1 2 3 4]
                ^ ^ 
 Expected: (s/cat :first integer? :second integer?)
 Failure 
   Reason: Extra input

----------------------------------------------------------------------
"

    (s/keys :req-un [::x ::y])
    {:y 42}
    "Detected 2 spec errors:
----------------------------------------------------------------------
(1/2)

    Cause: {:y 42}
           ^^^^^^^
 Expected: (fn [%] (contains? % :x))

----------------------------------------------------------------------
(2/2)

    Cause: {:y 42}
               ^^ 
 Expected: string?

----------------------------------------------------------------------
"

    (s/and string? (s/conformer seq) (s/* #{\a \b}))
    "abcab"
    #?(:clj
       "Detected 1 spec error:
----------------------------------------------------------------------
(1/1)

    Cause: (\\a \\b \\c \\a \\b)
                  ^^       
 Expected: #{\\a \\b}

   --- This comes originally from ---

 Original: \"abcab\"
           ^^^^^^^
     Spec
  Applied: (s/and string? (s/conformer seq) (s/* #{\\a \\b}))

----------------------------------------------------------------------
"
       :cljs
       "Detected 1 spec error:
----------------------------------------------------------------------
(1/1)

    Cause: (\"a\" \"b\" \"c\" \"a\" \"b\")
                    ^^^         
 Expected: #{\"a\" \"b\"}

   --- This comes originally from ---

 Original: \"abcab\"
           ^^^^^^^
     Spec
  Applied: (s/and string? (s/conformer seq) (s/* #{\"a\" \"b\"}))

----------------------------------------------------------------------
")

       ))
