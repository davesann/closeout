(ns closeout.render.core
  (:require 
    [dsann.utils.x.core :as u]
    [closeout.protocols.template-binding :as tb]
  ))

(defn init-node! 
  [node application data-path app-state]
  (let [template-name   (tb/name node)
        bound-data-path (tb/bound-path node data-path)
        node  (if-not (tb/placeholder? node)
                node
                (let [new-node  (tb/render node application)]
                  (tb/updated! node new-node application )
                  new-node))
        templates     (:templates application)
        node-updater! (:node-updater! (templates template-name))]
    (if node-updater! 
      (node-updater! node application bound-data-path app-state))
    
    ;; repeat for any introduced sub-templates (placeholders)
    (doseq [p (tb/find-placeholders node)]
      (init-node! p application bound-data-path app-state))
    ))






            
      