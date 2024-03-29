(ns tenkiwi.views
  (:require [re-frame.core :as re-frame]
            [tenkiwi.views.ftq :refer [-ftq-game-panel]]
            [tenkiwi.views.wretched :refer [wretched-game-panel]]
            [tenkiwi.games.debrief :refer [debrief-game-panel]]
            [tenkiwi.games.threads :refer [threads-game-panel]]
            [tenkiwi.games.prompted-by-charge :refer [recharge-game-panel]]
            [tenkiwi.games.opera-v2 :refer [opera-v2-game-panel]]
            [tenkiwi.games.push :refer [push-game-panel]]
            [tenkiwi.views.oracle :refer [oracle-game-panel]]
            [tenkiwi.views.opera :refer [opera-game-panel]]
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

(defn -unsupported []
  (let []
    [ui/view {:style
              {:height  "100%"
               :padding 16}}
     [ui/card {:style {:margin-top    "auto"
                       :margin-bottom "auto"}}
      [ui/card-content {}
       [ui/para "The game you're in doesn't work on this version of the app. You can try to restart with latest updates."]
       [ui/button
        {:style    {:margin-top 8}
         :mode     "contained"
         :on-press #(ui/refresh)}
        "Restart App"]]]]))

(defn game-panel []
  (let [game-type (re-frame/subscribe [:user->game-type])
        toast     (re-frame/subscribe [:toast])]
    [ui/view
     [ui/portal
      [ui/snackbar {:visible (:visible @toast)
                    :duration 5000
                    :wrapper-style {:top 0
                                    :bottom nil
                                    :z-index 102}
                    :on-dismiss #(re-frame/dispatch [:hide-toast])}
       [ui/text (:message @toast)]]]
     (case @game-type
       :ftq
       [-ftq-game-panel (re-frame/subscribe [:user]) re-frame/dispatch]
       :wretched
       [wretched-game-panel]
       :debrief
       [debrief-game-panel]
       :threads
       [threads-game-panel]
       :opera
       [opera-game-panel]
       :opera-v2
       [opera-v2-game-panel]
       :push
       [push-game-panel]
       :prompted-by-charge
       [recharge-game-panel]
       :walking-deck
       [walking-deck-game-panel]
       :walking-deck-v2
       [walking-deck-game-panel]
       :oracle
       [oracle-game-panel]
       ;; ELSE
       [-unsupported])]))

(defn layout [server-type body]
  (let [bg-color (if (#{"staging"} server-type)
                   ;; "rgba(21,25,31,1.0)"
                   "rgba(3,25,53,1.0)"
                   "rgba(10,10,10,1.0)")]
    [ui/safe-view {:style {:overflow-x "hidden"
                           :min-height "100%"
                           :flex 1
                           :background-color bg-color}}
     [ui/status-bar {:style "light"
                     ;; :hidden true
                     :background-color bg-color}]
     [ui/view {:style {:background-color (if (#{"staging"} server-type)
                                           "#001122"
                                           "#121212")}}
      body]]))

(defn -connecting-panel []
  (let []
    [ui/view {:style
              {:height "100%"
               :padding 16}}
     [ui/card {:style {:margin-top "auto"
                       :margin-bottom "auto"}}
      [ui/card-cover {:source (.-face js/assetLibrary)}]
      [ui/card-content {:style {:padding-top 12}}
       [ui/para "Connecting to server... If this takes too long you might need to quit the app and restart. Or the servers are down. :("]]]]))

(defn main-panel []
  (let [user    (re-frame/subscribe [:user])
        room    (re-frame/subscribe [:room])
        storage (re-frame/subscribe [:storage])
        game    (re-frame/subscribe [:user->game-type])]
    (fn []
      (layout
       (:server @storage "prod")
       (cond
         @game [game-panel]
         (get @user :current-room) [lobby-panel]
         (get @user :connected?) [opening-panel]
         :else [-connecting-panel])))))
