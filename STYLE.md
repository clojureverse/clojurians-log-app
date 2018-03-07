# Clojurians Log App Style guide

This document describes the stylistic aspects of how we want this codebase to
look. Note that consistency trumps personal preferences, having a code base that
looks as if it was written by a single person reduces cognitive overhead, and
makes it more pleasant to navigate to work in.

## Resources

The [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) should
be your main starting point. Note that if this document differs from the Clojure
Style Guide, than this document takes precedence.

## Whitespace

Functions without docstring should have their name arguments on the same line as the `defn`.

``` clojure
;; good
(defn foo [hello]
  )

;; bad
(defn foo
  [hello]
  )
```

Functions with docstring have both the docstring and the argument vector on a new line, indented two spaces

``` clojure
;; good
(defn foo
  "This does interesting stuff."
  [bar]
  )
```

Put a single empty line between function bodies, and avoid empty lines inside function bodies. Don't put an empty line after the argument vector.

```
;; good
(defn my-fn [hello]
  code)

(defn other-fn
  "doing stuff"
  [xx yy]
  code)

;; bad
(defn my-fn [hello]

  code)


(defn other-fn
  "doing stuff"
  [xx yy]

  code)
```

## Namespaces

As per the Clojure Style Guide, prefer `:as` over `:refer [,,,]`. Avoid `:refer :all` with these exceptions:

``` clojure
(ns foo
  (:refer [clojure.test :refer :all]
          [ns.under.test :refer :all]))
```

When using `:refer [,,,]`, use a vector to list the referred symbols, not a list.

``` clojure
;; good
(ns foo
  (:require [foo.bar :refer [aaa bbb]]))

;; bad
(ns foo
  (:require [foo.bar :refer (aaa bbb)]))
```
