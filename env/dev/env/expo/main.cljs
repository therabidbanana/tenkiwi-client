(ns env.expo.main
  (:require [reagent.core :as r]
            [tenkiwi.system :as core]
            [tenkiwi.core :refer [app-root]]
            #_[figwheel.client :as figwheel :include-macros true]
            #_[env.dev]))

(defn figwheel-rn-root []
  (r/as-element [app-root]))

(core/run)
