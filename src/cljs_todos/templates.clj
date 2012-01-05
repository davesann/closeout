(ns cljs-todos.templates
  (:require 
    [clojure.string :as s]
    [dsann.utils.map :as umap]
  ))


(def todo
  [:div {:class "component todo" :data-component-type "todo"} 
   [:div.display
    [:input.check {:type "checkbox"}]
    [:div.todo-text]
    [:span.todo-destroy]
    ]
   [:div.edit
    [:input {:class "todo-input" :type "text" :value ""}]
    ]
   ])

(def todo-list
  [:ul#todo-list {:class "component todo-list" :data-component-type "todo-list"}])

(def todo-stats
  [:div {:class "todo-stats component" :data-component-type "todo-stats"}])

(def todo-list-item [:li todo])
   
(def todo-app 
  [:div 
   {:class "todoapp component"
    :data-component-type "todoapp"}
   [:div.title
    [:h1 "Todos"]
    ]
   
   [:div.content
    [:div.create-todo
     [:div [:input.new-todo {:placeholder "What needs to be done?" :type "text"}]]
     [:span.ui-tooltip-top {:style "display:none;"} "Press Enter to save this task"]]
    todo-stats
    [:div.todos todo-list]
    ]
   ])
     



(defn stats [{:keys [total remaining done]}]
  [:div
   {:class "todo-stats component"
    :data-component-type "todo-stats"}
   (remove nil?
           [
            (if (> total 0)
              [:span.todo-count
               [:span.number (str remaining " ")]
               [:span.word (if (= 1 remaining) "item" "items")] " left."])
            (if (> done 0)
              [:span.todo-clear
               [:a {:href "#"}
                "Clear " [:span.number-done (str done)] " completed " 
                [:span.word-done (if (= done 1) "item" "items")]]])
            ])
   ])


(defn todo [{:keys [done? desc]}]
  [:li     
   [:div {:class
          (s/join " " (remove nil? ["component" "todo" (if done? "done")]))
          :data-component-type "todo"
          }
    [:div.display
     [:input.check (umap/assoc-if {:type "checkbox"}
                                  done? 
                                  :checked "checked")]
     [:div.todo-text desc]
     [:span.todo-destroy]
     ]
    [:div.edit
     [:input {:class "todo-input" :type "text" :value desc}  
      ]
     ]
    ]
   ]
  )


  