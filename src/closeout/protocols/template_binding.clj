(ns closeout.protocols.template-binding)

(defprotocol TemplateBinding
  (id    [this] "return an identifier")
  (binds [this parent-data-path] 
         "returns the subpath that this template binds, based on the
          parent-data-path") 
  )
