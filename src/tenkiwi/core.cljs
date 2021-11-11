(ns tenkiwi.core
  (:require
   [reagent.core :as r :refer [atom]]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [oops.core :refer [ocall]]
   [tenkiwi.events]
   [tenkiwi.subs]
   [tenkiwi.views :as views]
   [tenkiwi.config :as config]
   [react-native :as ReactNative]
   ["@expo/vector-icons" :as AtExpo]
   ["@expo/react-native-action-sheet" :as action-sheet]
   [react-native-paper :as rn-paper]
   #_[tenkiwi.handlers]
   #_[tenkiwi.subs]))

#_(set! *warn-on-infer* true)

(def ReactNative (js/require "react-native"))
(def expo (js/require "expo"))
(def AtExpo (js/require "@expo/vector-icons"))
(def rn-paper (js/require "react-native-paper"))
(def PaperProvider (r/adapt-react-class (.-Provider rn-paper)))
(def action-sheet (js/require "@expo/react-native-action-sheet"))
(def ActionSheetProvider (r/adapt-react-class (.-ActionSheetProvider action-sheet)))
(def DefaultTheme (js->clj (.-DefaultTheme rn-paper)))

(def ionicons (.-Ionicons AtExpo))
(def ic (r/adapt-react-class ionicons))

(def platform (.-Platform ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def Alert (.-Alert ReactNative))

(defn alert [title]
  (if (= (.-OS platform) "web")
    (js/alert title)
    (.alert Alert title)))

(def theme
  (-> DefaultTheme
      (assoc-in ["colors" "primary"] "#1e88e5")
      (assoc-in ["colors" "secondary"] "#ba68c8")))

(defn app-root []
  (fn []
    [PaperProvider {:theme theme}
     [ActionSheetProvider [views/main-panel]]]))

(defn init []
  (dispatch-sync [:initialize-db])
  (ocall expo "registerRootComponent" (r/reactify-component app-root)))

