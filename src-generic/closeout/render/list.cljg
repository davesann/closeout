(ns closeout.render.list
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.protocols.identifiable :as pid]
    [dsann.utils.protocols.mutable-tree :as mt]

    [closeout.protocols.template-binding :as tb]
    
    [closeout.state.update :as us]
    [closeout.state.mirror :as usm]
    [closeout.state.list-morph :as lm]
    
    [closeout.render.core :as rc]
    
    ))

;; generically manages list template - including add and remove
;; provided the ui/state or orther mutable tree 
;; is extended for dsann.utils.protocols.mutable-tree


;; updates all template nodes dependent on a datapath (with a new terminal idx)
;; specific to list elements
(defn data-path-changed! [mirror-state n data-path idx]
  (let [new-data-path (conj data-path idx)
        new-data-path-len (count new-data-path)]
    ;; adjust all nodes below
    ;; not sure if the order of application may cause issues for embedded lists
    ;; either way, this could be very slow for large sets
    (doseq [t (tb/find-templates n)]
      (let [id (pid/id t)
            primary-data-path (usm/get-primary-data-path @mirror-state id)
            primary-data-path-tail (drop new-data-path-len primary-data-path)
            
            new-primary-data-path (if (seq primary-data-path-tail)
                                    (apply (partial conj new-data-path) primary-data-path-tail)
                                    new-data-path) 
            ]
        (usm/data-path-changed! mirror-state t new-primary-data-path)))
    n))

;; remove items from the list
(defn list-update-remove! [application data-path container-element removed-indices] 
  (when removed-indices
    (let [min-index (apply min removed-indices)]
      (let [current-nodes (mt/get-children container-element)]
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
                      (tb/deactivate! child application)
                      
                      (= :path-change action)
                      (data-path-changed! (:mirror-state application) child data-path idx)
                      )))
              ]
          (doseq [u update-list] (f u))
          ;(ujs/doseq-with-yield update-list f 50 10)
          )))))


;; append items to the list
(defn list-update-append! [make-item! application data-path container-element new-app-state appended-count]
  (let [current-index (mt/child-count container-element)]
    (doseq [idx (range current-index (+ current-index appended-count))]
      (let [n (make-item! application data-path new-app-state idx)]
        (mt/append! container-element n)))))

(defn list-update-generic! [make-item! application data-path container-element old-app-state new-app-state]
  (let [new-list (get-in new-app-state data-path)
        old-list (get-in old-app-state data-path)
        current-nodes (mt/get-children container-element)
        current-nodes-vec (vec current-nodes)
        {:keys [new-list-changes old-list-deletes]} (lm/find-list-morph old-list new-list)
        ]

    ;; deletes
    (doseq [i old-list-deletes]
      (let [n (current-nodes-vec i)]
        (tb/deactivate! n application)))
    
    ;; inserts and moves
    (let [updates (drop-while #(= :unchanged (first (second %))) 
                              (sort-by first new-list-changes))
          f (fn [item]
              (let [[idx [action v]] item
                    old-node (current-nodes-vec idx)
                    new-node 
                    (cond 
                      (= action :insert)
                      (let [n (make-item! application data-path new-app-state idx)]
                        (rc/init-node! n application data-path new-app-state)
                        n)
                      
                      (= action :move)
                      (let [n (current-nodes-vec v)]
                        (data-path-changed! (:mirror-state application) n data-path idx)
                        n)
                      
                      (= action :unchanged)  ;; make this the default?
                      old-node
                      
                      ;:else what EXCEPTION?
                      )]
                (cond
                  (not old-node)
                  (mt/append! container-element new-node)
                  
                  (not= old-node new-node)
                  (mt/insert-at! container-element new-node idx)
                  )))]
      (doseq [u updates] (f u))
      ;(ujs/doseq-with-yield updates f 50 20)
      )))

;; dispatch - can improve
(defn list-update! [make-item! application container-element data-path old-app-state new-app-state]
  (let [m (meta new-app-state)
        action (:action m) ]
    (cond
      (= action :list-remove)
      (let [removed-indices (:removed-indices (meta new-app-state))] 
        (list-update-remove! application data-path container-element removed-indices))
      
      (= action :list-append)
      (let [appended-count (count (:appended m))]
        (list-update-append! make-item! application data-path container-element new-app-state appended-count))
      
      :else
      (list-update-generic! make-item! application data-path container-element old-app-state new-app-state)
      )
    container-element))


      