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
(defn update-todo-list-item [todo-node old-todo-item todo-item]
  (let [done? (tget-in todo-item [:done?])
        desc  (tget-in todo-item [:desc])
        check-box  (udfind/first-by-class "check" todo-node)]
    (u/log-str "old todo" old-todo-item)
    (u/log-str "todo" todo-item)
    (if done?
      (do
        (gcls/add todo-node "done")
        (gforms/setValue check-box "checked"))
      (do
        (gcls/remove todo-node "done")
        (gforms/setValue check-box)))
    (when (not= (:desc old-todo-item) desc)
      (let [todo-text  (udfind/first-by-class "todo-text"  todo-node)
            todo-input (udfind/first-by-class "todo-input" todo-node)]
        (gdom/setTextContent todo-text (str desc " " (js/Date.)))
        (gforms/setValue todo-input desc)))
    todo-node))

(defn ui-update-todo-list-item [application todo-node data-path old-app-state new-app-state]
  (let [state-read (atom (set))
        notifier (fn [path] (swap! state-read conj path))]
    (binding [*read-notifier* notifier]
      (let [data (get-in new-app-state data-path)
            old-data (get-in old-app-state data-path)
            todo-node (update-todo-list-item todo-node old-data data)
            update-fn (fn [data-path old-data new-data] 
                        (ui-update-todo-list-item application todo-node data-path old-data new-data))]
        (ui-su/updated-ui-element! 
          (:ui-state application) todo-node todo-node :EXACT data-path @state-read update-fn)
        todo-node))))

(defn new-todo-node! [application data-path app-state context]
  (ui-update-todo-list-item
    application
    (new-node! templates/todo-list-item context)
    data-path
    nil
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

(defn ui-update-todo-list-stats [application dom-node data-path old-app-state new-app-state]
  (do (u/log "update stats")
  (when-let [todo-list (get-in new-app-state data-path)]
    (let [new-node (update-todo-list-stats application dom-node todo-list)
          update-fn (fn [data-path old-data new-data] 
                      (ui-update-todo-list-stats application new-node data-path old-data new-data))]
      (ui-su/updated-ui-element! 
        (:ui-state application) dom-node new-node :ANY data-path nil update-fn)
      new-node))))


(defn update-list [make-new-node! application list-node data-path old-app-state new-app-state]
  (let [new-data (get-in new-app-state data-path)
        old-data (get-in old-app-state data-path)
        current-nodes (ujs/array->coll (gdom/getChildren list-node))
        
        ]
    (u/log-str "update-list-meta" (meta new-app-state))
    
    (if (= (:action (meta new-app-state))
           :list-remove)
      (let [removed-indices (:removed-indices (meta new-app-state))
            mi (u/log-str "ABC" (apply min removed-indices))]
        (u/log-str "LIST-remove")
        (loop [child-index mi
               current-index mi
               child-nodes (drop mi current-nodes)]
          (when (seq child-nodes)
            (let [[c & rc] child-nodes]
              (u/log-str child-index)
              (if (removed-indices child-index)
                (do
                  (gdom/removeNode c)
                  (behaviour/deactivate! c application)
                  (recur (inc child-index) current-index rc)) 
                (do
                  ;; this takes a loooong time
                  (ui-su/data-path-changed! (:ui-state application) c (conj data-path (u/log-str "NEW-index" current-index)))
                  (recur (inc child-index) (inc current-index) rc)))))
          )
        ;(u/log-str "NEW ui-state" @(:ui-state application))
        )

      
      (let [{:keys [new-list-changes old-list-deletes]} (us/map-moves old-data new-data)
            current-nodes-vec (vec current-nodes)]
        (u/log-str new-list-changes)
        (u/log-str old-list-deletes)

        ;; deletes
        (doseq [i list-deletes]
          (gdom/removeNode (current-nodes-vec i))
          (behaviour/deactivate! new-node application))
        
        ;; inserts and moves
        (let [updates (drop-while #(= :unchanged (first (second %))) 
                                  (sort-by first new-list-changes))]
          
          (doseq [[idx [action v]] updates]
            (let [old-node (current-nodes-vec idx)
                  new-node 
                  (cond 
                    (= action :insert)
                    (let [n (make-new-node! application (conj data-path idx) new-app-state application)]
                      (behaviour/activate! n application)
                      n)
                    
                    (= action :move)
                    (let [n (current-nodes-vec v)]
                      (ui-su/data-path-changed! 
                        (:ui-state application) n (conj data-path idx))
                      n)
                    
                    (= action :unchanged)  ;; default??
                    old-node
                    
                    ;:else EXCEPTION?
                    )]
              (u/log old-node)
              (u/log new-node)
              (cond
                (not old-node)
                (gdom/appendChild list-node new-node)
                
                (not= old-node new-node)
                (gdom/replaceNode old-node new-node))
              ;(gdom/insertChildAt list-node n idx))
            )))
                     
        ;(doseq [c current-nodes]
        ;  (gdom/removeNode c)
        ;  (behaviour/deactivate! c application))
        
        ;(doseq [i (range (count new-data))] 
        ;  (let [item-data-path (conj data-path i)
        ;        new-node (make-new-node! application item-data-path new-app-state application)]
        ;    (udom/append! list-node new-node)))
        )
      )
    list-node))



(def update-todo-list (partial update-list new-todo-node!))

;; this update fn replaces all list nodes
;(defn update-todo-list [application list-node data-path old-app-state app-state]
;  (when-let [todo-list (get-in app-state data-path)]
;    (doseq [c  (ujs/array->coll (gdom/getChildren list-node))]
;      (gdom/removeNode c)
;      (behaviour/deactivate! c application))
;    
;    (doseq [i (range (count todo-list))] 
;      (let [node-data-path (conj data-path i)
;            todo-node (new-todo-node! application node-data-path app-state application)]
;        (udom/append! list-node todo-node)))
;    list-node))


(defn ui-update-todo-list [application dom-node data-path old-app-state new-app-state]
  (do 
    (u/log "update todo-list")
    (when-let [todo-list (get-in new-app-state data-path)]
      (let [new-node (update-todo-list application dom-node data-path old-app-state new-app-state)
            update-fn (fn [data-path old-state new-state] 
                      (ui-update-todo-list application new-node data-path old-state new-state))]
        (ui-su/updated-ui-element! 
          (:ui-state application) dom-node new-node :EXACT data-path nil update-fn)
      new-node))))

            
      