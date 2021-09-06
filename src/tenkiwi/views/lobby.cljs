(ns tenkiwi.views.lobby
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [oops.core :refer [oget]]
            [tenkiwi.views.shared :as ui]))

(defn -player-boot [{:keys [id dispatch] :as props}]
  [ui/button {:on-press #(dispatch [:<-room/boot-player! id])} "x"])

(defn -config-panel [game-data dispatch]
  (let [{:keys [room-code available-games]
         :as   game-data} @game-data]
    [ui/scroll-view {:style {:padding 4}}
     [ui/card
      [ui/card-title {:title    "Players"
                   :subtitle (str "Lobby Code: " room-code)}]
      [ui/card-content
       [ui/list-section
        (for [player (:players game-data)]
          ^{:key (:id player)}
          [ui/list-item {:title (:user-name player)
                      :right (fn [props]
                               (r/as-element [-player-boot (assoc player :dispatch dispatch)]))}])]]]
     [ui/surface {:style {:margin  18
                       :padding 8}}
      [ui/para
       "Once everyone has joined, choose a game type to start."
       " "
       "Players without the app can join via web at "
       [ui/para {:style {:font-weight "bold"}}
        "tenkiwi.com"]]
      (map (fn [{:keys [title subtitle type sheet]
                 description :text}]
             [ui/card {:style {:margin-top 8}
                    :key sheet}
              [ui/card-title {:title                    title
                           :subtitle-number-of-lines 3
                           :subtitle subtitle}]
              [ui/card-content
               [ui/markdown {} description]
               [ui/button {:mode     "outlined"
                        :style    {:margin-top 4}
                        :on-press #(do
                                     (dispatch [:<-game/start! type {:game-url sheet}]))}
                [ui/text "Start Game"]]]])
           available-games)
      [:> (.-Caption ui/rn-paper)
       {:style {:margin-top 18
                :text-align "center"
                :padding 8}}
       "Want to add your own? Games are simple spreadsheets - contact tenkiwigame@gmail.com for more info."
       ]]
     [ui/view {:style {:padding          8
                    :text-align       "center"
                    :background-color "rgba(100,80,120,0.8)"}}
      [:> (.-Caption ui/rn-paper)
       "This work is based on For the Queen"
       " (found at http://www.forthequeengame.com/)"
       ", product of Alex Roberts and Evil Hat Productions, and licensed for our use under the "
       "Creative Commons Attribution 3.0 Unported license"
       "  (http://creativecommons.org/licenses/by/3.0/)."]]]))

(defn config-panel []
  (let [game-data (re-frame/subscribe [:room])]
    [-config-panel game-data re-frame/dispatch]))

(defn -lobby-panel [game-data dispatch]
  (let [{:keys [room-code available-games]
         :as   game-data} @game-data]
    [ui/scroll-view {:style {:padding 4}}
     [ui/card
      [ui/card-title {:title    "Players"
                   :subtitle (str "Lobby Code: " room-code)}]
      [ui/card-content
       [ui/list-section
        (for [player (:players game-data)]
          ^{:key (:id player)}
          [ui/list-item {:title (:user-name player)
                      :right (fn [props]
                               (r/as-element [-player-boot (assoc player :dispatch dispatch)]))}])]]]
     [ui/surface {:style {:margin  18
                       :padding 8}}
      [ui/para
       "Once everyone has joined, choose a game type to start."
       " "
       "Players without the app can join via web at "
       [ui/para {:style {:font-weight "bold"}}
        "tenkiwi.com"]]
      (map (fn [{:keys [title subtitle type sheet]
                 description :text}]
             [ui/card {:style {:margin-top 8}
                    :key sheet}
              [ui/card-title {:title                    title
                           :subtitle-number-of-lines 3
                           :subtitle subtitle}]
              [ui/card-content
               [ui/markdown {} description]
               [ui/button {:mode     "outlined"
                        :style    {:margin-top 4}
                        :on-press #(do
                                     (dispatch [:<-game/start! type {:game-url sheet}]))}
                [ui/text "Start Game"]]]])
           available-games)
      [:> (.-Caption ui/rn-paper)
       {:style {:margin-top 18
                :text-align "center"
                :padding 8}}
       "Want to add your own? Games are simple spreadsheets - contact tenkiwigame@gmail.com for more info."
       ]]
     [ui/view {:style {:padding          8
                    :text-align       "center"
                    :background-color "rgba(100,80,120,0.8)"}}
      [:> (.-Caption ui/rn-paper)
       "This work is based on For the Queen"
       " (found at http://www.forthequeengame.com/)"
       ", product of Alex Roberts and Evil Hat Productions, and licensed for our use under the "
       "Creative Commons Attribution 3.0 Unported license"
       "  (http://creativecommons.org/licenses/by/3.0/)."]]]))

(defn main-lobby-panel []
  (let [game-data (re-frame/subscribe [:room])]
    [-lobby-panel game-data re-frame/dispatch]))

(defn lobby-panel []
  (let [tab-state (r/atom 0)
        scene-map (ui/SceneMap (clj->js {:main (r/reactify-component main-lobby-panel)
                                         :config (r/reactify-component config-panel)}))]
    (fn []
      (let [
            on-tab-change (fn [x] (reset! tab-state x))
            current-index @tab-state
            ]
        [ui/clean-tab-view
         {:on-index-change on-tab-change
          ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
          :navigation-state {:index current-index
                             :routes [{:key "main"
                                       :title "Lobby"}
                                      #_{:key "config"
                                       :title "Configure"}]}
          :render-scene scene-map}
         ]))))
