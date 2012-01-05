(ns cljs-todos.x.todos-behaviour
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.map :as umap]
    [dsann.utils.seq :as useq]
    
    [dsann.cljs-utils.x.dom.events :as ude]
    [dsann.cljs-utils.x.dom.fx     :as udfx]
    [dsann.cljs-utils.x.dom.dom    :as udom]
    [dsann.cljs-utils.x.dom.find   :as udfind]

    [dsann.utils.state :as us]
    [cljs-todos.x.ui-state-utils :as ui-su]
    
    [cljs-todos.templates :as templates]
    
    [clojure.string :as s]
    
    [goog.dom :as gdom]
    [goog.dom.classes :as gcls]
    [goog.style :as gstyle]
    [goog.events :as gevents]
    [goog.events.EventType :as et]
    [goog.dom.dataset :as gdata]
    
    [pinot.dom :as pdom]
    [pinot.html :as ph]    
    )
  (:require-macros
    [dsann.utils.macros.ctrl :as uctrl]
    ))

(defn ui-element-id [e]
  (or (. e (getAttribute "pinotid")) (.id e)))

;; all application component events are defined here
(def event-fn-map
  {
   "todoapp"
   (fn [node context]
     (u/log "add events for todo-app")
     ;(u/log node)
     (let [create-todo (udfind/first-by-class "create-todo" node)
           new-todo    (udfind/first-by-class "new-todo" create-todo)
           tooltip     (udfind/first-by-class "ui-tooltip-top" create-todo)
           
           ui-state  (:ui-state context)
           app-state (:app-state context)
           rendered-node-id (ui-element-id (:rendered-node context))]

       (gevents/listen 
         new-todo et/KEYPRESS 
         (fn [evt]
           (when (= (.keyCode evt) 13)
             (let [new-todo-item {:desc (.value new-todo) :done? false}
                   data-path (ui-su/get-primary-data-path @ui-state rendered-node-id)]
               (us/update-in! app-state (conj data-path :todos) conj new-todo-item)
               (set! (.value new-todo) "")
               ))))
      
      (gevents/listen 
        new-todo et/KEYUP 
        (let [timeout (atom nil)]
          (fn [evt]
            (udfx/fadeout-and-hide tooltip)
            (swap! timeout #(do (if % (js/clearTimeout %))))
            (let [v (.value new-todo)]
              (if-not (or (= v "") (= v "placeholder"))
                (reset! timeout (js/setTimeout #(udfx/show-and-fadein tooltip) 1000)))))))
      ))

   "todo-stats" 
   (fn [node context]
     (u/log "add events for todo-stats")
     ;(u/log node)
     (let [ui-state  (:ui-state  context)
           app-state (:app-state context)
           rendered-node-id (ui-element-id (:rendered-node context))]
       (if-let [todo-clear (udfind/first-by-class "todo-clear" node)]
         (gevents/listen 
           todo-clear et/CLICK 
           (fn [evt]
             (let [data-path (ui-su/get-primary-data-path @ui-state rendered-node-id)]
               (us/update-in! app-state data-path #(vec (remove :done? %))))))
         (u/log "Warning" "todo-clear")
         )
       ))
   
   "todo"
   (fn [node context]
     (u/log "add events for todo")
     ;(u/log node)
     (let [ui-state  (:ui-state  context)
           app-state (:app-state context)
           rendered-node-id (ui-element-id (:rendered-node context))]
     
       (if-let [todo-check (udfind/first-by-class "check" node)]
         (gevents/listen 
           todo-check et/CLICK
           (fn [evt]
             (let [data-path (ui-su/get-primary-data-path @ui-state rendered-node-id)]
               (us/update-in! app-state (conj data-path :done?) not))))
         (u/log "Warning" "check")
         )
       
       (if-let [todo-destroy (udfind/first-by-class "todo-destroy" node)]
         (gevents/listen 
           todo-destroy et/CLICK
           (fn [evt]
             (let [data-path (ui-su/get-primary-data-path @ui-state rendered-node-id)
                   idx (last data-path)]
               (us/update-in! app-state (vec (butlast data-path)) #(vec (concat (take idx %) (drop (inc idx) %))))
               )))
         (u/log "Warning" "destroy")
         )

       (if-let [todo-text (udfind/first-by-class "todo-text" node)]
         (gevents/listen 
           todo-text et/DBLCLICK
           (fn [evt] (gcls/add node "editing")))
         (u/log "Warning" "text")
         )

       (if-let [todo-input (udfind/first-by-class "todo-input" node)]
         (gevents/listen 
           todo-input et/KEYPRESS
           (fn [evt]
             (when (= (.keyCode evt) 13)
               (let [data-path (ui-su/get-primary-data-path @ui-state rendered-node-id)]
                 (gcls/remove node "editing")
                 (us/assoc-in! app-state (conj data-path :desc) (.value todo-input))
                 ))))
         (u/log "Warning" "input")
         )
       ))
     })

(defn activate! [nodes context]
  (doseq [n (useq/ensure-sequential nodes)
          component (udfind/by-class-inclusive "component" n)]
    (when-not (gdata/get component "active")
      (let [context (assoc context :rendered-node n)]
        (uctrl/if-let [component-type (gdata/get component "componentType")
                       event-fn (event-fn-map component-type)]
                      (event-fn component context)))
      (gdata/set component "active" "true"))))

(defn deactivate! [nodes context]
  (let [ui-state (:ui-state context)]
    (doseq [n (useq/ensure-sequential nodes)]
      ;; events to this node are removed
      (udom/doto-node-and-children 
        n 
        #(do 
           (gevents/removeAll %)                     ; remove events
           (ui-su/remove-update-paths! ui-state %)   ; remove updates - fix me slow?                 
           )))
      ))

