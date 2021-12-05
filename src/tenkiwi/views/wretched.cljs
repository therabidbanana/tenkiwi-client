(ns tenkiwi.views.wretched
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [oops.core :refer [oget]]
            [tenkiwi.views.shared :as ui]))


(defonce do-collapse! (r/atom (fn [])))
#_(defn -image-panel [{:as display
                     :keys [dispatch]}]
  [ui/scroll-view
   [:img {:src (str (:text image))}]
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
                     :keys [image dispatch]}]
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
    [ui/text ""]]])

(defn -main-panel [display]
  (let [active-tags (-> display :card :tags)
        doom? (:doom active-tags)
        progress? (:clue active-tags)]
    [ui/collapse-scroll-view {:collapse! do-collapse!}
     [ui/view
      [ui/para {:theme {:colors {:text "white"}}
                :style {:padding-top  4
                        :padding-left 8}}
       (str "Progress: " (-> display :clocks :clues)
            ", Doom: " (-> display :clocks :doom) #_(str (-> clocks :plot) " / " 8))]]
     [ui/actions-list display]
     [ui/progressbar {:progress (:clock display)
                      :color "#336699"}]
     [ui/bottom-sheet-card (assoc display
                                  :collapse! do-collapse!
                                  :border-color (cond
                                                  doom? "darkred"
                                                  progress? "olivedrab")
                                  :turn-marker
                                  (str (get-in display [:active-player :user-name])
                                       "'s turn..."))]])
  )

(defn -wretched-game-panel [user-data dispatch]
  (let [tab-state  (r/atom 0)
        dimensions (.get ui/dimensions "window")]
    (fn [user-data dispatch]
      (let [{user-id        :id
             :as            data
             {:as   room
              :keys [game]} :current-room} @user-data
            active?                        (= user-id (:id (:active-player game)))
            clock (/ (-> game :clocks (get :tower 1)) 100)

            {:keys [actions card]
             :as   display}                  (if active?
                                                   (:active-display game)
                                                   (:inactive-display game))
            x-carded?                      (:x-card-active? display)

            display         (assoc display
                                   :active-player (:active-player game)
                                   :image (:image game)
                                   :clock clock
                                   :clocks (:clocks game)
                                   :dispatch dispatch)
            on-tab-change   (fn [x] (reset! tab-state x))
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
                             :bottom          3}
            ]
        [ui/view {:style sizing}
         [ui/tab-view
          {:initial-layout   (if-not (ui/os? "web") sizing)
           :scroll-enabled   true
           :on-index-change  on-tab-change
           ;;:scene-container-style {:background-color "red"}
           :navigation-state {:index  current-index
                              :routes [{:key   "main"
                                        :title "Main"}
                                       {:key   "other"
                                        :title "Extras"}]}
           :render-tab-bar   (fn [props]
                               (let [_ (goog.object/set props "tabStyle" (clj->js tab-style))
                                     _ (goog.object/set props "indicatorStyle" (clj->js indicator-style))
                                     _ (goog.object/set props "style" (clj->js bar-style))
                                     ;; Disable uppercase transform
                                     ;; _ (goog.object/set props "getLabelText" (fn [scene] (aget (aget scene "route") "title")))
                                     ]
                               (r/as-element [ui/tab-bar (js->clj props)])))
           :render-scene     (fn [props]
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