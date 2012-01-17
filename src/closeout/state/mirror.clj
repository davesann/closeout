(ns closeout.state.mirror
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.map :as umap]
    [dsann.utils.seq :as useq]
    
    [dsann.utils.protocols.identifiable :as p-id]
    
    
    [closeout.state.update :as us]
    
    )
  )

;; TODO : tidy, improve naming and document

;; utility function

(defn get-node-paths [updaters node-id]
  (get-in updaters [:node-paths node-id]))
                    
(defn get-lookup-type [updaters node-id]
  (get-in updaters [:node-paths node-id :lookup-type]))

(defn get-update-fn [updaters node-id]
  (get-in updaters [:node-paths node-id :update-fn!]))

(defn remove-node-path [updaters id]
  (update-in updaters [:node-paths] dissoc id))


;; get update-functions
(defn get-ANY-update-fns [mirror-state data-path]
  (let [any-path-map (get-in mirror-state [:updaters :data-paths :ANY])]
    (useq/mapcat- nil?
                  (fn [[p v]]
                    (if (useq/starts-with data-path p)
                   (vals v)))
                  any-path-map)))

(defn get-EXACT-update-fns [mirror-state data-path]
  (if-let [element-map (get-in  mirror-state [:updaters :data-paths :EXACT data-path])]
    (vals element-map)))

(defn get-update-fns [mirror-state data-path]
  (concat 
    (get-EXACT-update-fns mirror-state data-path)
    (get-ANY-update-fns   mirror-state data-path)))

(defn get-data-paths [mirror-state node-id]
  (get-in mirror-state [:updaters :node-paths node-id :data-paths]))

(defn get-primary-data-path [mirror-state node-id]
  (get-in mirror-state [:updaters :node-paths node-id :primary-data-path]))


;; manage data-paths

(defn remove-data-paths [updaters p id]
  (-> updaters
    (umap/dissoc-in [:data-paths :EXACT p id])
    (umap/dissoc-in [:data-paths :ANY   p id])))

(defn remove-update-paths [updaters node]
  (let [id (p-id/id node)]
    (if-let [node-paths (get-node-paths updaters id)]
      (let [updaters (remove-node-path updaters id)]
        (loop [[p & paths] (:data-paths node-paths) 
               updaters updaters]
          (if (nil? p)
            updaters
            (recur paths (remove-data-paths updaters p id)))))
      updaters)))

(defn add-data-paths [updaters lookup-type [p & paths] node-id v]
  (if (nil? p)
    updaters
    (let [new-updaters (assoc-in updaters [:data-paths lookup-type p node-id] v)] 
      (recur new-updaters lookup-type paths node-id v)))) 

(defn update-data-paths [updaters old-node new-node lookup-type update-paths update-fn]
  (let [node-id (p-id/id new-node)
        v {:lookup-type lookup-type
           :data-paths update-paths
           :primary-data-path (first update-paths)
           :update-fn! update-fn}
        updaters (-> updaters 
                   (remove-update-paths old-node)
                   (assoc-in [:node-paths node-id] v))]
    (add-data-paths updaters lookup-type update-paths node-id v)))

(defn update-data-path-changed [updaters node new-primary-path]
  (let [node-id (p-id/id node)
        old-paths (:data-paths (get-in updaters [:node-paths node-id]))
        [old-primary-path & old-sub-paths] old-paths
        ldrop (count old-primary-path)
        new-paths (cons new-primary-path 
                        (map (fn [osp]
                               (vec (concat new-primary-path 
                                            (drop ldrop osp))))
                             old-sub-paths))
        node-paths (get-node-paths updaters node-id)
        lookup-type (:lookup-type node-paths)
        update-fn!   (:update-fn! node-paths)
        ]
    (update-data-paths updaters node node lookup-type new-paths update-fn!)))
    




;; registed for update whenever a data is updated
;; old-node is the previous dependent element for this data, if the node was replaced
;; node is the dependent element
;; update-paths are the state paths that were read during the update
;; update-fn is (fn [new-state]) that will update (or replace) the node
(defn update-on-data-change! [mirror-state 
                              old-node node
                              lookup-type primary-path sub-paths update-fn]
  (let [paths (cons primary-path (map #(vec (concat primary-path %)) sub-paths))]
    (us/update-in! 
      mirror-state [:updaters] 
      update-data-paths old-node node lookup-type paths update-fn)))

;; change the data path for node to new-primary-path
;;  call if a nodes data-path changes (eg list deletion)
(defn data-path-changed! [mirror-state node new-primary-path]
  (us/update-in! mirror-state [:updaters]
                 update-data-path-changed node new-primary-path))

;; deregisters a node for updates
;; It is importatn to call this to ensure that stale inforamtion and paths do 
;;  not build up in the updaters 
(defn remove-update-paths! [mirror-state node]
  (us/update-in! mirror-state [:updaters] remove-update-paths node))
      
; maps data paths to ui update-fnuctions
(defn data-changed! [application old-app-state new-app-state]
  (let [mirror-state @(:mirror-state application)
        data-path (:update-path (meta new-app-state))]
    ;(u/log-str "update-ui meta" (meta new-app-state))
    ;(u/log-str "updaters" (:updaters mirror-state))
    ;(u/log-str new-app-state)
    (if-let [update-fns (get-update-fns mirror-state data-path)]
      (do
        ;(u/log-str "Number of update-fns:" (count update-fns))
        (doseq [{:keys [update-fn! primary-data-path]} update-fns]
          (update-fn! primary-data-path old-app-state new-app-state))
        ;(u/log-str "Done Ui Update")
        )
      (u/log-str "No update-functions for updated path: " data-path))))



