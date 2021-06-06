(ns tenkiwi.views.debrief
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [tenkiwi.views.shared :as ui]))

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
                    [])))

(defn build-other-panel [game-state-atom dispatch]
  (fn -other-panel []
    (let [{:keys [extra-actions]} (:display @game-state-atom)]
      [ui/view
      (map (fn [{conf  :confirm
                 :keys [action class text]}]
             (with-meta (vector ui/button
                                {:class class
                                 :on-press (fn [] (ui/maybe-confirm! conf #(dispatch [:<-game/action! action])))}
                                text)
               {:key action}))
           extra-actions)])))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:debrief-other])]
    (build-other-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :debrief-scoreboard
 (fn [db]
   (extract-display (:user db)
                    [:dossiers :all-players
                     :stage :player-scores
                     :company :mission])))

(defn -player-scoreboard-entry [display current-user-id player-scores player]
  (let [{:keys [id user-name dead? agent-name agent-codename agent-role]} player
        dispatch (:dispatch display)
        total-score (apply + (vals (player-scores id)))]
    [ui/card {:style {:margin 8}}
     [ui/card-content
      [ui/para {:title agent-name}
       (str "[ " total-score " ] " (if agent-name (str agent-codename ", " agent-role " ")) " (" user-name ")")]]
     [ui/card-actions {}
      ;; TODO - maybe this logic should come from gamemaster
      (if-not (= id current-user-id)
        [ui/button
         {:style {:flex 1}
          :on-press #(dispatch [:<-game/action! :downvote-player {:player-id id}])}
         " - "])
      [ui/text {:style {:flex 1}}
       (str (get-in player-scores [id current-user-id]))]
      (if-not (= id current-user-id)
        [ui/button
         {:style {:flex 1}
          :on-press #(dispatch [:<-game/action! :upvote-player {:player-id id}])}
         " + "])
      ]]))

(defn build-scoreboard-panel [game-state-atom dispatch]
  (fn -scoreboard-panel []
    (let [{:keys [dossiers all-players
                  stage player-scores
                  current-user-id
                  company mission display]
           :as game} @game-state-atom
          all-players    (map #(merge % (get dossiers (:id %) {}))
                              all-players)

          voting-active? (if-not (#{:intro} stage)
                           true
                           false)
          box-style {:margin-top 8 :padding 10}
          dimensions (.get ui/dimensions "screen")]

      [ui/scroll-view
       [ui/view 
        (if voting-active?
          (map (fn [player]
                 (with-meta
                   [-player-scoreboard-entry (assoc display :dispatch dispatch)
                      current-user-id player-scores player]
                   {:key (:id player)}))
               all-players))]
       [ui/surface {:style box-style}
        [ui/h1 "Round Themes"]
        [ui/view
         (map
          (fn [val] (with-meta [ui/para val] {:key val}))
          (:values company))]]
       (if voting-active?
         [ui/surface {:style box-style}
          [ui/h1 "More Details"]
          [ui/markdown (str (:text mission))]])
       [ui/view {:height (* 0.2 (.-height dimensions))}
        [ui/text ""]]
       ])))


(defn scoreboard-panel []
  (let [game-state (re-frame/subscribe [:debrief-scoreboard])]
    (build-scoreboard-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :debrief-main
 (fn [db]
   (extract-display (:user db) [:stage :stage-name :stage-focus])))

(defn build-main-panel [game-state-atom dispatch]
  (fn -main-panel []
    (let [{:keys [stage stage-name
                  stage-focus]
           {:as display
            :keys [extra-details]} :display
           :as game-state} @game-state-atom
          voting-active? (if-not (#{:intro} stage)
                           true
                           false)
          box-style {:margin-top 8 :padding 10}
          dimensions (.get ui/dimensions "screen")]
        [ui/scroll-view
         [ui/view
          [ui/view
           [ui/para (str stage-name)]
           [ui/para (str stage-focus)]]
          [ui/card-with-button (assoc display :dispatch dispatch)]
          [ui/bottom-sheet-fixed (assoc display :dispatch dispatch)]
          (if (and voting-active? extra-details)
            [ui/view
             (map (fn [{:keys [title items]}]
                    (with-meta
                      [ui/surface {:style box-style}
                       [ui/h1 title]
                       [ui/view
                        (map #(with-meta [ui/para %] {:key %}) items)]]
                      {:key title}))
                  extra-details
                  )])
          [ui/view {:height (* 0.2 (.-height dimensions))}
           [ui/text ""]]
          ]])))

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
           ;; valid-button? (fn [{:keys                 [action params disabled?]
           ;;                     {:keys [id rank act]} :params
           ;;                     :as                   button}]
           ;;                 (cond
           ;;                   (#{:rank-player} action)
           ;;                   (and
           ;;                    (not= user-id id)
           ;;                    (nil? (get-in player-ranks [user-id act rank]))
           ;;                    (not= id (get-in player-ranks [user-id act :best])))
           ;;                   :else
           ;;                   (not disabled?)))
           ;; "window" dimensions wrong to start sometimes - height 36?
           ;;  note: useWindowDimensions hook did _not_ prevent this problem
           ;;  most likely reagent deferring a render and causing window to be small?
           dimensions (.get ui/dimensions "screen")
           on-tab-change (fn [x] (reset! tab-state x))
           current-index @tab-state
           sizing {:min-height (.-height dimensions)
                   :width (.-width dimensions)}]
       [ui/view {:style sizing}
        [ui/tab-view
         {:initial-layout (assoc sizing :height (.-height dimensions))
          :scroll-enabled true
          :on-index-change on-tab-change
          ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
          :navigation-state {:index current-index
                             :routes [{:key "main"
                                       :title " "}
                                      {:key "scoreboard"
                                       :title " "}
                                      {:key "other"
                                       :title " "}]}
          :render-scene scene-map}
         ]
        ]))))
