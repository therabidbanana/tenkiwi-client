(ns tenkiwi.views.wretched
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [oops.core :refer [oget]]
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
 :wretched-other
 (fn [db]
   (extract-display (:user db)
                    [:image :active-player])))

(defn build-other-panel [game-state-atom dispatch]
  (let []
    (fn -other-panel []
      (let [{:keys [image display]} @game-state-atom]
        [ui/collapse-scroll-view {:collapse! do-collapse!}
         [ui/view {:style {:height 250
                           :align-items "center"
                           :flex-direction "row"}}
          [ui/button {:style {:flex 1}
                      :content-style {:height 250}
                      :on-press #(dispatch [:<-game/action! :previous-image {}])}
           "<"]
          [ui/image {:style {:flex 12
                             :width "100%"
                             :height "100%"}
                     :resize-mode "contain"
                     :source {:uri (str (:text image))}}]

          [ui/button {:style {:flex 1}
                      :content-style {:height 250}
                      :on-press #(dispatch [:<-game/action! :next-image {}])}
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
                (remove #(#{:next-image :previous-image} (:action %))
                        (get-in display [:extra-actions])))]]
         [ui/view {:style {:height (* 0.7 (.-height (.get ui/dimensions "screen")))}}
          [ui/text ""]]]))))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:wretched-other])]
    (build-other-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :wretched-logs
 (fn [db]
   (extract-display (:user db)
                    [:log :active-player :player-ranks])))

(defn build-logs-panel [game-state-atom dispatch]
  (let [notes       (r/atom "")
        notes-input (r/atom nil)]
    (fn -log-panel []
      (let [{:keys [log]} @game-state-atom
            log-text (clojure.string/join "\n\n" (map :text log))]
        [ui/collapse-scroll-view {:only-collapse! do-collapse!}
         [ui/card {:style {:margin-bottom 12}}
          [ui/card-content {}
           [ui/text-input {:on-change-text #(reset! notes  %)
                           :on-focus #(@do-collapse! true)
                           :multiline      true
                           :ref            (partial reset! notes-input)
                           :default-value  (deref notes)}]]
          [ui/card-actions {}
           [ui/button {:flex 1
                       :on-press #(do
                                    (.setString ui/clipboard log-text))}
            [ui/text "Copy Log"]]
           [ui/button {:mode "contained"
                       :flex 2
                       :on-press #(do
                                    (.clear @notes-input)
                                    (dispatch [:<-game/action! :add-note {:text @notes}])
                                    (reset! notes ""))
                       }
            [ui/text "Write your thoughts"]]]]
         [ui/surface {:style {:padding 8 :flex 1}}
          [ui/h1 "Log"]
          (map-indexed
           (fn [i {:keys [type text] :as log-line}]
             (let [styling (cond
                             (= :action type)
                             {:flex 1
                              :padding          6
                              :margin-right     20
                              :text-align       :right
                              :background-color "#bfbfbf"}
                             (= :note type)
                             {:flex             1
                              :margin-left      20
                              :padding          6
                              :background-color "#efefef"}
                             :else
                             {:font-family (if ui/android? "serif" "Georgia")})]
               (with-meta
                 (vector ui/markdown {:style {:body styling}}
                         text)
                 {:key i})))
           (reverse log))]
         [ui/view {:style {:height (* 0.7 (.-height (.get ui/dimensions "screen")))}}
          [ui/text ""]]

         ]))))

(defn log-panel []
  (let [game-state (re-frame/subscribe [:wretched-logs])]
    (build-logs-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :wretched-main
 (fn [db]
   (extract-display (:user db)
                    [:clocks :active-player :player-ranks])))

(defn build-main-panel [game-state-atom dispatch]
  (let []
    (fn -main-panel []
      (let [{:keys           [clocks
                              active-player]
             {:keys [card]
              :as   display} :display} @game-state-atom
            clock                      (/ (-> clocks (get :tower 1)) 100)
            display                    (assoc display :dispatch dispatch)
            active-tags                (:tags card)
            doom?                      (:doom active-tags)
            progress?                  (:clue active-tags)]
        [ui/collapse-scroll-view {:collapse! do-collapse!}
         [ui/view
          [ui/para {:theme {:colors {:text "white"}}
                    :style {:padding-top  4
                            :padding-left 8}}
           (str "Progress: " (-> clocks :clues)
                ", Doom: " (-> clocks :doom) #_(str (-> clocks :plot) " / " 8))]]
         [ui/actions-list display]
         [ui/progressbar {:progress clock
                          :color    "#336699"}]
         [ui/bottom-sheet-card (assoc display
                                      :collapse! do-collapse!
                                      :border-color (cond
                                                      doom?     "darkred"
                                                      progress? "olivedrab")
                                      :turn-marker
                                      (str (get-in active-player [:user-name])
                                           "'s turn..."))]])
      )))

(defn main-panel []
  (let [game-state (re-frame/subscribe [:wretched-main])]
    (build-main-panel game-state re-frame/dispatch)))

(defn wretched-game-panel []
  (let [tab-state  (r/atom 0)
        dimensions (.get ui/dimensions "window")
        scene-map  (ui/SceneMap (clj->js {:main  (r/reactify-component main-panel)
                                          :log   (r/reactify-component log-panel)
                                          :other (r/reactify-component other-panel)
                                          }))]
    (fn []
      (let [on-tab-change   (fn [x] (do
                                      #_(@do-collapse! true)
                                      (reset! tab-state x)))
            current-index   @tab-state
            sizing          (if (ui/os? "web")
                              {:min-height (.-height dimensions)
                               :width      "100%"}
                              {:min-height (.-height dimensions)
                               :width      (.-width dimensions)})
            tab-style       {:minHeight     24
                             :padding       6
                             :paddingBottom 9}
            bar-style       {:backgroundColor "rgba(0,0,0,0.3)"}
            indicator-style {:borderRadius    2
                             :backgroundColor "rgba(255,255,255,0.15)"
                             :height          4
                             :bottom          3}]
        [ui/view {:style sizing}
         [ui/tab-view
          {:initial-layout   (if-not (ui/os? "web") sizing)
           :scroll-enabled   true
           :on-index-change  on-tab-change
           ;;:scene-container-style {:background-color "red"}
           :render-tab-bar   (fn [props]
                               (let [_ (goog.object/set props "tabStyle" (clj->js tab-style))
                                     _ (goog.object/set props "indicatorStyle" (clj->js indicator-style))
                                     _ (goog.object/set props "style" (clj->js bar-style))
                                     ;; Disable uppercase transform
                                     ;; _ (goog.object/set props "getLabelText" (fn [scene] (aget (aget scene "route") "title")))
                                     ]
                                 (r/as-element [ui/tab-bar (js->clj props)])))
           :navigation-state {:index  current-index
                              :routes [{:key   "main"
                                        :title "Main"}
                                       {:key   "log"
                                        :title "Journal"}
                                       {:key   "other"
                                        :title "Extras"}]}
           :render-scene     scene-map}

          ]]))))
