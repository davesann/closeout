(ns 
  ^{:doc "Update functions that provide information on where data changed
          within an update. the info is supplied as metatdata on the result."}
  closeout.state.update
  (:require 
    [dsann.utils.x.core :as u]
    
    [dsann.utils.map :as umap]
    [dsann.utils.seq :as useq]
    [clojure.set :as cset]
    )
  )

;; TODO: document and tidy


;; bind *actor* if you want to flag who did the update.
(def ^:dynamic *actor* ::unspecified)

;; internal utils

(defn differs-in? [m path v]
  (not (= (get-in m path) v)))

(defn filter-i
  "filter by pred? - reporting indices removed"
  ([pred? coll] (filter-i pred? coll 0 [] []))
  ([pred? coll idx removed result]
    (if-let [s (seq coll)]
      (let [f (first s) r (rest s)]
        (if (pred? f)
          (recur pred? r (inc idx) removed (cons f  result))
          (recur pred? r (inc idx) (cons idx removed) result)))
      {:result (reverse result)
       :removed-indices (set removed)}
      )))

(defn remove-i [pred? coll]
  "filter by pred? - reporting indices removed"
  (filter-i (complement pred?) coll))

(defn remove-by-index-i [index-set a-seq]
  "remove by index-set - reporting indices removed
     note not returning actual removed - but the full index set"
  {:result (useq/remove-by-index index-set a-seq)
   :removed-indices index-set})

(defn filter-by-index-i [index-set a-seq]
  "remove by index-set - reporting indices removed
     note not returning actual removed - but the full index set"
  {:result (useq/filter-by-index index-set a-seq)
   :removed-indices index-set})


;; swap fns
;; You can see what meta is added here

(defn swap-update-in
  [state ks f args]
  (with-meta
    (apply (partial update-in state ks f) args)
    {:update-path ks
     :action :update
     :update-by *actor*
     }))

(defn swap-assoc-in [state ks v]
  (with-meta
    (assoc-in state ks v)
    {:update-path ks
     :action :set
     :update-by *actor*
     })) 

(defn swap-dissoc-in [state ks]
  (with-meta
    (umap/dissoc-in state ks)
    {:update-path ks
     :action :dissoc
     :update-by *actor*
     }))         

(defn swap-remove-in [state ks remove-fn pred?]
  (let [l (get-in state ks)
        {:keys [result removed-indices]} (remove-fn pred? l)]
    (with-meta
      (assoc-in state ks (vec result))
      {:update-path ks
       :action :list-remove
       :removed-indices removed-indices
       :update-by *actor*})))

(defn swap-append-in [state ks values]
  (let [l (get-in state ks [])]
    (with-meta
      (assoc-in state ks (apply (partial conj l) values))
      {:update-path ks
       :action :list-append  ;; assume that you have access to old value
       :appended values
       :update-by *actor*})))



;; update functions
;;  call these

(defn update-in! [an-atom ks f & args]
  "Update-in atom adding meta 
    {:update-path ks
     :action update
     :update-by *actor*}"
    (swap! an-atom swap-update-in ks f args))

(defn assoc-in! [an-atom ks v]
  "update atom with (assoc-in @v ks v)"
  (swap! an-atom swap-assoc-in ks v))

(defn assoc-in?! [an-atom ks v]
  "update atom with (assoc-in @v ks v) but only if values differ"  
  ;; have to do this outside to prevent triggering watchers
  (when (differs-in? @an-atom ks v)
    (swap! an-atom swap-assoc-in ks v)))

(defn dissoc-in! [an-atom ks]
  (swap! an-atom swap-dissoc-in ks))

(defn remove-in! [an-atom ks pred?]
  (swap! an-atom swap-remove-in ks remove-i pred?))

(defn filter-in! [an-atom ks pred?]
  (swap! an-atom swap-remove-in ks filter-i pred?))

(defn remove-by-index-in! [an-atom ks index-set]
  (swap! an-atom swap-remove-in ks remove-by-index-i index-set))

(defn filter-by-index-in! [an-atom ks index-set]
  (swap! an-atom swap-remove-in ks filter-by-index-i index-set))

(defn append-in! [an-atom ks & values]
  (swap! an-atom swap-append-in ks values))
