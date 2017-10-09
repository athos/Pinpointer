(ns pinpointer.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            pinpointer.core-test
            pinpointer.formatter-test
            pinpointer.trace-test))

(doo-tests 'pinpointer.trace-test
           'pinpointer.formatter-test
           'pinpointer.core-test)
