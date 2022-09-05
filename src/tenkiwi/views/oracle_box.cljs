(ns tenkiwi.views.oracle-box
  (:require [tenkiwi.views.shared :as ui]
            [reagent.core :as r :refer [atom]]
            [react-native-reanimated :as Reanimated]
            [react :as React]))

(def Reanimated (js/require "react-native-reanimated"))
(def animated-view (r/adapt-react-class (.-View (.-default Reanimated))))
(def animated-value (.-Value (.-default Reanimated)))
(def use-animated-style (.-useAnimatedStyle Reanimated))
(def use-shared-value (.-useSharedValue Reanimated))
(def on-js (.-runOnJS Reanimated))
(def on-ui (.-runOnUI Reanimated))
(def with-timing (.-withTiming Reanimated))
(def easing (.-Easing (.-default Reanimated)))

(defn use-animations [animations]
  ;; NOTE: There was some kebab case fixes here
  ;; but we could just deal with camels
  (use-animated-style (js/workletHack (clj->js animations))))

(defn fade-in-text [shared-val]
  (fn [text]
    (let [style      (use-animations {:opacity shared-val})]
      (r/as-element
       [animated-view {:style [style]} (.-children text)]))))

(defn -with-log [{:keys [id key]
                  :as   props}
                 {:keys [actions log current description title]}
                  shared-val
                   dispatch]
  (let [{current-label :label current-text :text
         :or           {current-label ""
                        current-text  "(results show here)"}} current]
    [ui/view {:style {:margin-bottom 8}
              :key   (str "oracle-" id)}
     [ui/card {}
      [ui/card-title {:title title}]
      [ui/card-content {}
       [ui/markdown {} description]]
      [ui/card-actions
       (map-indexed (fn [id {:keys [action params text]}]
                      (let [take-action #(dispatch [:<-game/action! action params])]
                       [ui/button
                        {:key      (str action "-" id)
                         :style    {:flex 1}
                         :on-press (fn []
                                     (set! (.-value shared-val)
                                           (js/timerHack 0
                                                         (js-obj "duration" 500)
                                                         take-action)
                                           ))}
                        text]))
                    actions)]
      [ui/card-content {}
       [ui/surface {:elevation 4 :style {:padding 12}}
        [:>
         (fade-in-text shared-val)
         (if current-label
           [ui/h2 {} current-label])
         (if current-text
           [ui/markdown {} current-text])]
        #_[ui/markdown {} current-text]]]]
     [ui/list-accordion
      {:title "Log"}
      (map-indexed (fn [id {:keys [label text]}]
                     [ui/list-item
                      {:key         (str "log-" id)
                       :title       label
                       :description text}])
                   log)]]))

;; TODO - this could definitely be better. Will it work in prod build? Web?
(defn box-with-animation [props details dispatch]
  [:>
   (fn wrapped-box []
     (let [shared-val (use-shared-value 1)]
       (r/as-element
        (-with-log props details shared-val dispatch))))])
