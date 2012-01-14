(ns closeout.behaviour-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.seq :as useq]
    [dsann.utils.state.mirror :as usm]
    
    [dsann.cljs-utils.dom.dom    :as udom]
    [dsann.cljs-utils.dom.find   :as udfind]
    [dsann.cljs-utils.dom.data   :as udd]
    
    

    [goog.dom.dataset :as gdata]
    [goog.dom :as gdom]
    [goog.events :as gevents]
    
    [closeout.protocols.template-binding :as cptb]
    [closeout.protocols.behaviour :as cpb]
    
    )
  (:require-macros
    [dsann.utils.macros.ctrl :as uctrl]
    )
  )


(defn apply-behaviour! [ui-element application]
  (if-let [template-name (cptb/name ui-element)]
    (let [templates  (:templates application)]
      (when-let [behaviour! (:behaviour-fn! (templates template-name))]
        (behaviour! application ui-element)
        (gdata/set ui-element "behaviourActive" "true")))))

(defn deactivate-node! [ui-state n]
  (gevents/removeAll n)                   ; remove events
  (gdata/remove n "behaviourActive")
  (usm/remove-update-paths! ui-state n)   ; remove updates                 
  )


(extend-protocol cpb/Behaviour
  js/Node
  (activate! [n application]
             (doseq [component (udfind/by-class-inclusive "template" n)]
               (when-not (gdata/get component "behaviourActive")
                 (apply-behaviour! component application))))

  (deactivate! [n application]
               (let [ui-state (:ui-state application)]
                 ; this will be a problem for large trees - need to manage better
                 ; probably by limiting where listeners are placed
                 (udom/doto-node-and-children 
                   n 
                   (partial deactivate-node! ui-state))
                 (gdom/removeNode n)
                 ))
  
  (updated! [old-node new-node application]
            (when (not= old-node new-node)
              ;; if replaced ensure that the new node can be activated
              (cptb/name! new-node (cptb/name old-node))
              
              (gdom/replaceNode    new-node old-node)
              (cpb/deactivate!     old-node application)
              (cpb/activate!       new-node application)
              ))
  )
