(ns tenkiwi.games.threads
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [oops.core :refer [oget oset!]]
            [goog.object]
            [tenkiwi.views.stress-scoreboard :as stress]
            [tenkiwi.views.wordbank :as wordbank]
            [tenkiwi.views.shared :as ui]))

(defonce do-collapse! (r/atom (fn [])))

(defn- extract-display [game-state key-list]
  (let [{user-id        :id
         :as            data
         {:as   room
          :keys [game]} :current-room} game-state
        active? (= user-id (get-in game [:display :active-player :id]))
        display (if active?
                  (:active-display game)
                  (:inactive-display game))]
    (merge {:current-user-id user-id}
           (select-keys game key-list)
           {:display (merge display (:display game))})))

(re-frame/reg-sub
 :threads-other
 (fn [db]
   (extract-display (:user db)
                    [])))

(defn build-other-panel [game-state-atom dispatch]
  (fn -other-panel []
    (let [{:keys [display]} @game-state-atom
          dimensions        (.get ui/dimensions "screen")
          ]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       [ui/card {:style {:padding 4
                         :margin  8}}
        [ui/actions-list (merge display {:mode "outlined"
                                         :dispatch dispatch
                                         :from :extra-actions})]]

       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]])))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:threads-other])]
    (build-other-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :threads-scoreboard
 (fn [db]
   (extract-display (:user db)
                    [:players])))

(defn build-scoreboard-panel [game-state-atom dispatch]
  (fn -scoreboard-panel []
    (let [{:keys [players
                  current-user-id
                  display]
           :as game} @game-state-atom

          {:keys [player-scoreboard]} display

          dimensions (.get ui/dimensions "screen")]

      [ui/collapse-scroll-view {:collapse! do-collapse!
                                :style {:padding 12}}
       [stress/-scoreboard {:title "Scoreboard"} player-scoreboard dispatch]
       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]])))

(defn scoreboard-panel []
  (let [game-state (re-frame/subscribe [:threads-scoreboard])]
    (build-scoreboard-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :threads-main
 (fn [db]
   (extract-display (:user db) [:stage])))

(defn build-main-panel [game-state-atom dispatch]
  (fn -main-panel []
    (let [{:keys [stage 
                  current-user-id
                  ]
           {:as display
            :keys [player-sheets
                   active-player
                   extra-details]} :display
           :as game-state} @game-state-atom
          active-player  (merge active-player (get player-sheets (:id active-player) {}))
          dimensions (.get ui/dimensions "screen")]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       [ui/view
        #_[ui/view
         [ui/para {:theme {:colors {:text "white"}}
                   :style {:padding-top 4
                           :padding-left 8}}
          (str stage-name "\n" stage-focus)]]
        [ui/actions-list (assoc display
                                :dispatch dispatch
                                :hide-invalid? true)]
        [wordbank/-wordbank {} extra-details]
        [ui/bottom-sheet-card
         (assoc display
                :collapse! do-collapse!
                :dispatch dispatch
                :regen-action true)]
        [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
         [ui/text ""]]]])))

(defn main-panel []
  (let [game-state (re-frame/subscribe [:threads-main])]
    (build-main-panel game-state re-frame/dispatch)))

(defn threads-game-panel []
  (let [tab-state (r/atom 0)
        dimensions (.get ui/dimensions "screen")
        scene-map (ui/SceneMap (clj->js {:main (r/reactify-component main-panel)
                                         :scoreboard (r/reactify-component scoreboard-panel)
                                         :other (r/reactify-component other-panel)}))]
    (fn []
      (let [;; ;; TODO - hide self-vote or push to server
           ;; self-vote?    (fn [{:keys                 [action params]
           ;;                     {:keys [id rank act]} :params
           ;;                     :as                   button}]
           ;;                 (and (#{:rank-player} action)
           ;;                      (= user-id id)))
           ;; ;; Figure out where to place this
           ;; "window" dimensions wrong to start sometimes - height 36?
           ;;  note: useWindowDimensions hook did _not_ prevent this problem
           ;;  most likely reagent deferring a render and causing window to be small?
            dimensions (.get ui/dimensions "screen")
            on-tab-change (fn [x] (reset! tab-state x))
            current-index @tab-state
            sizing (if (ui/os? "web")
                     {:min-height (.-height dimensions)
                      :width "100%"}
                     {:min-height (.-height dimensions)
                      :width (.-width dimensions)})
            tab-style {:minHeight 24
                       :padding 6
                       :paddingBottom 9}
            bar-style {:backgroundColor "rgba(0,0,0,0.3)"}
            indicator-style {:borderRadius 2
                             :backgroundColor "rgba(255,255,255,0.15)"
                             :height 4
                             :bottom 3}]
        [ui/view {:style sizing}
         [ui/tab-view
          {:initial-layout (if-not (ui/os? "web")
                             (assoc sizing :height (.-height dimensions)))
           :scroll-enabled true
           :on-index-change on-tab-change
           :render-tab-bar (fn [props]
                             (let [_ (goog.object/set props "tabStyle" (clj->js tab-style))
                                   _ (goog.object/set props "indicatorStyle" (clj->js indicator-style))
                                   _ (goog.object/set props "style" (clj->js bar-style))
                                  ;; Disable uppercase transform
                                  ;; _ (goog.object/set props "getLabelText" (fn [scene] (aget (aget scene "route") "title")))
                                   ]
                               (r/as-element [ui/tab-bar (js->clj props)])))
          ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
           :navigation-state {:index current-index
                              :routes [{:key "main"
                                        :title "Main"
                                        :icon "play-circle-outline"}
                                       {:key "scoreboard"
                                        :title "Scores"
                                        :icon "bar-chart"}
                                       {:key "other"
                                        :title "Extras"
                                        :icon "more-horiz"}]}
           :render-scene scene-map}]]))))
