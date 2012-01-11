(ns cljs-todos.x.closeout.ui-state-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.map :as umap]
    [dsann.utils.seq :as useq]
    
    [dsann.utils.state :as us]
    )
  )

;; move to specific
(defn ui-element-id [e]
  (or (. e (getAttribute "pinotid")) (.id e)))



(defn get-element-paths [updaters element-id]
  (get-in updaters [:ui-element-paths element-id]))
                    
(defn get-lookup-type [updaters element-id]
  (get-in updaters [:ui-element-paths element-id :lookup-type]))

(defn get-update-fn [updaters element-id]
  (get-in updaters [:ui-element-paths element-id :update-fn!]))


;; generic
(defn remove-update-paths [updaters ui-element]
  (let [id (ui-element-id ui-element)]
    (if-let [element-paths (get-in updaters [:ui-element-paths id])]
      (let [updaters (update-in updaters [:ui-element-paths] dissoc id)]
        (loop [[p & paths] (:data-paths element-paths) 
               updaters updaters]
          (if (nil? p)
            updaters
            (let [new-updaters (-> updaters
                                 (umap/dissoc-in [:data-paths :EXACT p id])
                                 (umap/dissoc-in [:data-paths :ANY   p id]))]
              (recur paths new-updaters)))))
      updaters)))

(defn add-data-paths [updaters lookup-type [p & paths] element-id v]
  (if (nil? p)
    updaters
    (let [new-updaters (assoc-in updaters [:data-paths lookup-type p element-id] v)] 
      (recur new-updaters lookup-type paths element-id v)))) 

(defn update-data-paths [updaters old-ui-element new-ui-element lookup-type update-paths update-fn]
  (let [element-id (ui-element-id new-ui-element)
        v {:lookup-type lookup-type
           :data-paths update-paths
           :primary-data-path (first update-paths)
           :update-fn! update-fn}
        updaters (-> updaters 
                   (remove-update-paths old-ui-element)
                   (assoc-in [:ui-element-paths element-id] v))]
    (add-data-paths updaters lookup-type update-paths element-id v)))

(defn update-data-path-changed [updaters ui-element new-primary-path]
  (let [element-id (ui-element-id ui-element)
        old-paths (:data-paths (get-in updaters [:ui-element-paths element-id]))
        [old-primary-path & old-sub-paths] old-paths
        ldrop (count old-primary-path)
        new-paths (cons new-primary-path 
                        (map (fn [osp]
                               (vec (concat new-primary-path 
                                            (drop ldrop osp))))
                             old-sub-paths))
        element-paths (get-element-paths updaters element-id)
        lookup-type (:lookup-type element-paths)
        update-fn!   (:update-fn! element-paths)
        ]
    (update-data-paths updaters ui-element ui-element lookup-type new-paths update-fn!)))
    
;; call whenever a ui-element is updated
;; old-ui-element is the previous element if the element was replaced
;; ui-element is the dependent element
;; update-paths are the state paths that were read during the update
;; update-fn is (fn [new-state]) that will update (or replace) the node
(defn updated-ui-element! [ui-state 
                           old-ui-element ui-element
                           lookup-type primary-path sub-paths update-fn]
  (let [paths (cons primary-path (map #(vec (concat primary-path %)) sub-paths))]
    (us/update-in! 
      ui-state [:updaters] 
      update-data-paths old-ui-element ui-element lookup-type paths update-fn)))

;; call if a ui-elements data-path changes (eg list deletion)
(defn data-path-changed! [ui-state ui-element new-primary-path]
  (us/update-in! ui-state [:updaters]
                 update-data-path-changed ui-element new-primary-path))

;; call whenever an element is deleted
(defn remove-update-paths! [ui-state ui-element]
  (us/update-in! ui-state [:updaters] remove-update-paths ui-element))
      
(defn seq-starts-with [a-seq sub-seq]
  (if (not (seq sub-seq))
    true
    (let [fs  (first a-seq)
          fss (first sub-seq)]
      (if (= fs fss)
        (recur (rest a-seq) (rest sub-seq))
        false))))
          
(defn get-ANY-update-fns [ui-state data-path]
  (let [any-path-map (get-in ui-state [:updaters :data-paths :ANY])]
    (useq/mapcat- nil?
                  (fn [[p v]]
                    (if (seq-starts-with data-path p)
                   (vals v)))
                  any-path-map)))

(defn get-EXACT-update-fns [ui-state data-path]
  (let [element-map (get-in ui-state [:updaters :data-paths :EXACT data-path])]
    (vals element-map)))

(defn get-update-fns [ui-state data-path]
  (concat 
    (get-EXACT-update-fns ui-state data-path)
    (get-ANY-update-fns   ui-state data-path)))

(defn get-data-paths [ui-state ui-element-id]
  (get-in ui-state [:updaters :ui-element-paths ui-element-id :data-paths]))

(defn get-primary-data-path [ui-state ui-element-id]
  (get-in ui-state [:updaters :ui-element-paths ui-element-id :primary-data-path]))

; maps data paths to ui update-fnuctions
(defn update-ui! [application old-app-state new-app-state]
  (let [ui-state @(:ui-state application)
        data-path (:update-path (meta new-app-state))]
    (u/log-str "update-ui meta" (meta new-app-state))
   ;(u/log-str "updaters" (:updaters ui-state))
    ;(u/log-str new-app-state)
    ;(u/log-str "num update-fns" (count (get-update-fns ui-state data-path)))
    (if-let [update-fns (get-update-fns ui-state data-path)]
      (do
        (u/log-str "Update-fns:" (count update-fns))
        (doseq [{:keys [update-fn! primary-data-path]} update-fns] 
          (update-fn! primary-data-path old-app-state new-app-state))
        (u/log-str "Done Ui Update"))
      (u/log-str "No update-functions for updated path: " data-path))))



