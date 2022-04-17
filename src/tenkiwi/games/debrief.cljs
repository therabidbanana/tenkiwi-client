(ns tenkiwi.games.debrief
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [oops.core :refer [oget oset!]]
            [goog.object]
            [tenkiwi.views.shared :as ui]
            [tenkiwi.views.wordbank :as wordbank]
            ))

(defonce do-collapse! (r/atom (fn [])))

(defn- extract-display [game-state key-list]
  (let [{user-id        :id
         :as            data
         {:as   room
          :keys [game]} :current-room} game-state
        active? (= user-id (get-in game [:active-player :id]))
        display (if active?
                  (:active-display game)
                  (:inactive-display game))]
    (merge {:current-user-id user-id}
           (select-keys game key-list)
           {:display display})))

(re-frame/reg-sub
 :debrief-other
 (fn [db]
   (extract-display (:user db)
                    [:stage :company :mission])))

(defn build-other-panel [game-state-atom dispatch]
  (fn -other-panel []
    (let [{:keys [extra-actions]}                 (:display @game-state-atom)
          {:keys [display company stage mission]} @game-state-atom
          dimensions                              (.get ui/dimensions "screen")
          voting-active?                          (if-not (#{:intro} stage)
                           true
                           false)]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       [ui/card {:style {:margin 8}}
        [ui/card-content
         [ui/h1 "Round Themes"]
         [ui/view
          (map
           (fn [val] (with-meta [ui/para val] {:key val}))
           (:values company))]]]
       (if voting-active?
         [ui/card {:style {:margin 8}}
          [ui/card-content
           [ui/h1 "More Details"]
           [ui/markdown (str (:text mission))]]])
       [ui/card {:style {:padding 4
                         :margin  8}}
        [ui/actions-list (merge display {:mode     "outlined"
                                         :dispatch dispatch
                                         :from     :extra-actions})]]
       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]])))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:debrief-other])]
    (build-other-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :debrief-scoreboard
 (fn [db]
   (extract-display (:user db)
                    [:dossiers :all-players
                     :stage :player-scores])))

(defn -player-scoreboard-entry [display current-user-id player-scores player]
  (let [{:keys [id user-name dead? agent-name agent-codename agent-role]} player
        player-scores (or player-scores {})
        current-user? (= id current-user-id)
        dispatch (:dispatch display)
        total-score (apply + (vals (player-scores id)))]
    [ui/surface {:style {:margin 4
                         :flex-direction "row"
                         :align-items "center"}}
     [ui/view {:style {:flex 1
                       :padding 4
                       :align-items "center"}}
      [ui/h1 {} total-score]]
     [ui/view {:style {:flex 7
                       :padding 4}}
      [ui/view {:style {:border-bottom-style "dashed"
                        :border-bottom-color "#bebebe"
                        :border-bottom-width 1
                        :padding 4}}
       [ui/para {:title agent-name}
        (str (if agent-name (str agent-codename ", " agent-role " ")) " (" user-name ")")]]
      [ui/view {:style {:flex-direction "row"
                        :align-items "center"}}
       ;; TODO - maybe this logic should come from gamemaster
       (if-not current-user?
         [ui/button
          {:style {:flex 1}
           :on-press #(dispatch [:<-game/action! :downvote-player {:player-id id}])}
          " - "])
       [ui/text {:style {:text-align "center"
                         :margin-top 9
                         :margin-bottom 9
                         :font-style "italic"
                         :opacity (if current-user? 0.4 0.7)
                         :flex 1}}
        (if current-user?
          "This is You"
          (str (get-in player-scores [id current-user-id])))]
       (if-not current-user?
         [ui/button
          {:style {:flex 1}
           :on-press #(dispatch [:<-game/action! :upvote-player {:player-id id}])}
          " + "])]]]))

(defn build-scoreboard-panel [game-state-atom dispatch]
  (fn -scoreboard-panel []
    (let [{:keys [dossiers all-players
                  stage player-scores
                  current-user-id
                  display]
           :as game} @game-state-atom
          all-players    (map #(merge % (get dossiers (:id %) {}))
                              all-players)

          voting-active? (if-not (#{:intro} stage)
                           true
                           false)
          box-style {:margin-top 8 :padding 10}
          dimensions (.get ui/dimensions "screen")]

      [ui/collapse-scroll-view {:collapse! do-collapse!
                                :style {:padding 12}}
       [ui/h1
        {:theme {:colors {:text "white"}}
         :style {:padding-top 4
                 :padding-left 4}}
        "Scoreboard"]
       [ui/view
        ;; Testing always shown scoreboard
        (if (or voting-active? true)
          (map (fn [player]
                 (with-meta
                   [-player-scoreboard-entry (assoc display :dispatch dispatch)
                    current-user-id player-scores player]
                   {:key (:id player)}))
               all-players)
          [ui/para
           {:theme {:colors {:text "white"}}
            :style {:padding-top 4
                    :padding-left 4
                    :font-style "italic"}}
           "Voting will start after introductions"])]
       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]])))

(defn scoreboard-panel []
  (let [game-state (re-frame/subscribe [:debrief-scoreboard])]
    (build-scoreboard-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :debrief-main
 (fn [db]
   (extract-display (:user db)
                    [:stage :stage-name :stage-focus
                     :dossiers
                     :active-player :player-ranks])))

(defn build-main-panel [game-state-atom dispatch]
  (fn -main-panel []
    (let [{:keys [stage stage-name
                  stage-focus company
                  dossiers
                  active-player
                  current-user-id
                  player-ranks]
           {:as display
            :keys [extra-details]} :display
           :as game-state} @game-state-atom
          voting-active? (if-not (#{:intro} stage)
                           true
                           false)
          active-player  (merge active-player (get dossiers (:id active-player) {}))
          box-style {:margin-top 8 :padding 10}
          dimensions (.get ui/dimensions "screen")

          valid-button? (fn [{:keys                 [action params disabled?]
                              {:keys [id rank act]} :params
                              :as                   button}]
                          (cond
                            (#{:rank-player} action)
                            (and
                             (not= current-user-id id)
                             (nil? (get-in player-ranks [current-user-id act rank]))
                             (not= id (get-in player-ranks [current-user-id act :best])))
                            :else
                            (not disabled?)))]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       [ui/view
        [ui/view
         [ui/para {:theme {:colors {:text "white"}}
                   :style {:padding-top 4
                           :padding-left 8}}
          (str stage-name "\n" stage-focus)]]
        #_[ui/card-with-button (assoc display :dispatch dispatch)]
        [ui/actions-list (assoc display
                                :dispatch dispatch
                                :hide-invalid? true
                                :action-valid? valid-button?)]
        [wordbank/-wordbank {} extra-details]
        [ui/bottom-sheet-card
         (assoc display
                :collapse! do-collapse!
                :dispatch dispatch
                :regen-action true
                :turn-marker (str
                              (if-let [agent-name (:agent-codename active-player)]
                                (str
                                 agent-name " "
                                 "(" (:user-name active-player) ")")
                                (:user-name active-player)) "'s turn..."))]
        [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
         [ui/text ""]]]])))

(defn main-panel []
  (let [game-state (re-frame/subscribe [:debrief-main])]
    (build-main-panel game-state re-frame/dispatch)))

(defn debrief-game-panel []
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
