(ns cljs-todos.x.templates
  (:require 
    [dsann.utils.x.core :as u]
    
    [dsann.utils.state :as us]
    
    [dsann.cljs-utils.x.dom.dom    :as udom]
    [dsann.cljs-utils.x.dom.find   :as udfind]

    
    [clojure.string :as s]
    [dsann.utils.map :as umap]
    [cljs-todos.templates :as templates]
    [pinot.html :as ph]
    
    [goog.dom.forms   :as gforms]
    [goog.dom.classes :as gcls]
    [goog.dom :as gdom]
    [dsann.cljs-utils.x.js :as ujs]
    [cljs-todos.x.todos-behaviour :as behaviour]
    [cljs-todos.x.ui-state-utils :as ui-su]
    
    
  )
  (:require-macros 
    [dsann.utils.macros.ctrl :as uctrl]
    )
  )

(def *print-fn* u/log-str)

(defn create! [hiccup-template context]
  (let [n (first (ph/html hiccup-template))]
    (behaviour/activate! n context)
    n))

(defn create-and-replace! [dom-node hiccup-template context]
  (let [n (first (ph/html hiccup-template))]
    (behaviour/deactivate! dom-node context)
    (behaviour/activate! n context)
    (gdom/replaceNode n dom-node)
    n))

(defn new-node! [template context]
  (let [n (first (ph/html template))]
    (behaviour/activate! n context)
    n))

(def ^:dynamic *read-notifier* u/log-str)

(defn tget-in 
  ([a-map path]
    (do 
      (*read-notifier* path)
      (get-in a-map path)))
  ([a-map path not-found]
    (do 
      (*read-notifier* path)
      (get-in a-map path not-found))))


;; this update fn updates selected parts
(defn update-todo-list-item [todo-node todo-item]
  (let [done? (tget-in todo-item [:done?])
        desc  (tget-in todo-item [:desc])
        check-box  (udfind/first-by-class "check"      todo-node)
        todo-text  (udfind/first-by-class "todo-text"  todo-node)
        todo-input (udfind/first-by-class "todo-input" todo-node)
        ]
    (if done?
      (do 
        (gcls/add todo-node "done")
        (gforms/setValue check-box "checked"))
      (do
        (gcls/remove todo-node "done")
        (gforms/setValue check-box)))
    (when (not= (gdom/getTextContent todo-text) desc)
      (gdom/setTextContent todo-text desc)
      (gforms/setValue todo-input desc))
    todo-node))

(defn ui-update-todo-list-item [application todo-node data-path current-app-state]
  (let [state-read (atom (set))
        notifier (fn [path] (swap! state-read conj path))]
    (binding [*read-notifier* notifier]
      (let [data (get-in current-app-state data-path)
            todo-node (update-todo-list-item todo-node data)
            update-fn (fn [data-path data] (ui-update-todo-list-item application todo-node data-path data))]
        (ui-su/updated-ui-element! 
          (:ui-state application) todo-node todo-node data-path @state-read  update-fn)
        todo-node))))

(defn new-todo-node! [application data-path app-state context]
  (ui-update-todo-list-item
    application
    (new-node! templates/todo-list-item context)
    data-path
    app-state))



(defn todo-stats [todos-list]
  (let [t (count todos-list)
        d (count (filter :done? todos-list))
        r (- t d)]
    {:total t
     :done  d
     :remaining r}))



;; this update fn does a full redraw
(defn update-todo-list-stats [application dom-node todo-list]
  (let [new-node (first (ph/html (templates/stats (todo-stats todo-list))))]
    (behaviour/activate! new-node application)
    (gdom/replaceNode new-node dom-node)
    (behaviour/deactivate! dom-node application)
    new-node))

(defn ui-update-todo-list-stats [application dom-node data-path current-app-state]
  (do (u/log "update stats")
  (when-let [todo-list (get-in current-app-state data-path)]
    (let [new-node (update-todo-list-stats application dom-node todo-list)
          update-fn (fn [data-path data] 
                      (ui-update-todo-list-stats application new-node data-path data))]
      (ui-su/updated-ui-element! 
        (:ui-state application) dom-node new-node data-path :ANY update-fn)
      new-node))))

;; this update fn replaces all list nodes
(defn update-todo-list [application list-node data-path app-state]
  (when-let [todo-list (get-in app-state data-path)]
    (doseq [c  (ujs/array->coll (gdom/getChildren list-node))]
      (gdom/removeNode c)
      (behaviour/deactivate! c application))
    
    (doseq [i (range (count todo-list))] 
      (let [node-data-path (conj data-path i)
            todo-node (new-todo-node! application node-data-path app-state application)]
        (udom/append! list-node todo-node)))
    list-node))


(defn ui-update-todo-list [application dom-node data-path current-app-state]
  (do 
    (u/log "update todo-list")
    (when-let [todo-list (get-in current-app-state data-path)]
      (let [new-node (update-todo-list application dom-node data-path current-app-state)
            update-fn (fn [data-path data] 
                      (ui-update-todo-list application new-node data-path data))]
        (ui-su/updated-ui-element! 
          (:ui-state application) dom-node new-node data-path nil update-fn)
      new-node))))

            
      