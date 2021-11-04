(ns tenkiwi.views.opera
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [oops.core :refer [oget oset!]]
            [goog.object]
            [tenkiwi.views.shared :as ui]))

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
 :opera-other
 (fn [db]
   (extract-display (:user db)
                    [:all-players :dossiers :stage :mission])))

(defn -player-scoreboard-entry [display current-user-id dossiers player]
  (let [{:keys [id user-name dead? background codename department]} player
        current-user? (= id current-user-id)
        dispatch (:dispatch display)
        total-score (get-in dossiers [id :stress])]
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
       [ui/para {:title background}
        (str (if codename (str codename ", " department " ")) " (" user-name ")")]]
      [ui/view {:style {:flex-direction "row"
                        :align-items "center"}}
       [ui/button
        {:style {:flex 1}
         :on-press #(dispatch [:<-game/action! :downstress-player {:player-id id}])}
        " - "]
       [ui/text {:style {:text-align "center"
                         :margin-top 9
                         :margin-bottom 9
                         :font-style "italic"
                         :opacity (if current-user? 0.4 0.7)
                         :flex 1}}
        (str (get-in dossiers [id :stress]))]
       [ui/button
        {:style {:flex 1}
         :on-press #(dispatch [:<-game/action! :upstress-player {:player-id id}])}
        " + "]
       ]]]))

(defn build-other-panel [game-state-atom dispatch]
  (fn -other-panel []
    (let [{:keys [display current-user-id
                  dossiers all-players stage mission]} @game-state-atom

          {:keys [extra-actions]} display
          all-players    (map #(merge % (get dossiers (:id %) {}))
                              all-players)

          extra-actions (remove (fn [x] (#{:jump-ahead} (:action x))) extra-actions)
          dimensions (.get ui/dimensions "screen")
          Voting-active? (if-not (#{:intro} stage)
                           true
                           false)]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       (if voting-active?
         [ui/view
          ;; Testing always shown scoreboard
          (map (fn [player]
                 (with-meta
                   [-player-scoreboard-entry (assoc display :dispatch dispatch)
                    current-user-id dossiers player]
                   {:key (:id player)}))
               all-players)])
       (if voting-active?
         [ui/card {:style {:margin 8}}
          [ui/card-content
           [ui/h1 "More Details"]
           [ui/markdown (str (:text mission))]]])
       [ui/card {:style {:padding 4
                         :margin 8}}
        (map (fn [{conf  :confirm
                   :keys [action class text]}]
               (with-meta (vector ui/button
                                  {:class class
                                   :mode "outlined"
                                   :on-press (fn [] (ui/maybe-confirm! conf #(dispatch [:<-game/action! action])))}
                                  text)
                 {:key action}))
             extra-actions)]

       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]]
      )))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:opera-other])]
    (build-other-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :opera-notes
 (fn [db]
   (extract-display (:user db)
                    [:dossiers :all-players
                     :scenes
                     :stage :player-scores])))

(defn -note-card [title list]
  [ui/card {:style {:margin-top 8}}
   [ui/card-content {}
    [ui/h1 {} title]
    [ui/view {:style {:padding 4}}
     (map (fn [x] [ui/markdown x]) list)]]])

(defn build-notes-panel [game-state-atom dispatch]
  (fn -notes-panel []
    (let [{:keys [dossiers scenes all-players
                  stage player-scores
                  current-user-id
                  display]
           :as   game}            @game-state-atom
          {:keys [extra-actions]} display
          extra-actions (filter (fn [x] (#{:jump-ahead} (:action x))) extra-actions)

          all-players   (map #(merge % (get dossiers (:id %) {}))
                             all-players)
          persons       (->> (map :npc scenes) distinct)
          places        (->> (map :setting scenes) distinct)
          clues         (->> (filter (fn [{:keys [type]}] (#{:clue} type)) scenes)
                             (map :text)
                             distinct)
          complications (->> (filter (fn [{:keys [type]}] (#{:complication} type)) scenes)
                             (map :text)
                             distinct)

          notes-active? (if-not (#{:intro} stage)
                          true
                          false)
          box-style     {:margin-top 8 :padding 10}
          dimensions    (.get ui/dimensions "screen")]

      [ui/collapse-scroll-view {:collapse! do-collapse!
                                :style     {:padding 12}}
       [ui/h1
        {:theme {:colors {:text "white"}}
         :style {:padding-top  4
                 :padding-left 4}}
        "Investigation Notes"]
       [ui/view 
        (if notes-active?
          [ui/view {}
           [-note-card "Clues" clues]
           (if-not (empty? extra-actions)
             [ui/card {:style {:padding 4
                               :margin 8}}
              (map (fn [{conf  :confirm
                         :keys [action class text]}]
                     (with-meta (vector ui/button
                                        {:class class
                                         :mode "outlined"
                                         :on-press (fn [] (ui/maybe-confirm! conf #(dispatch [:<-game/action! action])))}
                                        text)
                       {:key action}))
                   extra-actions)])
           [-note-card "People" persons]
           [-note-card "Complications" complications]
           [-note-card "Places" places]]
          [ui/para
           {:theme {:colors {:text "white"}}
            :style {:padding-top  4
                    :padding-left 4
                    :font-style   "italic"}}
           "Notes will appear as game begins"])]
       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]
       ])))


(defn notes-panel []
  (let [game-state (re-frame/subscribe [:opera-notes])]
    (build-notes-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :opera-main
 (fn [db]
   (extract-display (:user db)
                    [:stage :stage-name :stage-focus
                     :dossiers :clocks :position
                     :active-player :player-ranks])))

(defn build-main-panel [game-state-atom dispatch]
  (fn -main-panel []
    (let [{:keys                        [stage stage-name
                                         stage-focus
                                         clocks
                                         position
                                         dossiers
                                         active-player
                                         current-user-id
                                         player-ranks]
           {:as   display
            :keys [card extra-details]} :display
           :as                          game-state} @game-state-atom

          voting-active? (if-not (#{:intro} stage)
                           true
                           false)
          active-player  (merge active-player (get dossiers (:id active-player) {}))
          box-style      {:margin-top 8 :padding 10}
          dimensions     (.get ui/dimensions "screen")
          has-scene?     (-> card :scene)
          position-color (get {1 "#66bb6a"
                               2 "#fff59d"
                               3 "#fdd835"
                               4 "#ff9800"
                               5 "#ef5350"} position)

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
                            (not disabled?)))
          ]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
         [ui/view
          [ui/view
           [ui/para {:theme {:colors {:text "white"}}
                     :style {:padding-top  4
                             :padding-left 8}}
            (str stage-name #_(str (-> clocks :plot) " / " 8))]
           ]
          #_[ui/card-with-button (assoc display :dispatch dispatch)]
          [ui/actions-list (assoc display
                                  :dispatch dispatch
                                  :hide-invalid? true
                                  :action-valid? valid-button?)]
          (if has-scene?
            [ui/progressbar {:progress (/ (-> clocks :plot) 8)
                             :color position-color}]
            [ui/progressbar {:progress 1.0
                             :color position-color}])
          (if extra-details
            [ui/view {:style {:padding     2
                              :padding-top 8}}
             (map (fn [[{title1 :title items1 :items}
                        {title2 :title items2 :items}]]
                    (with-meta
                      [ui/view {:flex-direction "row"}
                       (if title1
                         [ui/surface {:style (assoc box-style
                                                    :background-color "rgba(255,255,255,0.40)"
                                                    :margin 4
                                                    :flex 1)}
                          [ui/h1 title1]
                          [ui/view
                           (map #(with-meta [ui/markdown %] {:key %}) items1)]])
                       (if title2
                         [ui/surface {:style (assoc box-style
                                                    :background-color "rgba(255,255,255,0.40)"
                                                    :margin 4
                                                    :flex 1)}
                          [ui/h1 title2]
                          [ui/view
                           (map #(with-meta [ui/markdown %] {:key %}) items2)]])]
                      {:key (str title1 title2)}))
                  (partition-all 2 extra-details)
                  )])
          [ui/bottom-sheet-card
           (assoc display
                  :collapse! do-collapse!
                  :dispatch dispatch
                  :regen-action true
                  :turn-marker (str
                                (if-let [agent-name (:codename active-player)]
                                  (str
                                   agent-name " "
                                   "(" (:user-name active-player) ")")
                                  (:user-name active-player)) "'s turn..."))]
          [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
           [ui/text ""]]
          ]])))

(defn main-panel []
  (let [game-state (re-frame/subscribe [:opera-main])]
    (build-main-panel game-state re-frame/dispatch)))

(defn opera-game-panel []
  (let [tab-state (r/atom 0)
        dimensions (.get ui/dimensions "screen")
        scene-map (ui/SceneMap (clj->js {:main (r/reactify-component main-panel)
                                         :notes (r/reactify-component notes-panel)
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
                                      {:key "notes"
                                       :title "Notes"
                                       :icon "bar-chart"}
                                      {:key "other"
                                       :title "Extras"
                                       :icon "more-horiz"}]}
          :render-scene scene-map}
         ]
        ]))))
