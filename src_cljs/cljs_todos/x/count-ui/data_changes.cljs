(ns cljs-todos.x.count-ui.data-changes
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.state :as us]
    )
  )

(defn update-count [app-state data-path]
  (let [data (get-in @app-state data-path)
        c (:value data)
        d (str (js/Date.))
        c-inc (inc c)
        count-value-path (conj data-path :value)
        history-path (conj data-path :history-maplist (keyword c-inc))
        history-item {:date d :count c-inc}
        history-list-path (conj data-path :history-list)
        ]
    (us/assoc-in! app-state count-value-path c-inc)
;    (us/assoc-in! app-state history-path history-item)
    (us/update-in! app-state history-list-path 
                   (fn [v & args]
                     (if v (apply (partial conj v) args)
                       (vec args))) 
                   history-item)
    ))

