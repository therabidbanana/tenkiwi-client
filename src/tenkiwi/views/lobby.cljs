(ns tenkiwi.views.lobby
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [oops.core :refer [oget]]
            [tenkiwi.views.shared :as ui]))

(defn -player-boot [{:keys [id dispatch] :as props}]
  [ui/button {:on-press #(dispatch [:<-room/boot-player! id])} "x"])

(defn -config-panel [game-data config-data dispatch]
  (let [{:keys [host? room-code available-games game-setup]
         :as   game-data} @game-data
        {:keys [game-type game-url]
         :as config-data} @config-data

        configuration (:configuration game-setup)
        params        (merge (:params configuration)
                             config-data)
        inputs        (:inputs configuration)

        update-val  (fn [name val]
                      (dispatch [:forms/set-params (assoc {:action :game-lobby}
                                                          name val)]))
        form-option (fn [name {text :name
                               :keys [value]}]
                      [ui/chip
                       {:on-press #(update-val name value)
                        :key    value
                        :selected (= value (get params name))}
                       text])]
    [ui/scroll-view {:style {:padding 4}}
     [ui/card
      [ui/card-title {:title "Personal Details"}]
      [ui/card-content
       [ui/text "(Nothing to configure)"]]]
     (if host?
       (cond
         configuration
         [ui/card
          (map
           (fn [{:keys [type label name options value nested]}]
             [ui/view {:key name}
              (cond
                (#{:select} type)
                [ui/card {}
                 [ui/card-content 
                  [ui/text label]
                  (if (map? options)
                    (map (fn [[group-name opts]]
                           (if (or
                                (and nested (#{(get params nested)} group-name))
                                (nil? nested))
                             [ui/view {:key group-name
                                       :label group-name}
                              (map #(form-option name %) opts)]))
                         options)
                    (map #(form-option name %) options))]]
                :else
                [ui/text-input {:on-change-text #(update-val name %)
                                :label     label
                                :name      name
                                :default-value     (get params name)}])
              ])
           inputs)
          [ui/surface {:style {:margin  18
                               :padding 8}}
           [ui/button {:mode     "outlined"
                       :style    {:margin-top 4}
                       :on-press #(do
                                    (dispatch [:<-game/select! nil {}]))}
            [ui/text "Deselect Game"]]]]
         game-type
         [ui/surface {:style {:margin  18
                              :padding 8}}
          [ui/activity-indicator]
          [ui/button {:mode     "outlined"
                      :style    {:margin-top 4}
                      :on-press #(do
                                   (dispatch [:<-game/select! nil {}]))}
           [ui/text "Deselect Game"]]]
         :else
         [ui/surface {:style {:margin  18
                              :padding 8}}
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
                                            (dispatch [:<-game/select! type {:title title
                                                                             :game-url sheet}]))}
                    [ui/text "Select Game"]]]])
               available-games)
          [ui/markdown {}
           "Want to add your own? Games are simple spreadsheets - get [more details here.](https://docs.google.com/forms/d/e/1FAIpQLScmKrw1TDr-OaYrGjmBLCWQj6aex9XCCvdRI-ogOEeYr3n-Xg/viewform?usp=sf_link)"]]))
     ]))

(defn config-panel []
  (let [game-data (re-frame/subscribe [:room])
        config-data (re-frame/subscribe [:form :game-lobby])]
    [-config-panel game-data config-data re-frame/dispatch]))

(defn -lobby-panel [game-data config-data dispatch]
  (let [{:keys [room-code host? game-setup]
         :as   game-data} @game-data

        {:keys [game-type title game-url]
         :as   config-data} (merge (get-in game-setup [:configuration :params] {})
                                   @config-data)]
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
                               (if host?
                                 (r/as-element [-player-boot (assoc player :dispatch dispatch)])))}])]]]
     [ui/surface {:style {:margin  18
                          :padding 8}}
      [ui/para
       "Once everyone has joined, the host must choose a game type to start."
       " "
       "Players without the app can join via web at "
       [ui/para {:style {:font-weight "bold"}}
        "tenkiwi.com"]]
      [ui/card {:style {:margin-top 8}}
       [ui/card-title {:title "Game Selected"}]
       [ui/card-content
        [ui/text (or title "(None Selected Yet)")]
        (if (and game-type host?)
          [ui/button {:mode     "outlined"
                      :style    {:margin-top 4}
                      :on-press #(do
                                   (dispatch [:<-game/start! game-type config-data]))}
           [ui/text "Start Game"]])
        ]]]
     ]))

(defn main-lobby-panel []
  (let [game-data (re-frame/subscribe [:room])
        config-data (re-frame/subscribe [:form :game-lobby])]
    [-lobby-panel game-data config-data re-frame/dispatch]))

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
