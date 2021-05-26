(ns your-project.components.ui
  (:require [com.stuartsierra.component :as component]
            [your-project.core :refer [init]]))

(defrecord UIComponent []
  component/Lifecycle
  (start [component]
    (init)
    component)
  (stop [component]
    component))

(defn new-ui-component []
  (map->UIComponent {}))
