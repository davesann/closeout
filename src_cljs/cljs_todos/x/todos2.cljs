(ns cljs-todos.x.todos2
  (:require 
    [dsann.utils.x.core :as u]
    
    [cljs-todos.x.ui-state-utils :as ui-su]
   
    [goog.dom :as gdom]
    [goog.events :as gevents]
    
    [dsann.cljs-utils.x.dom.find   :as udfind]
    
    [dsann.utils.state :as us]
    
    [cljs-todos.x.todos-behaviour :as behaviour]
    [cljs-todos.x.templates :as templates]
    ))


(defn initialise-ui! [application initial-components]
  (let [{:keys [ui-state app-state]} application
        ui-root (:ui-root @ui-state)]
    
    (u/log-str "initialise ui")  
    (doseq [[component-class data-path update-fn] initial-components]
      (when-let [dom-node (udfind/first-by-class-inclusive component-class ui-root)]
        (update-fn application dom-node data-path nil @app-state)))))

(defn init [ui-root initial-app-state]
  (do 
    (u/log "todos init")
    (let [app-state (atom initial-app-state)
          ui-state  (atom {:ui-root ui-root})
          application {:app-state app-state :ui-state  ui-state}
          notifier-fn (fn [_k a old-state new-state]
                        (ui-su/update-ui! application old-state new-state)
                        ;(u/log-str application)
                        (u/log-str "num event listeners" (gevents/getTotalListenerCount)))
          ]
      (initialise-ui! 
        application 
        [["todo-list"  [:todos] templates/ui-update-todo-list]
         ["todo-stats" [:todos] templates/ui-update-todo-list-stats]
         ])
      (add-watch app-state ::update-ui notifier-fn)
      (behaviour/activate! ui-root application)
      ;(u/log-str ui-state)
      )
    
    (u/log "todos-init done")
    ))


