(ns closeout.core
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.state.mirror :as usm]
    
    [closeout.node-update :as nu]
    [closeout.template-utils :as tu]
    [closeout.template-list-utils :as tlu]
    
    [goog.events :as gevents]
    
  ))


;; these are used in defining application templates
(defn update-on-ANY-data-path-change [node-update-fn!]
  (fn [node application data-path app-state]
    (nu/start-node-update! 
      node-update-fn! node :ANY 
      application data-path nil app-state)))

(defn update-on-EXACT-data-path-change
  ([node-update-fn!] (make-update-EXACT-path node-update-fn! nil))
  ([node-update-fn! sub-paths]
    (fn [node application data-path app-state]
      (nu/start-node-update-for-defined-sub-paths!
        node-update-fn! node sub-paths 
        application data-path nil app-state))))

(defn update-on-EXACT-data-path-change-with-identified-sub-paths 
  [node-update-fn!]
  (fn [node application data-path app-state]
    (nu/start-node-update-for-identfied-sub-paths!
      node-update-fn! node 
      application data-path nil app-state)))

(defn template-list [template-name]
   (fn [node application data-path app-state]
    (tlu/update-ui-list-element! 
      template-name node application data-path nil app-state)))


(defn init! [watch-key app-state root-node templates]
  (do 
    (u/log "init")
    (let [application {:app-state  app-state 
                       :ui-state   (atom {}) 
                       :templates  templates}
          notifier-fn (fn [_k _r o n]
                        (usm/data-changed! application o n)
                        ;(u/log-str application)
                        (u/log-str "num event listeners" (gevents/getTotalListenerCount)))
          ]
      (tu/initialise-node-update! root-node application [] @app-state)
      (add-watch app-state watch-key notifier-fn)
      ; (u/log-str ui-state)
      )
    (u/log "init done")
    ))






            
      