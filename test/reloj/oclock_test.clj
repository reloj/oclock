(ns reloj.oclock-test
  (:refer-clojure :exclude [==]) ; Explicit exclusion for core.logic
  (:require [clojure.core.logic :refer :all :exclude [is]]
            [reloj.oclock :refer :all]
            [clojure.test :refer :all]))


(deftest test--same-kvs
  (testing "about -same-kvs"
    (testing "it returns the set of ovelapping kvs"
      (is (= (-same-kvs {:a 1 :b 2 :c 3}
                        {:a 2 :b 2 :d 3})
             #{[:b 2]})))))

(deftest test--devec-map
  (testing "about -devec-map"
    (testing "it returns the originally vecced map"
      (is (= (-devec-map (vec {:a 1 :b 2}))
             {:a 1 :b 2})))))

(deftest test-unify-sets
  (testing "about unify-sets"
    (is (= (run 1 [q] (unify-sets #{2 1} #{1 2}))
           '(_0)))
    (is (= (run 1 [q] (unify-sets #{q 1} #{1 2}))
           '(2)))
    (is (= (run 1 [p q] (unify-sets #{q p 1} #{1 2}))
           '()))
    (is (= (run 1 [p q] (unify-sets #{q p 1} #{1 3 2}))
           '([3 2])))
    (is (= (run* [p q] (unify-sets #{q p 1} #{1 3 2}))
           '([3 2] [2 3])))))

(comment
(deftest test-unground-map
  (testing "about unground-map"
    (testing "- creates maps of increasing size"
      (is (= (run 1 [q] (unground-map q))
             '({})))
      (is (= (run 2 [q] (unground-map q))
             '({} {_0 _1})))
      (is (= (run 3 [q] (unground-map q))
             '({} {_0 _1} {_0 _1 _2 _3}))))))
)

(deftest test-omerge
  (testing "about omerge"
    (testing "when something is an empty dict"
      (is (= (run* [p q] (omerge {} p q))
             '([{} {}])))
      (is (= (run* [p q] (omerge {:a q} {} {:a 1}))
             '([_0 1])))
      (is (= (run* [p q] (omerge {:b q} {} {:a 1}))
             '()))
      (is (= (run* [p q] (omerge {p q} {} {:a 1})) ; TODO fails
             '([:a 1])))

      ;; TEMPO
      (is (= (run* [p q] (== {p q} {1 2}))
             '([1 2]))) ; fails because of == not looking into keys.
      
      (is (= (run* [p q] (omerge {:a q} {:a 1} {}))
             '([_0 1])))
      (is (= (run* [p q] (omerge {:b q} {:a 1} {}))
             '()))
      (is (= (run* [p q] (omerge {p q} {:a 1} {})) ; TODO fails
             '([:a 1]))))
    (testing "when everything is grounded"
      (is (= (run* [q] (omerge q {:a 1} {:b 2}))
             '({:a 1, :b 2})))
      (is (= (run* [q] (fresh [a b] (omerge q {a 1} {b 1}))) ; TODO fails: keys should unify when being lvars
             '({_0 1} {_0 1 _1 1}))))
    (testing "when one arg is an lvar"
      (is (= (run* [q] (fresh [x] (membero x [1 2 3]) (omerge q {:a x} {:b 2})))
             '({:a 1, :b 2} {:a 2, :b 2} {:a 3, :b 2})))
      (is (= (run* [q] (fresh [x] (membero x [1 2 3]) (omerge q {x x} {:b 2})))
             '({1 1, :b 2} {2 2, :b 2} {3 3, :b 2})))
      (is (= (run* [q] (omerge {:a 1 :b 2} {:b 2} q))
             '({:a 1, :b 2} {:a 1, :b nil})))
      (is (= (run* [q] (omerge {:a 1 :b 2} q {:b 2}))
             '({:a 1, :b 2} {:a 1, :b nil} {:a 1, :b _0}))))
    (testing "when two args are lvar"
      (is (= (run* [p q] (omerge {:a 1 :b 2} p q))
             '([{:a _0, :b _1} {:a nil, :b nil}]
               [{:a _0, :b _1} {:a nil, :b _2}]
               [{:a _0, :b nil} {:a nil, :b _1}]
               [{:a _0, :b _1} {:a _2, :b nil}]
               [{:a _0, :b _1} {:a _2, :b _3}]
               [{:a _0, :b nil} {:a _1, :b _2}]
               [{:a nil, :b _0} {:a _1, :b nil}]
               [{:a nil, :b _0} {:a _1, :b _2}]
               [{:a nil, :b nil} {:a _0, :b _1}])))
      (is (= (run* [p q] (omerge {} p q))
             '([{} {}])))
      (is (= (run 2 [p q] (omerge p {:a 1 :b 2} q))
             '([{:a 1, :b 2} {}]
               [{:a 1, :b 2, _0 _1} {_0 _1}])))
      (is (= (run 2 [p q] (omerge p q {:a 1 :b 2}))
             '([{:a 1, :b 2} {}]
               [{:a 1, :b 2, _0 _1} {_0 _1}]))))
    (testing "when nothing is grounded"
      (is (= (run 4 [p q r] (omerge p q r))
             '([{} {} {}]
               [{_0 _1} {_0 _1} {}]
               [{_0 _1} {} {_0 _1}]
               [{_0 _2} {_0 _1} {_0 _2}]))))
    (testing "- it helps construct dictionaries"
      (is (= (run* [p q]
               (omerge {:a 1 :b 2} p q)
               (omerge p {:a 1} {:a 1})) ; TODO fails
             '([{:a 1} {:a nil, :b _0}]
               [{:a 1} {:a 1, :b _0}]))))
    (testing "i misunderstood how merge and maps work!!"
      (is (= (= {:a 1 :b nil} {:a 1}) ; TODO fails
             true))
      (is (= (merge {:a 1} {:a nil}) ; TODO fails
             {:a 1})))))
