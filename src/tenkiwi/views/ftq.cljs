(ns tenkiwi.views.ftq
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [oops.core :refer [oget]]
            [tenkiwi.views.shared :as ui]))

(defn -other-panel [{:as display
                     :keys [dispatch]}]
  [ui/scroll-view
   #_[:img {:src (str (:text queen))}]
   (map (fn [{conf :confirm
              :keys [action class text]}]
          (with-meta
            (vector ui/button
                    {:mode "outlined"
                     :on-press (fn []
                                 (ui/maybe-confirm! conf
                                                 #(dispatch [:<-game/action! action])))} text)
            {:key action}))
        (get-in display [:extra-actions]))])

(defn -main-panel [display]
  [ui/scroll-view
   [ui/actions-list display]
   [ui/bottom-sheet-card display]]
  )

(defn -ftq-game-panel [user-data dispatch]
  (let [tab-state (r/atom 0)
        dimensions (.get ui/dimensions "window")]
    (fn [user-data dispatch]
      (let [{user-id        :id
             :as            data
             {:as   room
              :keys [game]} :current-room} @user-data
            active?                        (= user-id (:id (:active-player game)))
            queen                          (:queen game)
            {:keys [actions card]
             :as display}                        (if active?
                                                   (:active-display game)
                                                   (:inactive-display game))
            x-carded?                      (:x-card-active? display)

            display (assoc display :dispatch dispatch)
            on-tab-change (fn [x] (reset! tab-state x))
            current-index @tab-state
            sizing (if ui/web?
                     {:min-height (.-height dimensions)
                      :width "100%"}
                     {:min-height (.-height dimensions)
                      :width (.-width dimensions)})
            ]
        [ui/view {:style sizing}
         [ui/tab-view
          {:initial-layout (if-not ui/web? sizing)
           :scroll-enabled true
           :on-index-change on-tab-change
           ;;:scene-container-style {:background-color "red"}
           :navigation-state {:index current-index
                              :routes [{:key "main"
                                        :title " "}
                                       {:key "other"
                                        :title " "}]}
           :render-scene (fn [props]
                           (let [key (aget (aget props "route") "key")]
                             (case key
                               "main"
                               (r/as-element [-main-panel display])
                               "other"
                               (r/as-element [-other-panel display])
                               (r/as-element [ui/text "WHAAAA"])
                               )
                             ))}

          ]]))))
