(ns closeout.core
  (:require 
    [dsann.utils.x.core :as u]
    [closeout.state.mirror :as usm]
    [closeout.node-update :as nu]
    [closeout.render.core :as rc]    
    
  ;  [goog.events :as gevents]
  ))

;; these are used in defining application templates
(defn update-on-ANY-data-path-change [node-update-fn!]
  (fn [node application data-path app-state]
    (nu/start-node-update! 
      node-update-fn! node :ANY 
      application data-path nil app-state)))

(defn update-on-EXACT-data-path-change
  ([node-update-fn!] (update-on-EXACT-data-path-change node-update-fn! nil))
  ([node-update-fn! sub-paths]
    (fn [node application data-path app-state]
      (nu/start-node-update-for-defined-sub-paths!
        node-update-fn! node sub-paths 
        application data-path nil app-state))))

(defn update-on-EXACT-data-path-change-with-identified-sub-paths 
  [node-update-fn!]
  (fn [node application data-path app-state]
    (nu/start-node-update-for-identified-sub-paths!
      node-update-fn! node 
      application data-path nil app-state)))
  
(defn template-list [make-list-item!]
   (fn [node application data-path app-state]
    (nu/list-update! make-list-item! node application data-path nil app-state)))


;; initialises the state->node updates 
(defn init! [watch-key app-state root-node templates]
  (do 
   ; (u/log "init")
    (let [application {:app-state  app-state 
                       :mirror-state   (atom {}) 
                       :templates  templates}
          notifier-fn (fn [_k _r o n]
                        (usm/data-changed! application o n)
                        ;(u/log-str application)
                        ;(u/log-str "num event listeners" (gevents/getTotalListenerCount))
                        )
          ]
      (rc/init-node! root-node application [] @app-state)
      (add-watch app-state watch-key notifier-fn)
      ; (u/log-str mirror-state)
      )
   ; (u/log "init done")
    ))






            
      