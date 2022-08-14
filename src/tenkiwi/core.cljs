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
   [react-native-safe-area-context :as safe-area]
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
(def safe-area (js/require "react-native-safe-area-context"))
(def ActionSheetProvider (r/adapt-react-class (.-ActionSheetProvider action-sheet)))
(def SafeAreaProvider (r/adapt-react-class (.-SafeAreaProvider safe-area)))
(def DefaultTheme (js->clj (.-DefaultTheme rn-paper)))
(def DarkTheme (js->clj (.-DarkTheme rn-paper)))

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
  (-> DarkTheme
      (assoc "dark" true)
      ;;; Original Blue
      ;; (assoc-in ["colors" "primary"] "#1e88e5")
      ;; Darker blue
      ;; (assoc-in ["colors" "primary"] "#01579b")
      ;; Bright blue
      (assoc-in ["colors" "primary"] "#64B5F6")
      ;; Slate grey
      ;; (assoc-in ["colors" "primary"] "#455a64")

      ;; (assoc-in ["colors" "secondary"] "#212121")
      (assoc-in ["colors" "accent"] "#ba68c8")))

(defn app-root []
  (fn []
    [SafeAreaProvider
     [PaperProvider {:theme theme}
      [ActionSheetProvider [views/main-panel]]]]))

(defn init []
  (dispatch-sync [:initialize-db])
  (ocall expo "registerRootComponent" (r/reactify-component app-root)))

