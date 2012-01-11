(ns cljs-todos.x.main
  (:require 
    [dsann.utils.x.core :as u]
    
    [goog.dom :as gdom]
    
    
    [cljs-todos.x.closeout.template-utils :as co-tu]
    [cljs-todos.x.count-ui.templates :as templates]
    
    [cljs.reader :as reader]
    
    [dsann.utils.state :as us]
    
    [dsann.cljs-utils.x.js :as ujs]
    
    [cljs-todos.x.count-ui.data-changes :as dc]
    
    
   ))

(def *print-fn* u/log-str)


(def todos-fixtures
  {:todos (merge (for [i (range 5)]
                   {i {:id i
                       :desc (str "do something: " i)
                       :done? false
                       ;:due-date nil
                       }}
                   ))
   })

(def count-fixtures {:count {:value 0}})

(def app-state (atom count-fixtures))
(def ui-root (gdom/getElement "app"))


(defn f [i] (dc/update-count app-state [:count]))

;(ujs/doseq-with-delay (range 100) f 10)
;(ujs/doseq-with-yield (range 200) f 100 5)
(doseq [i (range 100)] (f i))

;(time (us/map-moves (range 1000) (for [i (range 1000)] (rand-int 1000))))

;; clear the update status before initialisation
(reset! app-state (with-meta @app-state {}))
(co-tu/init! ::app app-state ui-root templates/templates)


