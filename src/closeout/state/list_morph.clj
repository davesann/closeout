(ns 
  ^{:doc "Calculated changes between lists"}
  closeout.state.list-morph
  (:require 
    [dsann.utils.x.core :as u]
    
    [dsann.utils.map :as umap]
    [dsann.utils.seq :as useq]
    [clojure.set :as cset]
    )
  )

;; calculate difference between two lists

; very simple impl
; dtw might be better. faster?
; but it does ok even for large lists (say 100000) ~ 0.5s on my machine
; may be slower for complex values 
(comment
  (require '[dsann.utils.state.list-morph :as lm] :reload)
  
  (doseq [i (range 20)]
    (let [l1 (range 100000)
          l2 (for [i (range 120000)] (rand-int 50000))]
      (time (def x (doall (lm/list-morph l1 l2))))))
  )


(defn index-map 
  "takes a seq (v1 v2 v3 v4...) and produces a map
    {i1 [idx1 idx2 idx3]
     i2 [...]
     ....}
    note that the same v can repeat in the sequence
   "
  ([a-seq] (index-map a-seq 0 {}))
  ([a-seq idx result]
    (if-not (seq a-seq)
      (umap/mapvals reverse result)
      (let [[i & r] a-seq
            new-result (update-in 
                         result [i] 
                         #(if (nil? %) (list idx) (cons idx %)))]
        (recur r (inc idx) new-result)))))
  

(defn find-list-morph-
  [coll idx imap result]
  (if (not (seq coll))
    {:new-list-changes result
     :old-list-deletes (mapcat second imap)}
    (let [[item & r] coll]
      (if-let [indexes (get imap item)]
        (let [[i & rindexes] indexes
              result (assoc result idx [(if (= i idx) :unchanged :move) i])
              imap   (assoc imap item rindexes)
              ]
          (recur r (inc idx) imap result))
        (let [result (assoc result idx [:insert item])]
          (recur r (inc idx) imap result))))))

(defn find-list-morph 
  "calculate the changes to get from seq1 to seq2
   e.g.
    (list-morph [:a :b :c :d] [:b :f :a]) 
    = > {:new-list-changes 
            {2 [:move 0], 
             1 [:insert :f], 
             0 [:move 1]}, 
         :old-list-deletes (3 2)}
     so 
       index 0 gets old index 1 - move
       index 1 gets new :f
       index 2 gets old index 0
       indices 3 and 2 are removed
     "
  ([seq1 seq2] 
    (let [imap (index-map seq1)]
      (find-list-morph- seq2 0 imap {}))))

