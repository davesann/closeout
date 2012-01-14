(ns closeout.template-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.cljs-utils.dom.find   :as udfind]
    [dsann.cljs-utils.dom.data   :as udd]
    
    [closeout.node-update :as node-update]
    
    [goog.dom.classes :as gcls]
    [goog.dom :as gdom]
    [goog.dom.dataset :as gdata]
        
    [piccup.html :as ph]

    [closeout.protocols.behaviour :as cpb] ; possibly remove
    [closeout.protocols.template-binding :as cptb]
  ))


(defn placeholder? [node]
  (gcls/has node "placeholder"))

(defn find-placeholders [node]
  (udfind/by-class "placeholder" node))

(defn render-node [application placeholder-node]
  (let [template-name  (cptb/name placeholder-node)
        templates      (:templates application)
        template       (templates template-name)
        hiccup         (or (:hiccup-template template) [:div])
        n (first (ph/html hiccup))]
    ; add class and name for update and behaviour
    (gcls/add  n "template")
    (gdata/set n "templateName" template-name)
    n))

;; call for newly created elements
(defn initialise-node-update! 
  [node application data-path app-state]
  (let [template-name   (cptb/name node)
        bound-data-path (cptb/bound-path node data-path)
        node  (if-not (placeholder? node)
                node
                (let [new-node  (render-node application node)]
                  (cpb/updated! node new-node application )
                  new-node))
        templates     (:templates application)
        node-updater! (:node-updater! (templates template-name))]
    
    (if node-updater! 
      (node-updater! node application bound-data-path app-state))
    
    ;; repeat for any introduced sub-templates (placeholders)
    (doseq [p (find-placeholders node)]
      (initialise-node-update! p application bound-data-path app-state))
    ))






            
      