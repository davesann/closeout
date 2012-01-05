(ns cljs-todos.x.todos
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.map :as umap]
    
    [dsann.cljs-utils.x.dom.events :as ude]
    [dsann.cljs-utils.x.dom.fx :as udfx]
    
    [dsann.utils.state :as us]
    
    [cljs-todos.templates :as templates]
    
    [clojure.string :as s]
    
    [goog.dom :as gdom]
    [goog.dom.classes :as gcls]
    [goog.style :as gstyle]
    [goog.events :as gevents]
    
    [pinot.dom :as pdom]
    [pinot.html :as ph]
    
    )
  )

;; update shortcuts
;(defn create-desc-update-fn [todo-node] 
;  (fn [new-desc]
;    (if-let [desc-elem (gdom/getElementByClass "todo-text" todo-node)]
;      (gdom/setTextContent desc-elem new-desc))))
  

(defn render-todo-stats [app-atom data-path todo-list]
  (let [stats (todo-stats todo-list)
        e (first (ph/html (templates/stats stats)))]
    ;; attach events
    
    (ude/on (gdom/getElementByClass "todo-clear" e ) 
            :click
            
            (fn [e evt]
              (us/update-in! app-atom data-path
                         #(vec (remove :done? %)))))
    
    e))

(defn toggle-done! [todo-path]
  (us/update-in! app-atom (conj data-path :done?) not))

(defn render-todo [app-atom data-path todo]
 (let [e (first (ph/html (templates/todo todo)))]

   ; attach events
   (ude/on e :click
           (fn [e evt]
             (let [target (.target evt)]
               (cond 
                 (gcls/has target "check")
                 (us/update-in! app-atom (conj data-path :done?) not)
                 
                 (gcls/has target "todo-destroy")
                 (us/dissoc-in! app-atom data-path)
                 ))))
   
   
   (ude/on e :dblclick
           (fn [e evt]
             (let [target (.target evt)]
               (if-let [target (if (gcls/has target "todo-text") target)]
                 (gcls/add e "editing")))))
   
   (ude/on e :keypress
           (fn [e evt]
             (let [target (.target evt)]
               (if (= (.keyCode evt) 13)
                 (if-let [target (if (gcls/has target "todo-input") target)]
                   (us/set-in! app-atom (conj data-path :desc) (.value target))
                   )))))      
   e))


(defn render-todo-list [app-atom data-path todos]
  (if-let [children (map-indexed 
                      (fn [i item]
                        (render-todo app-atom (conj data-path i) item))
                      todos)]
    (let [e (first (ph/html [:ul#todo-list]))]
      (doseq [c children] (pdom/append e c))
      ; attach events
      
      e)))

(defn todo-stats [todos-list]
  (let [t (count todos-list)
        d (count (filter :done? todos-list))
        r (- t d)]
    {:total t
     :done  d
     :remaining r}))


    ;(let childArray (gdom/getChildren node)
    ;  (doall
    ;    (for [x (range 0 (.length childArray))]
    ;      (let [c (aget childArray x)]
    ;        (doto-node-and-children c f)))))

            

(defn render-app [app-atom new-state]
  (do
    (let [p [:todos-app :todo-list]
          todos-list (get-in new-state p)]
      (let [e (render-todo-list app-atom p todos-list )
            placeholder (gdom/getElement "todo-list")
            ]
        (gdom/replaceNode e placeholder)
        )
      
      (let [e (render-todo-stats app-atom p todos-list)
            placeholder (gdom/getElement "todo-stats")
            ]
        (gdom/replaceNode e placeholder)
        )
      )
    ))
  

(defn find-best [ui-state data-path]
  (loop [data-path data-path]
    (if (empty? data-path)
      nil
      (if-let [v (get-in ui-state data-path)]
        v
        (recur (pop data-path))))))
      

;(let [ui-state (atom {})]
;  (defn update-ui [k a old-state new-state]
;    (do 
;      (let [data-path (::update-path (meta new-state))]
;        (u/log-str "update-ui" data-path)
;        (if-let [ui-state1 
;                 (if-let [ui-element (find-best @ui-state data-path)]
;                   ; render specific
;                   (do (u/log-str "render" ui-element)
;                     nil)
 ;                  (render-app a new-state))]
;          ;(swap! ui-state merge ui-state1)
;          ;(u/log-str "new-ui-state" ui-state1)
 ;         nil
 ;         ))
;      (u/log-str "total-listener-count" (gevents/getTotalListenerCount))
;      )))

  (defn update-ui [k a old-state new-state]
    (do 
      (let [data-path (::update-path (meta new-state))]
        (u/log-str "update-ui" data-path)
        (render-app a new-state))
      (u/log-str "total-listener-count" (gevents/getTotalListenerCount))
      ))

(def todos-fixtures
  {:todo-list
   (vec (for [i (range 10)]
          {:desc (str "do something: " i)
           :done? false
           ;:due-date nil
           }))
   })

(defn init []
  (do 
    (u/log "todos init")
    (let [todos (atom {:todos-app {:todo-list []}})
          create-todo (gdom/getElement "create-todo")
          new-todo    (gdom/getElement "new-todo" create-todo)
          tooltip     (gdom/getElementByClass "ui-tooltip-top" create-todo)
          ]
      
      ; application "static element" events
      (ude/on new-todo :keypress 
              (fn [e evt]
                (u/log "press")
                (when (= (.keyCode evt) 13)
                  (us/update-in! todos [:todos-app :todo-list]
                                 conj  
                                 {:desc (.value e)
                                  :done? false})
                  (set! (.value e) "")
                  )))
      
      (ude/on new-todo :keyup 
              (let [timeout (atom nil)]
                (fn [e evt]
                  (udfx/fadeout-and-hide tooltip)
                  (swap! timeout #(do (if %) (js/clearTimeout %)))
                  (let [v (.value e)]
                    (if-not (or (= v "") (= v "placeholder"))
                      (reset! timeout (js/setTimeout #(udfx/show-and-fadein tooltip) 1000)))))))
      
      (add-watch todos :update update-ui)
      (us/set-in! todos [:todos-app] todos-fixtures)
      
      (u/log (gevents/removeAll new-todo))
      )
    (u/log "todos-init done")
    ))



