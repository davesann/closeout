(ns cljs-todos.x.todos
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.map :as umap]
    [dsann.utils.seq :as useq]
    
    [dsann.cljs-utils.x.dom.events :as ude]
    [dsann.cljs-utils.x.dom.fx     :as udfx]
    [dsann.cljs-utils.x.dom.dom    :as udom]
    [dsann.cljs-utils.x.dom.find   :as udfind]

    [dsann.utils.state :as us]
    
    [cljs-todos.templates       :as templates]
    [cljs-todos.x.todos-behaviour :as behaviour]
    
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

(defn todo-stats [todos-list]
  (let [t (count todos-list)
        d (count (filter :done? todos-list))
        r (- t d)]
    {:total t
     :done  d
     :remaining r}))


(defn render-todo-stats [app-atom data-path todo-list]
  (let [stats (todo-stats todo-list)
        e (first (ph/html (templates/stats stats)))]
    (behaviour/activate! e {:app-atom app-atom :data-path data-path})
    e))

(defn render-todo [app-atom data-path todo]
  (let [e (first (ph/html (templates/todo todo)))]
    (behaviour/activate! e {:app-atom app-atom :data-path data-path})
    e))

(defn render-todo-list [app-atom data-path todos]
  (if-let [children (map-indexed 
                      (fn [i item]
                        (render-todo app-atom (conj data-path i) item))
                      todos)]
    (let [e (first (ph/html [:ul.todo-list]))]
      (doseq [c children]
        (udom/append! e c))
      (behaviour/activate! e {:app-atom app-atom :data-path data-path})
      e)))


(defn render-app [app-atom new-state]
  (do
    (let [p [:todos-app :todo-list]
          todos-list (get-in new-state p)]
      (let [e (render-todo-list app-atom p todos-list )
            placeholder (udfind/first-by-class "todo-list")   ;; should be based on an identified template
            ]
        (gdom/replaceNode e placeholder)
        (behaviour/deactivate! placeholder)
        )
      
      (let [e (render-todo-stats app-atom p todos-list)
            placeholder (udfind/first-by-class "todo-stats")
            ]
        (gdom/replaceNode e placeholder)
        (behaviour/deactivate! placeholder)
        )
      )
    ))

(defn update-ui [k a old-state new-state]
  (do 
    (let [data-path (:update-path (meta new-state))]
      (u/log-str "update-ui" data-path)
      (render-app a new-state))
    (u/log-str "total-listener-count" (gevents/getTotalListenerCount))
    ))

(def todos-fixtures
  {:todo-list
   (vec (for [i (range 50)]
          {:desc (str "do something: " i)
           :done? false
           ;:due-date nil
           }))
   })

(defn init []
  (do 
    (u/log "todos init")
    
    (let [todos (atom {:todos-app {:todo-list []}})]    
      (behaviour/activate! [(udom/body)] 
                   {:app-atom todos
                    :app-path [:todos-app :todo-list]})
      (add-watch todos :update update-ui)
      (us/assoc-in! todos [:todos-app] todos-fixtures)
      )
    
    (u/log "todos-init done")
    ))



