(ns tenkiwi.components.ui
  (:require [com.stuartsierra.component :as component]
            [tenkiwi.core :refer [init]]))

(defrecord UIComponent []
  component/Lifecycle
  (start [component]
    (init)
    component)
  (stop [component]
    component))

(defn new-ui-component []
  (map->UIComponent {}))
