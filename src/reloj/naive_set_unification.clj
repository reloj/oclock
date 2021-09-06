(ns reloj.naive-set-unification
  (:refer-clojure :exclude [==]) ; Explicit exclusion for core.logic
  (:require [clojure.core.logic :refer :all]
            [clojure.set]
            [clojure.math.combinatorics]))


(in-ns 'clojure.core.logic)

;; Adapted from clojure.core.logic líne 946, it might make more sense to reimplement unify-with-map* as below

(count #{1 2})

(some #{31} (seq #{1 2 3}))
(remove #{1} (seq #{1 2 3}))


(defn unify-with-set [u v s]
  (when (and (set? v)
             (counted? u) (counted? v)
             (clojure.core/== (count u) (count v)))
    (let [int (clojure.set/intersection u v)
          j-u (clojure.set/difference u int)
          j-v (clojure.set/difference v int)
          ops (clojure.math.combinatorics/permutations j-v)] ; I did say naïve :)
      (loop [u (seq u) v (seq v) s s]
        (if-not (nil? u)
          (if-not (nil? v)
            (if-let [s (unify s (first u) (first v))] ; conceptual issue. At this unify level we do not deal with options...
              (recur (next u) (next v) s)
              nil)
            nil)
          (if-not (nil? v) nil s))))))


(println "reloj.oclock: Redifining clojure.core.logic/unify-with-map* to also unify keys...")
(defn unify-with-map* [u v s]
  (println "Defining a map!")
  (when (clojure.core/== (count u) (count v))
    (loop [ks (keys u) s s found '() unfound '()]
      (if (seq ks)
        (let [kf (first ks)
              vf (get v kf ::not-found)]
          (if (identical? vf ::not-found)
            (do (println kf " not found")(recur (next ks) s found (cons kf unfound)) )
            (if-let [s (unify s (get u kf) vf)]
              (do (println kf " found")(recur (next ks) s (cons kf found) unfound) )
              nil)))
        (if (empty? unfound)
          s
          (unify s
                 (set (select-keys u unfound)) ; TODO does not work because unify
                 (set (apply dissoc v found)))))))) ; does not understand sets.

;; Does not work because sets dont unify:
;; (run* [q] (== #{q} #{1}))
;; (unify empty-s (set {:a 1})(set {(lvar) 1}))

;; I NEED TO DO A unify-with-set
;; then I can reuse for unify-with-map

;; Question: does {:a 1} unify with {:a 1 :b 2}
;; answer: no. if you want you need to merge it
