(ns tenkiwi.views
  (:require [re-frame.core :as re-frame]
            [tenkiwi.views.ftq :refer [-ftq-game-panel]]
            [tenkiwi.views.debrief :refer [debrief-game-panel]]
            [tenkiwi.views.oracle :refer [oracle-game-panel]]
            [react-native-markdown-display :as markdown-lib]
            [reagent.core :as r]
            [clojure.string :as str]))

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

(def markdown-lib (js/require "react-native-markdown-display"))
(def markdown (r/adapt-react-class (.. markdown-lib -default)))

(def rn-paper (js/require "react-native-paper"))

(def text-input (r/adapt-react-class (.-TextInput rn-paper)))
(def para (r/adapt-react-class (.-Paragraph rn-paper)))
(def h1 (r/adapt-react-class (.-Title rn-paper)))
(def h2 (r/adapt-react-class (.-Subheading rn-paper)))
(def surface (r/adapt-react-class (.-Surface rn-paper)))
(def button (r/adapt-react-class (.-Button rn-paper)))
(def list-stuff (.-List rn-paper))
(def list-section (r/adapt-react-class (.-Section list-stuff)))
(def list-item (r/adapt-react-class (.-Item list-stuff)))
(def list-header (r/adapt-react-class (.-Subheader list-stuff)))
(def card (r/adapt-react-class (.-Card rn-paper)))
(def card-content (r/adapt-react-class (.. rn-paper -Card -Content)))
(def card-title (r/adapt-react-class (.. rn-paper -Card -Title)))
(def card-actions (r/adapt-react-class (.. rn-paper -Card -Actions)))

(def box-style {:margin 12
                :padding 12})

(defn secure-rand-id [alphabet number]
  (str (str/join "" (take number (shuffle alphabet)))
       "-"
       (str/join "" (take number (shuffle alphabet)))))

(defn -join-panel [form-state join dispatch]
  (let [random-room (secure-rand-id "abcdefghijklmnopqrstuvwxyz23456789"
                                    3)]
    [scroll-view
     [card {:style box-style}
      [para {:style {:margin-bottom 8}}
       "Tenkiwi is a system for playing story-telling games with friends. "
       "(You will need to be in the same room or in video conference.) "]
      [para {:style {:margin-bottom 8}}
       "To get started, what name do you want to use?"]
      [view
       [text-input {:name           "game-user-name"
                    :label          "Name"
                    :mode           "outlined"
                    :auto-focus     true
                    :default-value  (-> join deref :user-name)
                    :on-change-text #(dispatch [:join/set-params {:user-name %}])}]
       (cond
         (= :name @form-state)
         [view
          [view {:style {:margin-top 8}}
           [button {:mode     "contained"
                    :on-press #(do
                                 (dispatch [:join/set-params {:room-code random-room}])
                                 (reset! form-state :host))}
            "Host a Game"]]
          [view {:style {:margin-top 8}}
           [button {:mode     "contained"
                    :on-press #(do
                                 (dispatch [:join/set-params {:room-code ""}])
                                 (reset! form-state :join))}
            "Join Someone"]]]
         (= :host @form-state)
         [view
          [para {:style {:margin-top    12
                         :margin-bottom 4}}
           "A code friends will use to join the game:"]
          [text-input {:name            "game-lobby-code"
                       :label           "Lobby Code"
                       :mode            "outlined"
                       :auto-focus      true
                       :auto-capitalize "none"
                       :auto-correct    false
                       :default-value   (-> join deref :room-code)
                       :on-change-text  #(dispatch [:join/set-params {:room-code (str/lower-case %)}])}]
          [view  {:style {:margin-top 8}}
           [button
            {:mode     "contained"
             :disabled (< (-> join deref :room-code count) 4)
             :on-press #(do
                          (dispatch [:<-join/join-room!]))}
            "Start"]
           [button
            {:style    {:margin-top 4}
             :on-press #(reset! form-state :name)}

            "Go back"]]]
         :else
         [view
          [para {:style {:margin-top    12
                         :margin-bottom 4}}
           "The host will be able to tell you the code:"]
          [text-input {:name            "game-lobby-code"
                       :label           "Lobby Code"
                       :mode            "outlined"
                       :auto-focus      true
                       :auto-capitalize "none"
                       :auto-correct    false
                       :default-value   (-> join deref :room-code)
                       :on-change-text  #(dispatch [:join/set-params {:room-code (str/lower-case %)}])}]
          [view {:style {:margin-top 8}}
           [button
            {:mode     "contained"
             :disabled (< (-> join deref :room-code count) 4)
             :on-press #(do
                          (dispatch [:<-join/join-room!]))}
            "Join"]
           [button
            {:style    {:margin-top 4}
             :on-press #(reset! form-state :name)}

            "Go back"]]])]
      #_[view {:style {:margin-top 8}}
         [button
          {:mode     "contained"
           :on-press #(do
                        (dispatch [:<-join/join-room!]))}
          "Join"]]]
     [view {:style {:padding          8
                    :text-align       "center"
                    :background-color "rgba(100,80,120,0.8)"}}
      [:> (.-Caption rn-paper)
       "This work is based on For the Queen"
       " (found at http://www.forthequeengame.com/)"
       ", product of Alex Roberts and Evil Hat Productions, and licensed for our use under the "
       "Creative Commons Attribution 3.0 Unported license"
       "  (http://creativecommons.org/licenses/by/3.0/)."]]]))

(defn join-panel []
  (let [user-atom   (re-frame/subscribe [:join])
        form-state (r/atom :name)]
    [-join-panel form-state user-atom re-frame/dispatch]))

(defn -player-boot [{:keys [id dispatch] :as props}]
  [button {:on-press #(dispatch [:<-room/boot-player! id])} "x"])

(defn -lobby-panel [game-data dispatch]
  (let [{:keys [room-code available-games]
         :as   game-data} @game-data]
    [scroll-view {:style {:padding 4}}
     [card
      [card-title {:title    "Players"
                   :subtitle (str "Lobby Code: " room-code)}]
      [card-content
       [list-section
        (for [player (:players game-data)]
          ^{:key (:id player)}
          [list-item {:title (:user-name player)
                      :right (fn [props]
                               (r/as-element [-player-boot (assoc player :dispatch dispatch)]))}])]]]
     [surface {:style {:margin  18
                       :padding 8}}
      [para
       "Once everyone has joined, choose a game type to start."
       " "
       "Players without the app can join via web at "
       [para {:style {:font-weight "bold"}}
        "tenkiwi.com"]]
      (map (fn [{:keys [title subtitle type sheet]
                 description :text}]
             [card {:style {:margin-top 8}
                    :key sheet}
              [card-title {:title                    title
                           :subtitle-number-of-lines 3
                           :subtitle subtitle}]
              [card-content
               [markdown {} description]
               [button {:mode     "outlined"
                        :style    {:margin-top 4}
                        :on-press #(do
                                     (dispatch [:<-game/start! type {:game-url sheet}]))}
                [text "Start Game"]]]])
           available-games)]
     [view {:style {:padding          8
                    :text-align       "center"
                    :background-color "rgba(100,80,120,0.8)"}}
      [:> (.-Caption rn-paper)
       "This work is based on For the Queen"
       " (found at http://www.forthequeengame.com/)"
       ", product of Alex Roberts and Evil Hat Productions, and licensed for our use under the "
       "Creative Commons Attribution 3.0 Unported license"
       "  (http://creativecommons.org/licenses/by/3.0/)."]]]))

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
   [view {:style {:background-color "#003366"}} body]])

(defn -connecting-panel []
  (let []
    [text "Connecting to server..."]))

(defn main-panel []
  (let [user (re-frame/subscribe [:user])
        room (re-frame/subscribe [:room])
        game (re-frame/subscribe [:user->game-type])]
    (layout
     (cond
       @game [game-panel]
       (get @user :current-room) [lobby-panel]
       (get @user :connected?) [join-panel]
       :else [-connecting-panel]))))
