(ns tenkiwi.views.lobby
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [oops.core :refer [oget]]
            [tenkiwi.views.shared :as ui]))

(defonce switch-tab! (r/atom (fn [])))

(defn -player-boot [{:keys [id dispatch] :as props}]
  [ui/button {:on-press #(dispatch [:<-room/boot-player! id])} "x"])

(defn -config-panel [game-data config-data dispatch]
  (let [{:keys [host? room-code available-games game-setup]
         :as   game-data}   @game-data
        {:keys [game-type game-url]
         :as   config-data} @config-data

        configuration (:configuration game-setup)
        params        (merge (:params configuration)
                             config-data)
        inputs        (:inputs configuration)

        update-val  (fn [name val]
                      (dispatch [:forms/set-params (assoc {:action :game-lobby}
                                                          name val)]))
        form-option (fn [name {text  :name
                               :keys [value]}]
                      [ui/chip
                       {:on-press #(update-val name value)
                        :key      value
                        :selected (= value (get params name))}
                       text])]
    [ui/scroll-view {:style {:padding 4}}
     #_[ui/card
        [ui/card-title {:title "Personal Details"}]
        [ui/card-content
         [ui/text "(Nothing to configure)"]]]
     (if host?
       (cond
         configuration
         [ui/view {:style {:margin  4
                           :padding 8}}
          (map
           (fn [{:keys [type label name options value nested]}]
             [ui/view {:key name}
              (cond
                (#{:select} type)
                [ui/card {}
                 [ui/card-content
                  [ui/text {:style {:color "#a1a1a1"}} label]
                  (if (map? options)
                    ;; Picker doesn't have optgroup support... what do?
                    (map (fn [[group-name opts]]
                           (if (or
                                (and nested (#{(get params nested)} group-name))
                                (nil? nested))
                             [ui/view {:key   group-name
                                       :label group-name}
                              (map #(form-option name %) opts)]))
                         options)
                    ;; Non-strings get converted in web fallback
                    [ui/picker-select
                     {:use-native-android-picker-style false

                      :on-value-change #(update-val name (cljs.reader/read-string %))
                      :value           (prn-str (get params name))
                      :style           {:placeholder {:color "#a1a1a1"}
                                        :inputIOS    {:padding-right 24
                                                      :color "#e1e1e1"
                                                      :padding-top   4}
                                        :inputAnroid {:padding-right 24
                                                      :color "#e1e1e1"
                                                      :padding-top   14}}
                      :Icon            (fn [] (r/as-element [ui/ic {:name  "ios-chevron-down"
                                                                    :size  16
                                                                    :color "grey"}]))
                      :items           (map #(assoc %
                                          :label (str (:name %))
                                          :value (prn-str (:value %)))
                                  options)}]
                    #_(map #(form-option name %) options))]]
                :else
                [ui/text-input {:on-change-text #(update-val name %)
                                :label          label
                                :name           name
                                :default-value  (get params name)}])])
           inputs)
          (if (:custom-play? params)
            [ui/button {:mode     "contained"
                        :style    {:margin-top 8}
                        :on-press #(do
                                     (dispatch [:<-game/select! game-type (dissoc params :custom-play?)]))}
             [ui/text "Prepare Game"]]
            [ui/button {:mode     "contained"
                        :style    {:margin-top 8}
                        :on-press #(do
                                     (dispatch [:<-game/start! game-type params]))}
             [ui/text "Start Game"]])
          [ui/button {:mode     "outlined"
                      :style    {:margin-top 8}
                      :on-press #(do
                                   (dispatch [:<-game/select! nil {}]))}
           [ui/text "Deselect Game"]]]
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
         [ui/view {:style {:margin  18
                           :padding 8}}
          (map (fn [{:keys       [title subtitle type sheet]
                     description :text}]
                 [ui/card {:style {:margin-top 18}
                           :key   sheet}
                  [ui/card-title {:title                    title
                                  :subtitle-number-of-lines 3
                                  :subtitle                 subtitle}]
                  [ui/card-content
                   [ui/markdown {} description]
                   [ui/button {:mode     "outlined"
                               :style    {:margin-top 4}
                               :on-press #(do
                                            (dispatch [:<-game/select! type {:title    title
                                                                             :game-url sheet}]))}
                    [ui/text "Select Game"]]]])
               available-games)
          [ui/markdown {}
           "Want to add your own? Games are simple spreadsheets - get [more details here.](https://docs.google.com/forms/d/e/1FAIpQLScmKrw1TDr-OaYrGjmBLCWQj6aex9XCCvdRI-ogOEeYr3n-Xg/viewform?usp=sf_link)"]])
       ;; if not host
       [ui/card
        [ui/card-title {:title "Game Configuration"}]
        [ui/card-content [ui/para "Your host will set this up."]]])]))

(defn config-panel []
  (let [game-data (re-frame/subscribe [:room])
        config-data (re-frame/subscribe [:form :game-lobby])]
    [-config-panel game-data config-data re-frame/dispatch]))

(defn -lobby-panel [game-data config-data dispatch]
  (let [{:keys [room-code host? host-id current-player-id game-setup]
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
                         :description (if (= host-id (:id player))
                                        "(Host Player)")
                         :right (fn [props]
                                  (if host?
                                    (r/as-element [-player-boot (assoc player :dispatch dispatch)])))}])]]]
     [ui/surface {:style {:margin  10
                          :padding 12}}
      [ui/para
       "Once everyone has joined, the host must choose a game type to start."
       " "
       "Players without the app can join via web at "
       [ui/para {:style {:font-weight "bold"}}
        "tenkiwi.com"]]
      [ui/card {:style {:margin-top 8}}
       [ui/card-title {:title "Game Selected"}]
       [ui/card-content
        [ui/para (or title "(None Selected Yet)")]
        (cond
          (and game-type host?)
          [ui/button {:mode     "contained"
                      :style    {:margin-top 8}
                      :on-press #(do
                                   (dispatch [:<-game/start! game-type config-data]))}
           [ui/text "Start Game"]]

          host?
          [ui/button {:mode     "contained"
                      :style    {:margin-top 8}
                      :on-press #(do (@switch-tab! 1))}
           [ui/text "Choose Game"]]

          :else
          [ui/button {:mode     "outlined"
                      :style    {:margin-top 8}
                      :on-press #(do
                                   (dispatch [:<-room/boot-player! current-player-id]))}
           [ui/text "Leave Game"]])]]]]))

(defn main-lobby-panel []
  (let [game-data (re-frame/subscribe [:room])
        config-data (re-frame/subscribe [:form :game-lobby])]
    [-lobby-panel game-data config-data re-frame/dispatch]))

(defn lobby-panel []
  (let [tab-state (r/atom 0)
        scene-map (ui/SceneMap (clj->js {:main   (r/reactify-component main-lobby-panel)
                                         :config (r/reactify-component config-panel)}))]
    (fn []
      (let [on-tab-change (fn [x] (reset! tab-state x))
            ;; TODO: Is this really the best way for me to do a tab change?
            _             (reset! switch-tab! on-tab-change)
            current-index @tab-state]
        [ui/clean-tab-view
         {:on-index-change  on-tab-change
          ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
          :navigation-state {:index  current-index
                             :routes [{:key   "main"
                                       :title "Lobby"}
                                      {:key   "config"
                                       :title "Configure"}]}
          :render-scene     scene-map}]))))
