(ns cljs-todos.x.count-ui.templates
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.state :as su]
    
    [cljs-todos.x.closeout.ui-state-utils      :as co-su]
    [cljs-todos.x.closeout.template-utils      :as co-tu]
    [cljs-todos.x.closeout.template-list-utils :as co-tlu]
    [cljs-todos.x.closeout.template-maplist-utils :as co-tmlu]
    
    [pinot.html :as ph]
    
    [dsann.cljs-utils.x.dom.find   :as udfind]
    
    [goog.dom :as gdom]
    [goog.dom.classes :as gcls]
    [goog.events :as gevents]
    [goog.events.EventType :as et]
    [goog.dom.dataset :as gdata]
    
    [dsann.utils.state :as us]
    
    [cljs-todos.x.count-ui.data-changes :as dc]
    )
  )


(defn ui-element-id [e]
  (or (. e (getAttribute "pinotid")) (.id e)))


;; template 1
(def static
  [:div
   [:p "count updated at: " [:span.some-text "some text"]]
   [:div.counter.count-trigger ]
   [:div.history-list "Update MAPList"
    [:div.placeholder {:data-template-name    "history-maplist"
                       :data-template-bind-kw "history-maplist"}]]
   
   [:div.history-list "Update List"
    [:div.placeholder {:data-template-name    "history-list"
                       :data-template-bind-kw "history-list"}]
    
    
    [:div.placeholder {:data-template-name "count-app-verbose"}]
    ]
   ])

(defn update! [ui-element data-path old-app-state new-app-state]
  (do 
    (u/log-str data-path)
    (let [data (get-in new-app-state data-path)
          count-value (:value data)
          count-node  (udfind/first-by-class "counter" ui-element)]
      (gdom/setTextContent count-node (str count-value)))
    (let [txt-node (udfind/first-by-class "some-text" ui-element)]
      (gdom/setTextContent txt-node (str (js/Date.))))
    ui-element))

;; template 2

;; need to add the template name separately to allow move of templates
;; add name on render

(def static2
  [:span.count-trigger "the count is: " [:span.counter]])

(defn update2! [ui-element data-path old-app-state new-app-state]
  (let [count-value (get-in new-app-state (conj data-path :value))
        count-node  (udfind/first-by-class "counter" ui-element)
        ]
    (if (= count-value 1)
      (gdom/setTextContent count-node (str count-value " item"))
      (gdom/setTextContent count-node (str count-value " items")))
    ui-element))


(defn behaviour! [application ui-element context]
  (u/log "add events for counter")
  ;(u/log node)
  (let [ui-state  (:ui-state  application)
        app-state (:app-state application)
        id        (ui-element-id ui-element)]
    
       (if-let [count-node (udfind/first-by-class-inclusive "count-trigger" ui-element)]
         (gevents/listen 
           count-node et/CLICK
           (fn [evt]
             (let [data-path (co-su/get-primary-data-path @ui-state id)]
               (dc/update-count app-state data-path)
               )))
         (u/log "Warning" "counter"))
       (u/log "done add events for counter") 
       ))



(def app-static
  [:div 
   [:div.placeholder {:data-template-name    "count-app"
                      :data-template-bind-kw "count"
                      }]
   
   
   ;[:div.placeholder {:data-template-name "count-app-verbose"
   ;                   :data-template-bind "count"
   ;                   }]
   ])


(def count-history-static
  [:div "Count-history: "
   [:span.count ]
   [:span " "]
   [:span "upated on: " [:span.date  ]]
   [:ol.list]
   ])

(defn count-history-update! [ui-element data-path old-app-state new-app-state]
  (do 
    (let [date-node (udfind/first-by-class-inclusive "date" ui-element)]
      (gdom/setTextContent  date-node (js/Date.)))
    (let [the-list (get-in new-app-state data-path)]
      (let [n (udfind/first-by-class-inclusive "count" ui-element)]
        (gdom/setTextContent n (count the-list)))
      (let [n (udfind/first-by-class-inclusive "list" ui-element)]
        (gdom/removeChildren n)
        (doseq [[k v] the-list]
          (let [c (:count v)
                d (:date v)
                child (first (ph/html [:li (str c " -> " d "   rendered at :" (js/Date.))]))]
            (gdom/appendChild n child))))
      )
    ui-element))

(def history-list-item-static [:div])

(defn history-list-item-update! [ui-element data-path old-app-state new-app-state]
  (do
    (let [raw-data (get-in new-app-state data-path)
          print-data (assoc raw-data :render-time (js/Date.))]
      (gdom/setTextContent ui-element (pr-str print-data)))
    ui-element))


(defn history-list-item-behaviour! [application ui-element context]
  (u/log "add events for history-list-item")
  (let [ui-state  (:ui-state  application)
        app-state (:app-state application)
        id        (ui-element-id ui-element)]
    
    (gevents/listen 
      ui-element et/CLICK
      (fn [evt]
        (let [data-path (co-su/get-primary-data-path @ui-state id)]
          (u/log-str "Click history-list-item" id)
          (u/log-str "Node id" id)
          (u/log-str "data path" data-path)
          
          ;delete the item
          (let [idx       (last data-path)
                list-path (vec (butlast data-path))
                ]
            (su/remove-by-index-in! app-state list-path #{idx})
;            (su/dissoc-in! app-state data-path)
            )
          
          
          )))
    (u/log "done add events for history-list-item") 
    ))


(def templates
  {
   :main
   {:render-fn     (co-tu/make-basic-render-fn app-static)
    :init-fn!      nil ; no init required
    :behaviour-fn! nil ; no behaviour
    }
   
   :count-app 
   {:render-fn     (co-tu/make-basic-render-fn static)
    :init-fn!      (co-tu/make-init-ANY update!)
    :behaviour-fn! behaviour!
    }
  
   :count-app-verbose 
   {:render-fn     (co-tu/make-basic-render-fn static2)
    :init-fn!      (co-tu/make-init-ANY update2!)
    :behaviour-fn! behaviour!
    }
   
   :count-history 
   {:render-fn     (co-tu/make-basic-render-fn count-history-static)
    :init-fn!      (co-tu/make-init-ANY count-history-update!)
    :behaviour-fn! nil
    }
   
   :history-list 
   {:render-fn     (co-tu/make-basic-render-fn [:ol])
    :init-fn!      (co-tlu/make-init-list :history-list-item)
    :behaviour-fn! nil
    }
   
   :history-maplist 
   {:render-fn     (co-tu/make-basic-render-fn [:ol])
    :init-fn!      (co-tmlu/make-init-maplist :history-list-item)
    :behaviour-fn! nil
    }
   
   
   :history-list-item 
   {:render-fn     (co-tu/make-basic-render-fn history-list-item-static)
    :init-fn!      (co-tu/make-init-ANY history-list-item-update!) 
    :behaviour-fn! history-list-item-behaviour!
    }
      

   })  
  
            
      