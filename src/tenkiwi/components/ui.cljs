(ns tenkiwi.components.ui
  (:require [com.stuartsierra.component :as component]))

(defrecord UIComponent []
  component/Lifecycle
  (start [{:keys [ui-boot]
           :as component}]
    (ui-boot)
    component)
  (stop [component]
    component))

(defn new-ui-component []
  (map->UIComponent {}))
