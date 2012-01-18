(ns closeout.dom.template-helpers
  (:require     
    [piccup.html :as ph]
    )
  )

(defn li-template-fn [contents-template-name]
  (fn [application data-path app-state idx]
    (first
      (ph/html [:li [:div.placeholder {:data-template-name     contents-template-name
                                       :data-template-bind-int idx}]]))))
