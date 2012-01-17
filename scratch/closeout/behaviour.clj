(ns closeout.protocols.behaviour)

(defprotocol Behaviour
  (activate! [this application] 
             "activate the behaviour in the context of this application")
  (deactivate! [this application] 
               "deactivate the behaviour in the context of this application")
  (updated! [old-node new-node application] 
            "ensures safe propagation of behaviour if replacing 'nodes'")
  )
