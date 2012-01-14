(ns closeout.protocols.template-binding
  (:refer-clojure :exclude [name])
  )

(defprotocol TemplateBinding
  (name  [this]       "get the name")
  (name! [this value] "set the name")
  (bound-path     [this parent-data-path] 
                  "returns the subpath that this template binds, based on the
                    parent-data-path") 
  )
