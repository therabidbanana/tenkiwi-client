(ns tenkiwi.views
  (:require [re-frame.core :as re-frame]
            [tenkiwi.views.ftq :refer [-ftq-game-panel]]
            [tenkiwi.views.debrief :refer [debrief-game-panel]]
            [tenkiwi.views.oracle :refer [oracle-game-panel]]
            [tenkiwi.views.walking-deck :refer [walking-deck-game-panel]]
            [tenkiwi.views.lobby :refer [lobby-panel]]
            [tenkiwi.views.home-screen :refer [opening-panel]]
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
       [para "Connecting to server... If this takes too long you might need to quit the app and restart. Or the servers are down. :("]]]]))

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
