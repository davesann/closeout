(ns cljs-todos.x.main
  (:require 
    [dsann.utils.x.core :as u]

    [goog.dom :as gdom]
    
    [cljs-todos.x.todos :as todos]
    [cljs-todos.x.todos2 :as todos2]
   ))


;(todos/init)

(def todos-fixtures
  (vec (for [i (range 500)]
         {:desc (str "do something: " i)
          :done? false
          ;:due-date nil
          })))

(let [ui-root (gdom/getElement "todo-app")
      todos-data {:todos todos-fixtures}] 
  (todos2/init ui-root todos-data))
