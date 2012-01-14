(ns closeout.template-maplist-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.seq :as useq]
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


(defn make-li! [application data-path app-state template-name k]  
  (let [li (first (ph/html [:li]))
        lc (first (ph/html [:div.placeholder {:data-template-name     template-name
                                              :data-template-bind-kw  k}]))
        ]
    (gdom/appendChild li lc)
    (co-tu/initialise-node-update! lc application data-path app-state)
    li))


(defn indexed-deleted-keys [old-map new-map]
  (useq/map-indexed- nil? (fn [i [k v]]
                            (if-not (contains? new-map k)
                              [i k]))
                     old-map))

(defn maplist-update-remove! [template-name application data-path container-element old-app-state new-app-state]
 (let [new-map (get-in new-app-state data-path)
       old-map (get-in old-app-state data-path)
       deleted-keys (indexed-deleted-keys old-map new-map)
       current-nodes-vec (vec (ujs/array->coll (gdom/getChildren container-element)))
       ]
   (u/log-str "maplist-update-remove!" deleted-keys)
   (doseq [[idx k] deleted-keys]
     (let [n (current-nodes-vec idx)]
       (gdom/removeNode n)
       (cpb/deactivate! n application)))))
     

(defn maplist-update-generic! [template-name application data-path container-element old-app-state new-app-state]
  (let [current-nodes (ujs/array->coll (gdom/getChildren container-element))
        m (meta new-app-state)
        
        new-map (get-in new-app-state data-path)
        old-map (get-in old-app-state data-path)
        ;new-keys (keys new-map)
        ;old-keys (keys old-map)
        
        new-list (seq new-map)  ; convert map to seqs
        old-list (seq old-map)
        {:keys [new-list-changes old-list-deletes]} (lm/find-list-morph old-list new-list)
        current-nodes-vec (vec current-nodes)
        ]
    ;(u/log-str "old-list" new-list)
    ;(u/log-str "new-list" new-list)
    ;(u/log-str "Changes" new-list-changes)
    ;(u/log-str "deletes" old-list-deletes)
   ; (u/log-str application)
    
    ;; deletes
    (doseq [i old-list-deletes]
      (let [n (current-nodes-vec i)]
        (gdom/removeNode n)
        (cpb/deactivate! n application)))
    
    ;; inserts and moves
    (let [updates (drop-while #(= :unchanged (first (second %))) 
                              (sort-by first new-list-changes))
          f (fn [item]
              (let [[idx [action v]] item
                    old-node (current-nodes-vec idx)
                    new-node 
                    (cond 
                      (= action :move)
                      (current-nodes-vec v) ;; v is the old index
     
                      (= action :insert)
                      (let [[child-name _child-value] v]
                        (make-li! application data-path new-app-state template-name child-name))
                      
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
        



(defn maplist-update! [template-name application container-element data-path old-app-state new-app-state]
  (let [current-nodes (ujs/array->coll (gdom/getChildren container-element))
        m (meta new-app-state)
        action (:action m)
        ]
    (u/log-str "MapList update")
    (cond 
      (= action :dissoc)
      (maplist-update-remove! template-name application data-path container-element old-app-state new-app-state)
      
      :else
      (maplist-update-generic! template-name application data-path container-element old-app-state new-app-state)
      )
    container-element))


;; identical to map but using map list
;; differentiated because they have template name
;;; todo DEFINE container update element to simplify

(defn update-ui-maplist-element! 
  [template-name ui-element application data-path old-app-state new-app-state]
  (let [updated-node   (maplist-update! template-name application ui-element data-path old-app-state new-app-state)
        new-update-fn  (partial update-ui-maplist-element! template-name updated-node application)
        ]
    
    (usm/update-on-data-change!
      (:ui-state application) ui-element updated-node :ANY data-path nil new-update-fn)
    (when (not= ui-element updated-node)
      (cpb/deactivate! ui-element application)
      (gdom/replaceNode  updated-node ui-element)
      (cpb/activate!   updated-node application))
    updated-node))

(defn make-init-maplist [template-name]
   (fn [ui-element application data-path app-state]
    (update-ui-maplist-element! template-name ui-element application data-path nil app-state)))




      