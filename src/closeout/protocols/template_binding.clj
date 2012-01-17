(ns closeout.protocols.template-binding
  (:refer-clojure :exclude [name])
  )

;; binding applies to the mirror-state nodes 
;; need better terminology

(defprotocol TemplateBinding
  (name           [this]       "get the name")
  (name!          [this value] "set the name")
  (bound-path     [this parent-data-path] 
                  "returns the subpath that this template binds, based on the
                    parent-data-path")
  (placeholder?      [this] "true if this is a placeholder node")
  (find-placeholders [this] "find all placeholders within this node")
  (render            [placeholder-node application] "render a placeholder node")
  
  (find-templates    [this] "find all instanciated templates within this node")
  
  (activate! [this application] 
            "activate the behaviour in the context of this application")
  (deactivate! [this application] 
            "deactivate the behaviour in the context of this application")
  (updated! [old-node new-node application] 
            "ensures safe propagation of behaviour if replacing 'nodes'")
  )
