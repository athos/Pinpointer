(ns pinpointer.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            pinpointer.trace-test))

(doo-tests 'pinpointer.trace-test)
