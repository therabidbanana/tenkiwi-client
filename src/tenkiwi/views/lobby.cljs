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
      [ui/markdown {}
       "Want to add your own? Games are simple spreadsheets - get [more details here.](https://docs.google.com/forms/d/e/1FAIpQLScmKrw1TDr-OaYrGjmBLCWQj6aex9XCCvdRI-ogOEeYr3n-Xg/viewform?usp=sf_link)"]]
     ]))

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
     ]))

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
                                      {:key "config"
                                       :title "Configure"}]}
          :render-scene scene-map}
         ]))))
