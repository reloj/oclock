(ns reloj.naive-set-unification
  (:refer-clojure :exclude [==]) ; Explicit exclusion for core.logic
  (:require [clojure.core.logic :refer :all]))


(in-ns 'clojure.core.logic)

;; Adapted from clojure.core.logic líne 946, it might make more sense to reimplement unify-with-map* as below
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
