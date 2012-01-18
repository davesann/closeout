(ns closeout.state.read-notifier)

(def ^:dynamic *read-notifier* identity)

(defn tget-in 
  ([a-map path]
    (do 
      (*read-notifier* path)
      (get-in a-map path)))
  ([a-map path not-found]
    (do 
      (*read-notifier* path)
      (get-in a-map path not-found))))
