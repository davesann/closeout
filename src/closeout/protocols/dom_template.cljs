(ns closeout.protocols.dom-template
  (:require 
    [closeout.protocols.template-binding :as t]
    )
  )

(extend-protocol t/TemplateBinding
  js/Node
  (name  [n] (keyword (gdata/get n "templateName"))
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
  ))