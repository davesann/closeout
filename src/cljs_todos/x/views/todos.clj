(ns cljs-todos.x.views.todos
  (:require
    [noir.core :as nc]
    [hiccup.core :as hc]
    [hiccup.page-helpers :as hph]
    
    [cljs-todos.templates :as templates]
    )
  )

(nc/defpage "/todos" []
  (hph/html5
    [:head
     [:title "Todos"]
     [:meta {:http-equiv "Content-Type" :content "text/html; charset=UTF-8"}]
     (hph/include-css "/css/reset.css")
     (hph/include-css "/css/g-todos.css")
     ]
    [:body 
     [:div#todo-app templates/todo-app]]
    (hph/include-js "/cljs/bootstrap.js")
    ))

