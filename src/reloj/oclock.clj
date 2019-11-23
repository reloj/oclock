(ns reloj.oclock
  (:refer-clojure :exclude [==]) ; Explicit exclusion for core.logic
  (:require [clojure.core.logic :refer :all]
            [clojure.math.combinatorics]
            [clojure.set]))


(defn ocoll? [elts]
  "A relational coll? - a collection is composed of things."
  (fresh [x]
    (membero x elts)))

(defn oset? [elts]
  "Pending: How to determine this is closed."
  succeed)

(defn -same-kvs [a b]
  (clojure.set/intersection (set a) (set b)))

(defn -devec-map [vecced-map]
  (apply hash-map (flatten vecced-map)))

(defn -either-list [len]
  "Generates all possible lists of length len with 'either vectors'
   (vectors of 2 booleans where or is true)"
  (let [pos [[true false] [true true] [false true]]]
    (if (= len 1)
      (map vector pos)
      (for [elt (-either-list (dec len))
            new pos]
        (conj elt new)))))

(defn -key-and-lvar-map [pos keyvec]
  "Receives a possibility generated by -either-list, and generates a
   pair of lvared maps with it. (-either-list is used to ensure that keys
   are present in either of the two maps, so the merge produces each target
   key, but until this function they are not proper maps)"
  (apply mapv (comp #(zipmap keyvec %)
                    #(map (fn [x] (when x (lvar))) %)
                    vector)
         pos))

(defn -lvar-map-size-x [x]
  (apply hash-map (repeatedly (* 2 x) lvar)))

(defn unify-sets [a b]
  (let [int (clojure.set/intersection a b)
        ua (clojure.set/difference a int)
        ub (clojure.set/difference b int)]
    (if (= (count ua) (count ub))
       (or* (map #(== (vec ua) %)
                 (clojure.math.combinatorics/permutations ub)))
       fail)))

;(defn unground-map [m]
;  (or* (map #(== m (-lvar-map-size-x %))
;            (range))))


;; Adapted from clojure.core.logic líne 946, it might make more sense to reimplement unify-with-map* as below
(in-ns 'clojure.core.logic)
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

;; No functiona porque los sets no unifican:
;; (run* [q] (== #{q} #{1}))
;; (unify empty-s (set {:a 1})(set {(lvar) 1}))
(in-ns 'reloj.oclock)

(defn kv== [mapa mapb]
  (== (set mapa)
      (set mapb)))

(defn omerge
  "Relates the merged map ab as the union of a and b.
   As in merge, b can override values in a for the same key."
  [ab a b]
  (cond
   ; if any dict is grounded to {} problem is easy
   (= {} ab) (== [a b] [{} {}])
   (= {} a) (== ab b)
   (= {} b) (== ab a)
   ; if a and b are grounded, irrespective of ab's status, the answer is easy
   (every? (complement lvar?) [a b])
   (== ab (merge a b)) ; WRONG! merge will not unify 2 keys that are lvars
   ; if only a is not ground: a ~= ab but its vals of same-kvs can be anything, or nothing (not same as nil)
   (every? (complement lvar?) [ab b])
   (let [overlap (-same-kvs ab b)
         opts (map (fn [[k v1]]
                     (let [v (lvar)]
                       [k v (membero v [v1 nil (lvar)])])) ; Error: nil needs to be no key
                   overlap)]
     (all (everyg #(nth % 2) opts)
          (== a (merge ab (-devec-map (map pop opts))))))
   ; if only b is not ground: b ~= ab but its vals of same-kvs can also be nonexisting (not same as nil)
   (every? (complement lvar?) [ab a])
   (let [overlap (-same-kvs ab a)
         opts (map (fn [[k v1]]
                     (let [v (lvar)]
                       [k v (membero v [v1 nil])])) ; Error: nil needs to be no key
                   overlap)]
     (all (everyg #(nth % 2) opts)
          (== b (merge ab (-devec-map (map pop opts))))))
   ; only ab grounded restricts all keys
   (not (lvar? ab))
   (let [ks (keys ab)]
     (or* (map #(all (let [[x y] (-key-and-lvar-map % ks)]
                       (== [a b] [x y]))
                     (unify-sets (set ab) (set [:????]))) ; THIS WAS HALF-WRITTEN!!
               (-either-list (count ks)))))
   ; only b grounded is a strict subset of ab for both keys and vals
   (not (lvar? b))
   ;(all (unground-map a) ; omerge keeps seing an lvar!
   ;     (omerge ab a b))
   (or* (map #(let [m (-lvar-map-size-x %)]
                (all (== m a)
                     (omerge ab m b)))
             (range)))
   ; only a grounded is a strict subset of ab for keys, but any val can be replaced
   (not (lvar? a))
   ;(all (unground-map b) ; omerge keeps seing an lvar!
   ;     (omerge ab a b))
   (or* (map #(let [m (-lvar-map-size-x %)]
                (all (== m b)
                     (omerge ab a m)))
             (range)))
   ; if none are grounded, i need to generate a divergent sequence of growing maps
   :else
   ;(all (unground-map ab) ; omerge keeps seing an lvar!
   ;     (omerge ab a b))
   (or* (map #(let [m (-lvar-map-size-x %)]
                (all (== m ab)
                     (omerge m a b)))
             (range)))))


  ;
;(run 1 [q] (== q (partial-map {:a 1}))
;     (== q (partial-map {:b 1})))
;Error printing return value at clojure.core.logic.LVar/unify_terms (logic.clj:678).
;clojure.core.logic.PMap@3c6f0c28 is non-storable

;; I found a bug!!

;(run 1 [q] (== (partial-map {:a q})
;
;               (partial-map {:a 1 :b 1})))
;=> (1)
;(run 1 [q] (== (partial-map {:a 1})
;
;               (partial-map {:a q :b 1})))
;Error printing return value (IllegalArgumentException) at clojure.lang.RT/seqFrom (RT.java:553).
;Don't know how to create ISeq from: java.lang.Long

