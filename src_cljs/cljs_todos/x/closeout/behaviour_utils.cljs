(ns cljs-todos.x.closeout.behaviour-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.seq :as useq]
    
    [dsann.cljs-utils.x.dom.dom    :as udom]
    [dsann.cljs-utils.x.dom.find   :as udfind]
    [dsann.cljs-utils.x.dom.data   :as udd]
    
    [cljs-todos.x.closeout.ui-state-utils :as co-su]
    
    [goog.dom.dataset :as gdata]
    [goog.events :as gevents]
    )
  (:require-macros
    [dsann.utils.macros.ctrl :as uctrl]
    )
  )


(defn get-template-name [ui-element]
  (keyword (gdata/get ui-element "templateName"))) 

(defn apply-behaviour! [application ui-element]
  (if-let [t-name (get-template-name ui-element)]
    (let [templates  (:ui-templates application)]
      (when-let [behaviour! (:behaviour-fn! (templates t-name))]
        (behaviour! application ui-element)
        (gdata/set ui-element "behaviourActive" "true")))))

(defn deactivate-node! [ui-state n]
  (gevents/removeAll n)                     ; remove events
  (gdata/remove n "behaviourActive")
  (co-su/remove-update-paths! ui-state n)   ; remove updates                 
  )


; TODO: rendered-node and correct data-path??

(defn activate! [application nodes context]
  (doseq [n (useq/ensure-sequential nodes)
          component (udfind/by-class-inclusive "template" n)]
    (when-not (gdata/get component "behaviourActive")
      (apply-behaviour! application component))))

(defn deactivate! [application nodes]
  (let [ui-state (:ui-state application)]
    (doseq [n (useq/ensure-sequential nodes)]
      (udom/doto-node-and-children n (partial deactivate-node! ui-state)))))
