(ns tenkiwi.views
  (:require [re-frame.core :as re-frame]
            [tenkiwi.views.ftq :refer [-ftq-game-panel]]
            [tenkiwi.views.debrief :refer [debrief-game-panel]]
            [tenkiwi.views.oracle :refer [oracle-game-panel]]
            [tenkiwi.views.walking-deck :refer [walking-deck-game-panel]]
            [tenkiwi.views.lobby :refer [lobby-panel]]
            [tenkiwi.views.shared :as ui]
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
                                    3)
        dimensions (.get ui/dimensions "screen")
        ]
    [scroll-view
     [card {:style box-style}
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
          [para {:style {:margin-top    12
                         :margin-bottom 4}}
           "Optional - a game unlock code"]
          [text-input {:name            "game-unlock-code"
                       :label           "Unlock Code"
                       :mode            "outlined"
                       :auto-focus      true
                       :auto-capitalize "none"
                       :auto-correct    false
                       :default-value   (-> join deref :unlock-code)
                       :on-change-text  #(dispatch [:join/set-params {:unlock-code (str/lower-case %)}])}]
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
      ]
     [ui/view {:height (* 0.7 (.-height dimensions))}
      [ui/text ""]]
     ]))

(defn join-panel []
  (let [user-atom   (re-frame/subscribe [:join])
        form-state (r/atom :name)]
    [-join-panel form-state user-atom re-frame/dispatch]))

(defn welcome-panel []
  (let [user-atom   (re-frame/subscribe [:join])
        dimensions (.get ui/dimensions "screen")
        form-state (r/atom :name)]
    [ui/scroll-view
     [ui/card
      [ui/card-content
       [ui/para
        "Welcome to Tenkiwi"
        ]]]
     [ui/view {:height (* 0.7 (.-height dimensions))}
      [ui/text ""]]
     (if (clojure.string/blank? (:room-code @user-atom))
       [ui/bottom-sheet-card
        {:dispatch re-frame/dispatch
         :turn-marker "About Tenkiwi"
         :card {:text (str
                       "Tenkiwi is an app for playing storytelling games with friends.\n\n"
                       "For this to work, you're going to need a way to talk to each other, either being in the same room or in a video conference app.\n\n"
                       "However you choose to play Tenkiwi, the goal stays the same - to have fun telling a story together."
                       )} }])]
    ))

(defn settings-panel []
  (let [user-atom   (re-frame/subscribe [:join])
        dimensions (.get ui/dimensions "screen")
        form-state (r/atom :name)]
    [ui/scroll-view {:style {:margin 12}}
     [ui/card {:style {:margin-top 12}}
      [ui/card-title {:title "Credits"
                      :subtitle "Tenkiwi"}]
      [ui/card-content
       [ui/para
        "Tenkiwi is a hybrid storytelling game app built by David Haslem."]]]
     [ui/card {:style {:margin-top 12}}
      [ui/card-title {:subtitle "Descended from the Queen"}]
      [ui/card-content
       [ui/para
        "This work is based on For the Queen"
        " (found at http://www.forthequeengame.com/)"
        ", product of Alex Roberts and Evil Hat Productions, and licensed for our use under the "
        "Creative Commons Attribution 3.0 Unported license"
        "  (http://creativecommons.org/licenses/by/3.0/)."]]]
     [ui/card {:style {:margin-top 12}}
      [ui/card-title {:subtitle "X-Card"}]
      [ui/card-content
       [ui/para
        "This application adapts the X-Card, originally by John Stavropoulos"
        "  (http://tinyurl.com/x-card-rpg)."]]]
     [ui/view {:height (* 0.7 (.-height dimensions))}
      [ui/text ""]]
     [view {:style {:padding          8
                    :text-align       "center"
                    :background-color "rgba(100,80,120,0.8)"}}
      ]]))

(defn opening-panel []
  (let [tab-state (r/atom 0)
        scene-map (ui/SceneMap (clj->js {:welcome (r/reactify-component welcome-panel)
                                         :play (r/reactify-component join-panel)
                                         :settings (r/reactify-component settings-panel)}))]
    (fn []
      (let [
            on-tab-change (fn [x] (reset! tab-state x))
            current-index @tab-state
            ]
        [ui/clean-tab-view
         {:on-index-change on-tab-change
          ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
          :navigation-state {:index current-index
                             :routes [{:key "welcome"
                                       :title "Welcome"}
                                      {:key "play"
                                       :title "Play"}
                                      {:key "settings"
                                       :title "Other"}]}
          :render-scene scene-map}
         ]))))

(defn game-panel []
  (let [game-type (re-frame/subscribe [:user->game-type])
        toast     (re-frame/subscribe [:toast])]
    [ui/view
     [ui/portal
      [ui/snackbar {:visible (:visible @toast)
                    :duration 10000
                    :wrapper-style {:top 0
                                    :bottom nil
                                    :z-index 102}
                    :on-dismiss #(re-frame/dispatch [:hide-toast])}
       [ui/text (:message @toast)]]]
     (case @game-type
       :ftq
       [-ftq-game-panel (re-frame/subscribe [:user]) re-frame/dispatch]
       :debrief
       [debrief-game-panel]
       :walking-deck
       [walking-deck-game-panel]
       :oracle
       [oracle-game-panel])]))

(defn layout [body]
  [safe-view {:style {:overflow-x "hidden"
                      :min-height "100%"
                      :padding-top (if (ui/os? "android")
                                     (.. ReactNative -StatusBar -currentHeight))
                      :flex 1
                      :background-color "#1e88e5"}}
   [view {:style {:background-color "#003366"}}
    body]])

(defn -connecting-panel []
  (let []
    [view {:style
           {:height "100%"
            :padding 16}}
     [card {:style {:margin-top "auto"
                    :margin-bottom "auto"}}
      [card-content {}
       [para "Connecting to server... If this takes too long you might need to quit the app and restart. :("]]]]))

(defn main-panel []
  (let [user  (re-frame/subscribe [:user])
        room  (re-frame/subscribe [:room])
        game  (re-frame/subscribe [:user->game-type])]
    (layout
     (cond
       @game [game-panel]
       (get @user :current-room) [lobby-panel]
       (get @user :connected?) [opening-panel]
       :else [-connecting-panel])
     )))
