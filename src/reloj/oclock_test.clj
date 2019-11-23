(ns reloj.oclock-test
  (:refer-clojure :exclude [==]) ; Explicit exclusion for core.logic
  (:require [clojure.core.logic :refer :all]
            [reloj.oclock :refer :all]
            [midje.sweet :refer [facts fact]]))


(facts "about -same-kvs"
  (fact "it returns the set of ovelapping kvs"
    (-same-kvs {:a 1 :b 2 :c 3}
               {:a 2 :b 2 :d 3}) => #{[:b 2]}))

(facts "about -devec-map"
  (fact "it returns the originally vecced map"
    (-devec-map (vec {:a 1 :b 2})) => {:a 1 :b 2}))

(facts "about unify-sets"
  (run 1 [q] (unify-sets #{2 1} #{1 2}))
  => '(_0)
  (run 1 [q] (unify-sets #{q 1} #{1 2}))
  => '(2)
  (run 1 [p q] (unify-sets #{q p 1} #{1 2}))
  => '()
  (run 1 [p q] (unify-sets #{q p 1} #{1 3 2}))
  => '([3 2])
  (run* [p q] (unify-sets #{q p 1} #{1 3 2}))
  => '([3 2] [2 3]))

;(facts "about unground-map"
;  (fact "- creates maps of increasing size"
;    (run 1 [q] (unground-map q))
;    => '({})
;    (run 2 [q] (unground-map q))
;    => '({} {_0 _1})
;    (run 3 [q] (unground-map q))
;    => '({} {_0 _1} {_0 _1 _2 _3})))

(facts "about omerge"
  (fact "when something is an empty dict"
    (run* [p q] (omerge {} p q))
    => '([{} {}])
    (run* [p q] (omerge {:a q} {} {:a 1}))
    => '([_0 1])
    (run* [p q] (omerge {:b q} {} {:a 1}))
    => '()
    (run* [p q] (omerge {p q} {} {:a 1})) ; TODO fails
    => '([:a 1])

    ; TEMPO
    (run* [p q] (== {p q} {1 2})) => '([1 2]) ; fails because of == not looking into keys.
    
    (run* [p q] (omerge {:a q} {:a 1} {}))
    => '([_0 1])
    (run* [p q] (omerge {:b q} {:a 1} {}))
    => '()
    (run* [p q] (omerge {p q} {:a 1} {})) ; TODO fails
    => '([:a 1]))
  (fact "when everything is grounded"
    (run* [q] (omerge q {:a 1} {:b 2}))
    => '({:a 1, :b 2})
    (run* [q] (fresh [a b] (omerge q {a 1} {b 1}))) ; TODO fails: keys should unify when being lvars
    => '({_0 1} {_0 1 _1 1}))
  (fact "when one arg is an lvar"
    (run* [q] (fresh [x] (membero x [1 2 3]) (omerge q {:a x} {:b 2})))
    => '({:a 1, :b 2} {:a 2, :b 2} {:a 3, :b 2})
    (run* [q] (fresh [x] (membero x [1 2 3]) (omerge q {x x} {:b 2})))
    => '({1 1, :b 2} {2 2, :b 2} {3 3, :b 2})
    (run* [q] (omerge {:a 1 :b 2} {:b 2} q))
    => '({:a 1, :b 2} {:a 1, :b nil})
    (run* [q] (omerge {:a 1 :b 2} q {:b 2}))
    => '({:a 1, :b 2} {:a 1, :b nil} {:a 1, :b _0}))
  (fact "when two args are lvar"
    (run* [p q] (omerge {:a 1 :b 2} p q))
    => '([{:a _0, :b _1} {:a nil, :b nil}]
         [{:a _0, :b _1} {:a nil, :b _2}]
         [{:a _0, :b nil} {:a nil, :b _1}]
         [{:a _0, :b _1} {:a _2, :b nil}]
         [{:a _0, :b _1} {:a _2, :b _3}]
         [{:a _0, :b nil} {:a _1, :b _2}]
         [{:a nil, :b _0} {:a _1, :b nil}]
         [{:a nil, :b _0} {:a _1, :b _2}]
         [{:a nil, :b nil} {:a _0, :b _1}])
    (run* [p q] (omerge {} p q))
    => '([{} {}])
    (run 2 [p q] (omerge p {:a 1 :b 2} q))
    => '([{:a 1, :b 2} {}]
         [{:a 1, :b 2, _0 _1} {_0 _1}])
    (run 2 [p q] (omerge p q {:a 1 :b 2}))
    => '([{:a 1, :b 2} {}]
         [{:a 1, :b 2, _0 _1} {_0 _1}]))
  (fact "when nothing is grounded"
    (run 4 [p q r] (omerge p q r))
    => '([{} {} {}]
         [{_0 _1} {_0 _1} {}]
         [{_0 _1} {} {_0 _1}]
         [{_0 _2} {_0 _1} {_0 _2}]))
  (fact "- it helps construct dictionaries"
    (run* [p q]
      (omerge {:a 1 :b 2} p q)
      (omerge p {:a 1} {:a 1})) ; TODO fails
    => '([{:a 1} {:a nil, :b _0}]
         [{:a 1} {:a 1, :b _0}]))
  (fact "i misunderstood how merge and maps work!!"
    (= {:a 1 :b nil} {:a 1}) ; TODO fails
    => true
    (merge {:a 1} {:a nil}) ; TODO fails
    => {:a 1}))

