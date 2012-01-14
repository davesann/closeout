(ns closeout.protocols.dom-template
  (:refer-clojure :exclude [name])
  
  (:require 
    [dsann.utils.x.core :as u]
    
    [goog.dom.dataset :as gdata]
    [goog.dom.classes :as gcls]
    
    [closeout.protocols.template-binding :as t]
    )
  )

(extend-protocol t/TemplateBinding
  js/Node
  (name [n]   (keyword (gdata/get n "templateName")))
  (name! [n v] 
         (do 
           (gcls/add  n "template")
           (gdata/set n "templateName" (clojure.core/name v))))
  (bound-path [n parent-data-path] 
              (if-let [t-bind (gdata/get n "templateBindKw")]
                (conj parent-data-path (keyword t-bind))
                (if-let [t-bind (gdata/get n "templateBindInt")]
                  (conj parent-data-path (js/parseInt t-bind 10))
                  (if-let [t-bind (gdata/get n "templateBindStr")]
                    (conj parent-data-path t-bind)
                    (if-let [t-bind (gdata/get n "templateBindSeq")]
                      (apply (partial conj parent-data-path) (reader/read t-bind))
                      parent-data-path)))))
  
  ;js/Object
  ;(template-name  [n]   (do 
  ;                        ;(u/log n) 
  ;                        throw "Error"))
  ;(template-name! [n v] (do (u/log n) throw "Error"))
  ;(bound-path     [n parent-data-path] (do (u/log [n parent-data-path]) throw "Error")) 
  
  )