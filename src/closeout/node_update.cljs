(ns closeout.node-update
  (:require 
    [dsann.utils.x.core :as u]
    
    [dsann.utils.state.read-notifier :as rn]
    [dsann.utils.state.mirror :as usm]
    
    [closeout.protocols.behaviour :as cpb]
  ))

;; this is fully generic


;; need to document the big picture what is happening here
;; because of the way these are bound - no change to 
;;   node-update-fn! is possible after the initial setup.
;;   could this be required?

;; all of these register an update fn of the form
;; (update-fn [data-path old-app-state new-app-state] ...)
;; that will be called when the data on the registered paths changes.
;; the function automatically re-registers with 
;;  (possibly) updated node and sub-paths on each call 

;; node is any mutation based tree that allows in place upadte.
;; may be able to relax this constraint


(defn node-update! 
  [node-update-fn! old-node update-type sub-paths 
   application 
   data-path old-app-state new-app-state]
  (let [new-node   (node-update-fn! old-node data-path old-app-state new-app-state)
        update-fn! (partial node-update!
                            node-update-fn! new-node update-type sub-paths
                            application)]
    ;; register for update on data change
    (usm/update-on-data-change! 
      (:ui-state application) old-node new-node update-type 
      data-path sub-paths update-fn!)
    ;; notify for behaviour activation
    (cpb/updated! old-node new-node application)
    updated-node))


;; :ANY   - registers to be called if the data path or any sub path of it changes 
;; :EXACT - registers to be called if the exact data path changes
;;        -   will not be called for sub paths
(defn start-node-update!
  [node-update-fn! old-node update-type application data-path old-app-state new-app-state]
  (node-update!
    node-update-fn! old-node update-type nil  
    application 
    data-path old-app-state new-app-state))

;; :EXACT - registers to be called if the exact data path changes
;;        -   will be called for any of the given sub paths
;;        -   will not be called for any other sub paths
(defn start-node-update-for-defined-sub-paths! 
  [node-update-fn! old-node sub-paths application data-path old-app-state new-app-state]
  (node-update!
    node-update-fn! old-node :EXACT sub-paths  
    application 
    data-path old-app-state new-app-state))
    

;; records subpaths read by the node-update-fn! and registers these for update
(defn start-node-update-for-identified-sub-paths! 
  [node-update-fn! old-node application data-path old-app-state new-app-state]
  (let [state-read (atom (set))
        notifier (fn [path] (swap! state-read conj path))]
    (binding [rn/*read-notifier* notifier]
      (let [new-node       (node-update-fn! old-node data-path old-app-state new-app-state)
            sub-paths      @state-read
            new-update-fn  (partial start-node-update-for-identified-sub-paths! 
                                    node-update-fn! new-node application)]
        (usm/update-on-data-change! 
          (:ui-state application) old-node new-node :EXACT 
          data-path sub-paths new-update-fn)
        (cpb/updated! ui-element updated-node application)
        updated-node))))


      