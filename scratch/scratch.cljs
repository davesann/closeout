
(defn f [app-atom create? i]
  (do
    (u/log (str i " " create?))
    (if create?
     (su/set-in!   app-atom [:todos-app :todo-list 9] {:desc (str "create" i) :done false})
      (su/dissoc-in!   app-atom [:todos-app :todo-list 9] )
      )
    (if (< i 1000)
      (js/setTimeout #(f app-atom (not create?) (inc i)) 10))))



(def nodes 
  (ph/html 
    [:div#id1.new "Hi, I'm new"
     [:div#id2.click-class "click 1"
      [:div [:div [:div#id4.click-class "click 4"]]]
      ]
     ]
    [:div#id3.click-class "click 2"]
    ))

(let [x  {:a 1 :b 2}]
  (udom/append! 
    (udom/body)
    nodes
    x
  ))

(js/setTimeout #(udom/remove! nodes) 2000)



;(defn find-best [ui-state data-path]
;  (loop [data-path data-path]
;    (if (empty? data-path)
;      nil
;      (if-let [v (get-in ui-state data-path)]
;        v
;        (recur (pop data-path))))))
      

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



;; update shortcuts
;(defn create-desc-update-fn [todo-node] 
;  (fn [new-desc]
;    (if-let [desc-elem (gdom/getElementByClass "todo-text" todo-node)]
;      (gdom/setTextContent desc-elem new-desc))))

; Event management
;(udom/add-watch! 
;  :disposing :id 
;  (fn [notify-type id nodes context]
;    (doseq [n nodes]
;      (udom/doto-node-and-children n #(gevents/removeAll %)))))
