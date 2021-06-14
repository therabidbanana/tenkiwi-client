(ns tenkiwi.views
  (:require [re-frame.core :as re-frame]
            [tenkiwi.views.ftq :refer [-ftq-game-panel]]
            [tenkiwi.views.debrief :refer [debrief-game-panel]]
            [tenkiwi.views.oracle :refer [oracle-game-panel]]
            [reagent.core :as r :refer [atom]]))

(def ReactNative (js/require "react-native"))
(def expo (js/require "expo"))
(def AtExpo (js/require "@expo/vector-icons"))
(def ionicons (.-Ionicons AtExpo))
(def ic (r/adapt-react-class ionicons))

(def platform (.-Platform ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def safe-view (r/adapt-react-class (.-SafeAreaView ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def flat-list (r/adapt-react-class (.-FlatList ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def Alert (.-Alert ReactNative))

(def rn-paper (js/require "react-native-paper"))

(def text-input (r/adapt-react-class (.-TextInput rn-paper)))
(def para (r/adapt-react-class (.-Paragraph rn-paper)))
(def h1 (r/adapt-react-class (.-Title rn-paper)))
(def button (r/adapt-react-class (.-Button rn-paper)))
(def list-stuff (.-List rn-paper))
(def list-section (r/adapt-react-class (.-Section list-stuff)))
(def list-item (r/adapt-react-class (.-Item list-stuff)))
(def list-header (r/adapt-react-class (.-Subheader list-stuff)))
(def card (r/adapt-react-class (.-Card rn-paper)))
(def card-content (r/adapt-react-class (.. rn-paper -Card -Content)))
(def card-actions (r/adapt-react-class (.. rn-paper -Card -Actions)))

(def box-style {:margin 12
                :padding 12})

(defn -join-panel [join dispatch]
  (let []
    [card {:style box-style}
     [view
      [text-input {:name      "game-user-name"
                   :label "Name"
                   :mode "outlined"
                   :default-value     (-> join deref :user-name)
                   :on-change-text #(dispatch [:join/set-params {:user-name %}])}]
      [text-input {:name      "game-lobby-code"
                   :label "Lobby Code"
                   :mode "outlined"
                   :default-value     (-> join deref :room-code)
                   :on-change-text #(dispatch [:join/set-params {:room-code %}])}]]
     [view {:style {:margin-top 8}}
      [button
       {:mode     "contained"
        :on-press #(do
                     (dispatch [:<-join/join-room!]))}
       "Join"]]]))

(defn join-panel []
  (let [user-atom   (re-frame/subscribe [:join])
        new-allowed true]
    [-join-panel user-atom re-frame/dispatch]))

(defn -player-boot [{:keys [id dispatch] :as props}]
  [button {:on-press #(dispatch [:<-room/boot-player! id])} "x"])

(def PlayerBoot (r/reactify-component -player-boot))

(defn -lobby-panel [game-data dispatch]
  (let [game-data @game-data]
    [view
     [list-section
      (for [player (:players game-data)]
        ^{:key (:id player)}
        [list-item {:title (:user-name player)
                    :right (fn [props]
                             (r/create-element PlayerBoot (clj->js (assoc player :dispatch dispatch))))}])]
     [view
      (if (= (:room-code game-data) "haslem")
        [button {:mode "outlined"
                 :on-press #(do
                              (dispatch [:<-game/start! :ftq]))}
         [text "Start: FTQ (Original)"]])
      [button {:mode "outlined"
               :on-press #(do
                            (dispatch [:<-game/start! :ftq {:game-url "https://docs.google.com/spreadsheets/d/e/2PACX-1vQy0erICrWZ7GE_pzno23qvseu20CqM1XzuIZkIWp6Bx_dX7JoDaMbWINNcqGtdxkPRiM8rEKvRAvNL/pub?gid=59533190&single=true&output=tsv"}]))}
       [text "Start: For The Captain"]]
      [button {:mode "outlined"
               :on-press #(do
                            (dispatch [:<-game/start! :debrief {:game-url "https://docs.google.com/spreadsheets/d/e/2PACX-1vQy0erICrWZ7GE_pzno23qvseu20CqM1XzuIZkIWp6Bx_dX7JoDaMbWINNcqGtdxkPRiM8rEKvRAvNL/pub?gid=1113383423&single=true&output=tsv"}]))}
       [text "Start: The Debrief"]]
      [button {:mode "outlined"
               :on-press #(do
                            (dispatch [:<-game/start! :debrief {:game-url "https://docs.google.com/spreadsheets/d/e/2PACX-1vQy0erICrWZ7GE_pzno23qvseu20CqM1XzuIZkIWp6Bx_dX7JoDaMbWINNcqGtdxkPRiM8rEKvRAvNL/pub?gid=599053556&single=true&output=tsv"}]))}
       [text "Start: The Culinary Contest"]]
      (if (= (:room-code game-data) "haslem")
        [button {:mode "outlined"
                 :on-press #(do
                              (dispatch [:<-game/start! :oracle {:game-url "https://docs.google.com/spreadsheets/d/e/2PACX-1vQy0erICrWZ7GE_pzno23qvseu20CqM1XzuIZkIWp6Bx_dX7JoDaMbWINNcqGtdxkPRiM8rEKvRAvNL/pub?gid=1204467298&single=true&output=tsv"}]))}
         [text "Seer: D&D"]])]]))

(defn lobby-panel []
  (let [game-data (re-frame/subscribe [:room])]
    [-lobby-panel game-data re-frame/dispatch]))

(defn game-panel []
  (let [game-type (re-frame/subscribe [:user->game-type])]
    (case @game-type
      :ftq
      [-ftq-game-panel (re-frame/subscribe [:user]) re-frame/dispatch]
      :debrief
      [debrief-game-panel]
      :oracle
      [oracle-game-panel])))

(defn layout [body]
  [safe-view {:style {:overflow-x "hidden"
                      :min-height "100%"
                      :background-color "#1e88e5"}}
   [view {:style {:background-color "#003366"}} body]
   [view {:style {:padding 8
                  :text-align "center"
                  :background-color "rgba(100,80,120,0.8)"}}
    [:> (.-Caption rn-paper)
     "This work is based on For the Queen"
     ", product of Alex Roberts and Evil Hat Productions, and licensed for our use under the "
     "Creative Commons Attribution 3.0 Unported license"]]])

(defn -connecting-panel []
  (let []
    [text "Connecting to server..."]))

(defn main-panel []
  (let [user (re-frame/subscribe [:user])
        room (re-frame/subscribe [:room])
        game (re-frame/subscribe [:user->game-type])]
    (println "rerender main-panel")
    (layout
     (cond
       @game [game-panel]
       (get @user :current-room) [lobby-panel]
       (get @user :connected?) [join-panel]
       :else [-connecting-panel]))))
