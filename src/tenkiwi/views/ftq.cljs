(ns tenkiwi.views.ftq
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [oops.core :refer [oget]]
            [tenkiwi.views.shared :as ui]))

#_(defn -image-panel [{:as display
                     :keys [dispatch]}]
  [ui/scroll-view
   [:img {:src (str (:text queen))}]
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

(defn -other-panel [{:as display
                     :keys [queen dispatch]}]
  [ui/scroll-view
   [ui/view {:style {:height 250
                     :align-items "center"
                     :flex-direction "row"}}
    [ui/button {:style {:flex 1}
                :content-style {:height 250}
                :on-press #(dispatch [:<-game/action! :previous-queen {}])}
     "<"]
    [ui/image {:style {:flex 8
                       :width "100%"
                       :height "100%"}
               :resize-mode "contain"
               :source {:uri (str (:text queen))}}]

    [ui/button {:style {:flex 1}
                :content-style {:height 250}
                :on-press #(dispatch [:<-game/action! :next-queen {}])}
     ">"]
    ]
   [ui/card {:margin 8
             :margin-top 12}
    [ui/card-content
     (map (fn [{conf :confirm
                :keys [action class text]}]
            (with-meta
              (vector ui/button
                      {:mode "outlined"
                       :on-press (fn []
                                   (ui/maybe-confirm! conf
                                                      #(dispatch [:<-game/action! action])))} text)
              {:key action}))
          (remove #(#{:next-queen :previous-queen} (:action %))
                  (get-in display [:extra-actions])))]]
   [ui/view {:style {:height (* 0.7 (.-height (.get ui/dimensions "screen")))}}
    [ui/text ""]]])

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

            display (assoc display
                           :dispatch dispatch
                           :queen queen)
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
