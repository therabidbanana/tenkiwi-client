(ns tenkiwi.games.push
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [oops.core :refer [oget oset!]]
            [goog.object]
            [tenkiwi.views.stress-scoreboard :as stress]
            [tenkiwi.views.oracle-box :as oracle-box]
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
 :push-other
 (fn [db]
   (extract-display (:user db)
                    [:episode])))

(defn build-other-panel [game-state-atom dispatch]
  (fn -other-panel []
    (let [{:keys [episode display]} @game-state-atom
          dimensions        (.get ui/dimensions "screen")
          ]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       [ui/card {:style {:margin  8}}
        [ui/card-content
         [ui/h1 {} (str "Episode Details")]
         [ui/markdown {} (str (:text episode))]
         [ui/actions-list (merge display {:mode "outlined"
                                          :dispatch dispatch
                                          :from :extra-actions})]]]

       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]])))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:push-other])]
    (build-other-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :push-dice
 (fn [db]
   (extract-display (:user db) [:stage])))

(defn build-dice-panel [game-state-atom dispatch]
  (fn -dice-panel []
    (let [{:keys [stage 
                  current-user-id
                  ]
           {:as display
            :keys [player-sheets
                   oracle-box
                   active-player
                   extra-details]} :display
           :as game-state} @game-state-atom
          active-player  (merge active-player (get player-sheets (:id active-player) {}))
          dimensions (.get ui/dimensions "screen")]
      [ui/collapse-scroll-view {:collapse! do-collapse!
                                :style {:padding 12}}
       [ui/view
        (map (fn [[id box]]
               [oracle-box/box-with-animation {:id id :key (str "oracle-" id)} box dispatch])
             oracle-box)
        [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
         [ui/text ""]]]])))

(defn dice-panel []
  (let [game-state (re-frame/subscribe [:push-dice])]
    (build-dice-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :push-main
 (fn [db]
   (extract-display (:user db) [:stage])))

(defn build-main-panel [game-state-atom dispatch]
  (fn -main-panel []
    (let [{:keys [stage 
                  current-user-id
                  ]
           {:as display
            :keys [player-sheets
                   dice-bag
                   card
                   active-player
                   extra-details]} :display
           :as game-state} @game-state-atom
          active-player  (merge active-player (get player-sheets (:id active-player) {}))
          dimensions (.get ui/dimensions "screen")
          display    (cond-> display
                       (get-in card [:tags :challenge])
                       (assoc-in [:border-color] "orange"))]
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
  (let [game-state (re-frame/subscribe [:push-main])]
    (build-main-panel game-state re-frame/dispatch)))


(re-frame/reg-sub
 :push-intro
 (fn [db]
   (extract-display (:user db) [:stage :episode])))

(defn build-intro-panel [game-state-atom dispatch]
  (fn -intro-panel []
    (let [{:keys [stage
                  current-user-id
                  episode]
           {:as display
            :keys [player-sheets
                   dice-bag
                   card
                   active-player
                   extra-details]} :display
           :as game-state} @game-state-atom
          active-player  (merge active-player (get player-sheets (:id active-player) {}))
          dimensions (.get ui/dimensions "screen")]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       [ui/card {:style {:margin 12}}
        [ui/card-content
         [ui/markdown {}
          (str (:text episode))]]
        #_[wordbank/-wordbank {} extra-details]]
       [ui/bottom-sheet-fixed
        (assoc display
               :collapse! do-collapse!
               :dispatch dispatch
               :regen-action true)]
       [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
        [ui/text ""]]])))

(defn intro-panel []
  (let [game-state (re-frame/subscribe [:push-intro])]
    (build-intro-panel game-state re-frame/dispatch)))

(defn push-game-panel []
  (let [tab-state (r/atom 0)
        dimensions (.get ui/dimensions "screen")
        game-state (re-frame/subscribe [:push-main])
        scene-map (ui/SceneMap (clj->js {:main (r/reactify-component main-panel)
                                         :dice (r/reactify-component dice-panel)
                                         :other (r/reactify-component other-panel)}))]
    (fn []
      (let [dimensions (.get ui/dimensions "screen")
            on-tab-change (fn [x] (reset! tab-state x))
            current-index @tab-state
            stage (get-in @game-state [:display :stage])]
        (cond
          (#{:intro} stage)
          [intro-panel]
          #_[ui/clean-tab-view
           {:dimensions dimensions
            :scroll-enabled true
            :on-index-change on-tab-change
            :navigation-state {:index current-index
                               :routes [{:key "main"
                                         :title "Main"
                                         :icon "play-circle-outline"}
                                        {:key "other"
                                         :title "Extras"
                                         :icon "more-horiz"}]}
            :render-scene scene-map}]
          :else
          [ui/clean-tab-view
           {:dimensions dimensions
            :scroll-enabled true
            :on-index-change on-tab-change
            :navigation-state {:index current-index
                               :routes [{:key "main"
                                         :title "Main"
                                         :icon "play-circle-outline"}
                                        {:key "dice"
                                         :title "Oracles"
                                         :icon "bar-chart"}
                                        {:key "other"
                                         :title "Extras"
                                         :icon "more-horiz"}]}
            :render-scene scene-map}]

          )))))
