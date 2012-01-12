(ns closeout.protocols.template)

(defprotocol Template
  (id    [this] "return an identifier")
  (binds [this] "returns the subpath that this template binds") 
  )
