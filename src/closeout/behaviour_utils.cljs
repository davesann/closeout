(ns closeout.behaviour-utils
  (:require 
    [dsann.utils.x.core :as u]
    [dsann.utils.seq :as useq]
    [dsann.utils.state.mirror :as usm]
    
    [dsann.cljs-utils.dom.dom    :as udom]
    [dsann.cljs-utils.dom.find   :as udfind]
    [dsann.cljs-utils.dom.data   :as udd]
    
    [closeout.protocols.template-binding :as ptb]

    [goog.dom.dataset :as gdata]
    [goog.events :as gevents]
    )
  (:require-macros
    [dsann.utils.macros.ctrl :as uctrl]
    )
  )

(defn apply-behaviour! [application ui-element]
  (if-let [t-name (ptb/name ui-element)]
    (let [templates  (:ui-templates application)]
      (when-let [behaviour! (:behaviour-fn! (templates t-name))]
        (behaviour! application ui-element)
        (gdata/set ui-element "behaviourActive" "true")))))

(defn deactivate-node! [ui-state n]
  (gevents/removeAll n)                   ; remove events
  (gdata/remove n "behaviourActive")
  (usm/remove-update-paths! ui-state n)   ; remove updates                 
  )


(defn activate! [application nodes context]
  (doseq [n (useq/ensure-sequential nodes)
          component (udfind/by-class-inclusive "template" n)]
    (when-not (gdata/get component "behaviourActive")
      (apply-behaviour! application component))))

(defn deactivate! [application nodes]
  (let [ui-state (:ui-state application)]
    (doseq [n (useq/ensure-sequential nodes)]
      (udom/doto-node-and-children n (partial deactivate-node! ui-state)))))
