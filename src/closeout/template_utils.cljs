(ns closeout.template-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.cljs-utils.dom.find   :as udfind]
    [dsann.cljs-utils.dom.data   :as udd]
    
    [dsann.utils.state.read-notifier :as rn]
    [dsann.utils.state.mirror :as usm]
    
    [goog.dom.classes :as gcls]
    [goog.dom :as gdom]
    [goog.dom.dataset :as gdata]
    [goog.events :as gevents]
    
    [piccup.html :as ph]

    [closeout.behaviour-utils :as co-bu]
  ))

(defn ensure-activation! [application old-node new-node]
  (when (not= old-node new-node)
    ;; if replaced ensure that the new node can be activated
    (gcls/add  new-node "template")
    (gdata/set new-node "templateName" 
               (gdata/get old-node "templateName"))
    
    (co-bu/deactivate! application old-node)
    (gdom/replaceNode  new-node old-node)
    (co-bu/activate!   application new-node nil)))
  

;; TODO: these three are very similar

;; :ANY or :EXACT
(defn update-ui-element! 
  [update-type ui-element update-element-fn! application data-path old-app-state new-app-state]
  (let [updated-node   (update-element-fn! ui-element data-path old-app-state new-app-state)
        new-update-fn  (partial update-ui-element! update-type updated-node update-element-fn! application)
        ]
    (usm/updated-ui-element! 
      (:ui-state application) ui-element updated-node update-type data-path nil new-update-fn)
    (ensure-activation! application ui-element updated-node)
    updated-node))

(defn update-ui-element-identified-sub-paths! 
  [ui-element update-element-fn! application data-path old-app-state new-app-state]
  (let [state-read (atom (set))
        notifier (fn [path] (swap! state-read conj path))]
    (binding [rn/*read-notifier* notifier]
      (let [updated-node   (update-element-fn! ui-element data-path old-app-state new-app-state)
            sub-paths      @state-read
            new-update-fn  (partial update-ui-element-identified-sub-paths! 
                                    updated-node update-element-fn! application)]
        (usm/updated-ui-element! 
          (:ui-state application) ui-element updated-node :EXACT data-path sub-paths new-update-fn)
        (ensure-activation! application ui-element updated-node)
        updated-node))))

(defn update-ui-element-defined-sub-paths! 
  [ui-element update-element-fn! sub-paths application data-path old-app-state new-app-state]
  (let [updated-node   (update-element-fn! ui-element data-path old-app-state new-app-state)
        new-update-fn  (partial update-ui-element-defined-sub-paths! 
                                updated-node update-element-fn! sub-paths application)]
    (usm/updated-ui-element! 
      (:ui-state application) ui-element updated-node :EXACT data-path sub-paths update-fn)
    (ensure-activation! application ui-element updated-node)
    updated-node))


(defn render-node [application placeholder-element]
  (let [t-name    (get-template-name placeholder-element)
        templates (:ui-templates application)]
    (if-let [render    (:render-fn (templates t-name))]
      (render t-name)
      (u/log-str "WARNING - No render fn for template named" t-name)
    )))

(defn init-node! [t-name ui-element application data-path app-state]
  (let [templates (:ui-templates application)
        init!     (:init-fn! (templates t-name))]
    (if init!
      (init! ui-element application data-path app-state))))

(defn get-template-name [ui-element]
  (keyword (gdata/get ui-element "templateName"))) 

(defn get-bound-path [ui-element data-path]
  (if-let [t-bind (gdata/get ui-element "templateBindKw")]
    (conj data-path (keyword t-bind))
    (if-let [t-bind (gdata/get ui-element "templateBindInt")]
      (conj data-path (js/parseInt t-bind 10))
      (if-let [t-bind (gdata/get ui-element "templateBindStr")]
        (conj data-path t-bind)
        (if-let [t-bind (gdata/get ui-element "templateBindSeq")]
          (apply (partial conj data-path) (reader/read t-bind))
          data-path)))))

;; call for newly created elements
(defn initialise-update-loop! [application ui-element data-path app-state]
  (let [t-name          (get-template-name ui-element)
        bound-data-path (get-bound-path ui-element data-path)
        ui-element  (if-not (gcls/has ui-element "placeholder")
                      ui-element
                      (let [new-node  (render-node application ui-element)]
                        (gdom/replaceNode  new-node ui-element)
                        ;(co-bu/deactivate! application ui-element)
                        (co-bu/activate!   application new-node)
                        new-node))
        ]
    (init-node! t-name ui-element application bound-data-path app-state)
    
    ;; REPEAT FOR SUB-TEMPLATES
    (doseq [p (udfind/by-class "placeholder" ui-element)]
      (initialise-update-loop! application p bound-data-path app-state))
    ))


(defn make-basic-render-fn [static-template]
  (fn [template-name]
    (let [n (first (ph/html static-template))]
      (gcls/add n "template")
      (gdata/set n "templateName" template-name)
      n)))


(defn init! [watch-key app-state ui-root templates]
  (do 
    (u/log "init")
    (let [;app-state (atom initial-app-state)
          ui-state  (atom {})
          application {:app-state    app-state 
                       :ui-state     ui-state 
                       :ui-root      ui-root
                       :ui-templates templates
                       }
          notifier-fn (fn [_k a old-state new-state]
                        (usm/update-ui! application old-state new-state)
                        ;(u/log-str application)
                        (u/log-str "num event listeners" (gevents/getTotalListenerCount)))
          ]
      (initialise-update-loop! application ui-root [] @app-state)
      (add-watch app-state watch-key notifier-fn)
     ; (u/log-str ui-state)
      )
    
    (u/log "init done")
    ))


(defn make-init-ANY [update-fn!]
  (fn [ui-element application data-path app-state]
    (update-ui-element! :ANY ui-element update-fn! application data-path nil app-state)))


;; FIX ME SUBPATHS
(defn make-init-EXACT 
  ([update-fn!] (make-init-EXACT update-fn! nil))
  ([update-fn! sub-paths]
    (fn [ui-element application data-path app-state]
      (update-ui-element! :EXACT ui-element update-fn! application data-path nil app-state))))

(defn make-init-EXACT-identified-sub-paths 
  [update-fn!]
  (fn [ui-element application data-path app-state]
    (update-ui-element-identified-sub-paths!
      ui-element update-fn! application data-path nil app-state)))



            
      