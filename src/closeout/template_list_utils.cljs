(ns closeout.template-list-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.state.update :as us]
    [dsann.utils.state.mirror :as usm]
    [dsann.utils.state.list-morph :as lm]
    
    [dsann.cljs-utils.js       :as ujs]
    [dsann.cljs-utils.dom.find :as udfind]
    
    [piccup.html :as ph]
    
    [goog.dom :as gdom]
    
    [closeout.protocols.behaviour :as cpb]
    [closeout.template-utils  :as co-tu]
    ))

(defn make-li! [application data-path app-state template-name idx]  
  (let [li (first (ph/html [:li]))
        lc (first (ph/html [:div.placeholder {:data-template-name     template-name
                                              :data-template-bind-int idx}]))
        ]
    (gdom/appendChild li lc)
    (co-tu/initialise-node-update! lc application data-path app-state)
    li))


;; need to do with yield
;; loop?

(defn list-update-remove! [application data-path container-element removed-indices] 
  (when removed-indices
    (let [min-index (apply min removed-indices)]
      (let [current-nodes (ujs/array->coll (gdom/getChildren container-element))]
        (let [update-list (loop [child-index min-index
                                 current-index min-index
                                 child-nodes (drop min-index current-nodes)
                                 result []
                                 ]
                            (if-not (seq child-nodes)
                              (reverse result)
                              (let [[c & rc] child-nodes]
                                ;(u/log-str child-index)
                                (if (removed-indices child-index)
                                  (recur (inc child-index) current-index rc      
                                         (cons [:remove c child-index] result))
                                  (recur (inc child-index) (inc current-index) rc 
                                         (cons [:path-change c current-index] result))))))
              f (fn [u]
                  (let [[action child idx] u]
                    (cond
                      (= :remove action)
                      (cpb/deactivate! child application)
                      
                      (= :path-change action)
                      (data-path-changed! (:ui-state application) child data-path idx)
                      )))
              ]
          (doseq [u update-list] (f u))
          ;(ujs/doseq-with-yield update-list f 50 10)
          )))))


(defn list-update-remove1! [application data-path container-element removed-indices] 
  (when removed-indices
    (let [min-index (apply min removed-indices)]
     ; (u/log-str "LIST-remove" removed-indices)
      ;(u/log-str "LIST-remove" data-path)
      (let [current-nodes (ujs/array->coll (gdom/getChildren container-element))]
        (loop [child-index min-index
               current-index min-index
               child-nodes (drop min-index current-nodes)]
          (when (seq child-nodes)
            (let [[c & rc] child-nodes]
              ;(u/log-str child-index)
              (if (removed-indices child-index)
                (do
                  (cpb/deactivate! c application)
                  (recur (inc child-index) current-index rc)) 
                (do
                  ;; this takes a loooong time for large lists
                  (data-path-changed! (:ui-state application) c data-path current-index)
                  (recur (inc child-index) (inc current-index) rc)))))
          )
        ;(u/log-str "NEW ui-state" @(:ui-state application))
        ))
    ;(u/log-str @(:app-state application))
    ))



(defn list-update-append! [template-name application data-path container-element new-app-state appended-count]
  (let [current-index (.length (gdom/getChildren container-element))]
    (doseq [idx (range current-index (+ current-index appended-count))]
      (let [n (make-li! application data-path new-app-state template-name idx)]
        (gdom/appendChild container-element n)))))

(defn ui-element-id [e]
  (or (. e (getAttribute "pinotid")) (.id e)))


;; updates all template nodes dependent on a datapath (with a new terminal idx)
(defn data-path-changed! [ui-state n data-path idx]
  (let [new-data-path (conj data-path idx)
        new-data-path-len (count new-data-path)]
    ;; adjust all nodes below
    ;; not sure if the order of application may cause issues for embedded lists
    ;; either way, this could be very slow for large sets
    (doseq [t (udfind/by-class-inclusive "template" n)]
      (let [id (ui-element-id t)
            primary-data-path (usm/get-primary-data-path @ui-state id)
            primary-data-path-tail (drop new-data-path-len primary-data-path)
            
            new-primary-data-path (if (seq primary-data-path-tail)
                                    (apply (partial conj new-data-path) primary-data-path-tail)
                                    new-data-path) 
            ]
        (usm/data-path-changed! ui-state t new-primary-data-path)))
    n))
                

    

(defn list-update-generic-1! [template-name application data-path container-element old-app-state new-app-state]
  (let [current-nodes (ujs/array->coll (gdom/getChildren container-element))
        new-list (get-in new-app-state data-path)
        old-list (get-in old-app-state data-path)
        {:keys [new-list-changes old-list-deletes]} (lm/find-list-morph old-list new-list)
        current-nodes-vec (vec current-nodes)]
    
    ;; deletes
    (doseq [i old-list-deletes]
      (let [n (current-nodes-vec i)]
        (cpb/deactivate! n application)))
    
    ;; inserts and moves
    (let [updates (drop-while #(= :unchanged (first (second %))) 
                              (sort-by first new-list-changes))]
      (doseq [[idx [action v]] updates]
        (let [old-node (current-nodes-vec idx)
              new-node 
              (cond 
                (= action :insert)
                (let [n (make-li! application data-path new-app-state template-name idx)]
                  ;(cpb/activate! n application)
                  n)
                
                (= action :move)
                (let [n (current-nodes-vec v)]
                  (data-path-changed! (:ui-state application) n data-path idx)
                  n)

                (= action :unchanged)  ;; default??
                old-node
                
                ;:else EXCEPTION?
                )]
          (cond
            (not old-node)
            (gdom/appendChild container-element new-node)
            
            (not= old-node new-node)
            (gdom/insertChildAt container-element new-node idx)
            ))))))

(defn list-update-generic! [template-name application data-path container-element old-app-state new-app-state]
  (let [new-list (get-in new-app-state data-path)
        old-list (get-in old-app-state data-path)
        current-nodes (ujs/array->coll (gdom/getChildren container-element))
        current-nodes-vec (vec current-nodes)
        {:keys [new-list-changes old-list-deletes]} (lm/find-list-morph old-list new-list)
        ]
    ; (u/log-str "Changes" new-list-changes)
    ; (u/log-str "deletes" old-list-deletes)
    ; (u/log-str application)
    
    ;; deletes
    (doseq [i old-list-deletes]
      (let [n (current-nodes-vec i)]
        (cpb/deactivate! n application)))
    
    ;; inserts and moves
    (let [updates (drop-while #(= :unchanged (first (second %))) 
                              (sort-by first new-list-changes))
          f (fn [item]
              (let [[idx [action v]] item
                    old-node (current-nodes-vec idx)
                    new-node 
                    (cond 
                      (= action :insert)
                      (let [n (make-li! application data-path new-app-state template-name idx)]
                        ;(cpb/activate! n application)
                        n)
                      
                      (= action :move)
                      (let [n (current-nodes-vec v)]
                        (data-path-changed! (:ui-state application) n data-path idx)
                        n)
                      
                      (= action :unchanged)  ;; default??
                      old-node
                      
                      ;:else EXCEPTION?
                      )]
                ;           (u/log old-node)
                ;            (u/log new-node)
                ;           (u/log-str ">>>>>>")
                (cond
                  (not old-node)
                  (gdom/appendChild container-element new-node)
                  
                  (not= old-node new-node)
                  (gdom/insertChildAt container-element new-node idx)
                  )))]
      (ujs/doseq-with-yield updates f 50 20))))


(defn list-update! [template-name application container-element data-path old-app-state new-app-state]
  (let [m (meta new-app-state)
        action (:action m) ]
    ; (u/log-str "List update" m)
    (cond
      (= action :list-remove)
      (let [removed-indices (:removed-indices (meta new-app-state))] 
        (list-update-remove! application data-path container-element removed-indices))
      
      (= action :list-append)
      (let [appended-count (count (:appended m))]
        (list-update-append! template-name application data-path container-element new-app-state appended-count))
      
      :else
      (list-update-generic! template-name application data-path container-element old-app-state new-app-state)
      )
    container-element))


(defn update-ui-list-element! 
  [template-name ui-element application data-path old-app-state new-app-state]
  (let [updated-node   (list-update! template-name application ui-element data-path old-app-state new-app-state)
        new-update-fn  (partial update-ui-list-element! template-name updated-node application)
        ]
    
    (usm/update-on-data-change!
      (:ui-state application) ui-element updated-node :EXACT data-path nil new-update-fn)
    (cpb/updated! ui-element updated-node application)
    updated-node))




      