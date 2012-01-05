(ns cljs-todos.x.ui-state-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.map :as umap]
    [dsann.utils.seq :as useq]
    
    [dsann.utils.state :as us]
    )
  )


(def initial-ui-state
  {:updaters 
   {:paths {}
    :ui-element-paths {} 
    }
   ; use for recycling? :ui-elements - maybe clone nodes
   })

;; move to specific
(defn ui-element-id [e]
  (or (. e (getAttribute "pinotid")) (.id e)))


(defn remove-update-paths [updaters ui-element]
  (let [id (ui-element-id ui-element)]
    (if-let [element-paths (get-in updaters [:ui-element-paths id])]
      (let [updaters (update-in updaters [:ui-element-paths] dissoc id)]
        (loop [[p & paths] element-paths 
               updaters updaters]
          (if (nil? p)
            updaters
            (let [new-updaters (-> updaters
                                 (umap/dissoc-in [:paths p id])
                                 (umap/dissoc-in [:any-paths p id]))]
              (recur paths new-updaters)))))
      updaters)))


(defn add-data-paths [updaters [p & paths] element-id v]
  (if (nil? p)
    updaters
    (let [new-updaters (assoc-in updaters [:paths p element-id] v)] 
      (recur new-updaters paths element-id v)))) 


(defn update-data-paths [updaters old-ui-element new-ui-element update-paths update-fn]
  (let [element-id (ui-element-id new-ui-element)
        updaters (-> updaters 
                   (remove-update-paths old-ui-element)
                   (assoc-in [:ui-element-paths element-id] update-paths))
        primary-path (first update-paths)
        v {:update-fn! update-fn :primary-path primary-path}
        ]
    (add-data-paths updaters update-paths element-id v)))

(defn update-data-any-paths [updaters old-ui-element new-ui-element primary-path update-fn]
  (let [element-id (ui-element-id new-ui-element)
        v {:update-fn! update-fn :primary-path primary-path}]
    (-> updaters
      (remove-update-paths old-ui-element)
      (assoc-in [:ui-element-paths element-id] [primary-path])
      (assoc-in [:any-paths primary-path element-id] v))))

;; call whenever a ui-element is updated
;; old-ui-element is the previous element if the element was replaced
;; ui-element is the dependent element
;; update-paths are the state paths that were read during the update
;; update-fn is (fn [new-state]) that will update (or replace) the node
(defn updated-ui-element! [ui-state 
                           old-ui-element ui-element 
                           primary-path sub-paths 
                           update-fn]
  (cond
    (or (nil? sub-paths) (set? sub-paths) (sequential? sub-paths))
    (let [paths (cons primary-path (map #(vec (concat primary-path %)) sub-paths))]
      (us/update-in! 
        ui-state [:updaters] 
        update-data-paths old-ui-element ui-element paths update-fn))
    
    (= :ANY sub-paths)
    (us/update-in! 
      ui-state [:updaters] 
      update-data-any-paths old-ui-element ui-element primary-path update-fn)
  ;:else
  ; EXCEPTION
  :else
  (u/log-str "Failed to add update-paths" sub-paths)
  ))
    

;; call whenever an element is deleted
(defn remove-update-paths! [ui-state ui-element]
  (us/update-in! ui-state [:updaters] remove-update-paths ui-element))
      
(defn get-sub-path-update-fns [ui-state data-path]
  (let [any-path-map (get-in ui-state [:updaters :any-paths])]
    (useq/mapcat- nil?
               (fn [[k v]] 
                 (if (= k (take (count k) data-path))
                   (vals v)))
               any-path-map)))
                 
(defn get-exact-path-update-fns [ui-state data-path]
  (let [element-map (get-in ui-state [:updaters :paths data-path])]
    (vals element-map)))

(defn get-update-fns [ui-state data-path]
  (concat 
    (get-exact-path-update-fns ui-state data-path)
    (get-sub-path-update-fns ui-state data-path)))

; maps data paths to ui update-fnuctions
(defn update-ui! [application old-app-state new-app-state]
  (let [ui-state @(:ui-state application)
        data-path (:update-path (meta new-app-state))]
    (u/log-str "update-ui" (meta new-app-state))
    (if-let [update-fns (get-update-fns ui-state data-path)]
      (doseq [{:keys [update-fn! primary-path]} update-fns] 
        (update-fn! primary-path new-app-state))
      (u/log-str "No update-functions for updated path: " data-path))))

(defn get-data-paths [ui-state ui-element-id]
  (get-in ui-state [:updaters :ui-element-paths ui-element-id]))

(defn get-primary-data-path [ui-state ui-element-id]
  (first (get-data-paths ui-state ui-element-id)))

(defn get-secondary-data-paths [ui-state ui-element-id]
  (rest (get-data-paths ui-state ui-element-id)))


