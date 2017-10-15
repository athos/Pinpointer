# Pinpointer
[![Clojars Project](https://img.shields.io/clojars/v/pinpointer.svg)](https://clojars.org/pinpointer)
[![CircleCI](https://circleci.com/gh/athos/Pinpointer.svg?style=shield)](https://circleci.com/gh/athos/Pinpointer)
[![codecov](https://codecov.io/gh/athos/Pinpointer/branch/master/graph/badge.svg)](https://codecov.io/gh/athos/Pinpointer)
[![join the chat at https://gitter.im/athos/pinpointer](https://badges.gitter.im/athos/pinpointer.svg)](https://gitter.im/athos/pinpointer?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Pinpointer is yet another clojure.spec error reporter with sophisticated error analysis.

It has the following features:

- Visually pinpoints what portion of the input data is causing the spec error, based on the spec error analysis of [`spectrace`](https://github.com/athos/spectrace), a fine-grained spec error analyzer
- Formats and colorizes the error reports in an easy-to-grasp manner
- Tracks 'value changes', i.e. reports the spec errors correctly even when `s/conformer` in the spec transforms the input data
- Extensible to user-defined spec macros (not documented yet)

**Notice**: Pinpointer is built on top of `clojure.spec`, which is one of the most actively developed new features of Clojure. So, it's still in alpha and its APIs are also subject to change.

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

### pinpoint: replacement of s/explain

_Pinpointer_ provides APIs compatible with `explain` and displays the problematic parts in an easier-to-grasp manner:

```clj
=> (require '[pinpointer.core :as p])
nil
=> (p/pinpoint (s/keys :req-un [::x ::y]) {:y 1})
Detected 2 spec errors:
----------------------------------------------------------------------
(1/2)

    Input: {:y 1}
           ^^^^^^
 Expected: (fn [%] (contains? % :x))

----------------------------------------------------------------------
(2/2)

    Input: {:y 1}
               ^
 Expected: string?

----------------------------------------------------------------------
nil
=>
```


You can also colorize the report by adding the option `{:colorize :ansi}`:

<img src="doc/images/colorized-pinpoint-result.png" width="630">

### pinpoint-out: plugin implementation for s/\*explain-out\*

If you'd rather like to completely replace the `explain` facility for any kinds of spec error reporting, it would be helpful to replace `s/*explain-out*` with `pinpointer.core/pinpoint-out` instead:

```clj
=> (set! s/*explain-out* p/pinpoint-out)
#function[pinpointer.core/pinpoint-out]
=>
;; from now on, p/pinpoint-out will be used in place of s/explain-printer
=>
=> (defn f [x] (inc x))
#'user/f
=> (s/fdef f
     :args (s/cat :x (s/and integer? even?))
     :ret (s/and integer? odd?))
user/f
=> (require '[clojure.spec.test.alpha :as t])
nil
=> (t/instrument)
[user/f]
=> (f 3)
ExceptionInfo Call to #'user/f did not conform to spec:
Detected 1 spec error:
----------------------------------------------------------------------
(1/1)

    Input: (3)
            ^
 Expected: even?

----------------------------------------------------------------------
  clojure.core/ex-info (core.clj:4725)
=>
```

## Known Issues

There are a couple of known issues in Pinpointer, primarily due to `clojure.spec`'s bugs. They can be found on [Issues page](https://github.com/athos/Pinpointer/issues?q=is%3Aissue+is%3Aopen+label%3A%22spec+bug%22), being tagged with `spec bug`.

If you found something wrong when using Pinpointer and you want to report an issue, check whether or not it's already been filed there first.

## License

Copyright © 2016 - 2017 Shogo Ohta ([@athos0220](https://twitter.com/athos0220))

Distributed under the Eclipse Public License 1.0.
