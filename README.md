# Pinpointer
[![Clojars Project](https://img.shields.io/clojars/v/pinpointer.svg)](https://clojars.org/pinpointer)

_Pinpointer_ aims to enhance `clojure.spec/explain` facility and make it easy to grasp which part of data is causing the spec error.

**Notice**: Pinpointer is built on top of `clojure.spec`, which is one of the most actively developed new features of Clojure. So, it's very fragile by nature, and its APIs are also highly subject to change.

## Installation

Add the following to your `:dependencies`:

[![Clojars Project](https://clojars.org/pinpointer/latest-version.svg)](http://clojars.org/pinpointer)

## Why and how to use it

`clojure.spec(.alpha)` provides an API named `explain`, which (as its name suggests) explains which part of the code causes a spec error:

```clj
=> (s/def ::x integer?)
:user/x
=> (s/def ::y string?)
:user/y
=> (s/explain (s/keys :req-un [::x ::y]) {:y 1})
In: [:y] val: 1 fails spec: :user/y at: [:y] predicate: string?
val: {:y 1} fails predicate: (contains? % :x)
:clojure.spec.alpha/spec  #object[clojure.spec.alpha$map_spec_impl$reify__695 0x47cb4017 "clojure.spec.alpha$map_spec_impl$reify__695@47cb4017"]
:clojure.spec.alpha/value  {:y 1}
nil
=>
```

As you can see, however, the result of `explain` doesn't look very human-friendlily formatted, and it's likely to take a while to find out where the actual problem is.

_Pinpointer_ provides APIs compatible with `explain`, and they show us the problem part in a easier-to-grasp manner:

```clj
=> (require '[pinpointer.core :as p])
nil
=> (p/pinpoint (s/keys :req-un [::x ::y]) {:y 1})

  2 spec errors were detected:
 --------------------------------------------------
 (1/2)

     Input: {:y 1}
          : ^^^^^^
  Expected: (fn [%] (contains? % :x))

 --------------------------------------------------
 (2/2)

     Input: {:y 1}
          :     ^
  Expected: string?

 --------------------------------------------------
nil
=>
```

You can also highlight the part by adding the option `{:colorize :ansi}`:

<img src="doc/images/colorized-pinpoint-result.png" width="630">

## License

Copyright © 2016 Shogo Ohta

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
