(ns tenkiwi.core
  (:require
   [reagent.core :as r :refer [atom]]
              [re-frame.core :refer [subscribe dispatch dispatch-sync]]
              [oops.core :refer [ocall]]
              [tenkiwi.events]
              [tenkiwi.subs]
              [tenkiwi.views :as views]
              [tenkiwi.config :as config]
              #_[tenkiwi.handlers]
              #_[tenkiwi.subs]))

(def ReactNative (js/require "react-native"))
(def expo (js/require "expo"))
(def AtExpo (js/require "@expo/vector-icons"))
(def rn-paper (js/require "react-native-paper"))
(def PaperProvider (r/adapt-react-class (.-Provider rn-paper)))
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
     [views/main-panel]]
    #_[view {:style {:flex-direction "column" :margin 40 :align-items "center"}}
       [image {:source (js/require "./assets/images/cljs.png")
               :style {:width 200
                       :height 200}}]
       [text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}} @greeting]
       [ic {:name "ios-arrow-down" :size 60 :color "purple"}]
       [touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                             :on-press #(alert "HELLO!")}
        [text {:style {:color "white" :text-align "center" :font-weight "bold"}} "press me"]]]))

(defn init []
  (dispatch-sync [:initialize-db])
  (ocall expo "registerRootComponent" (r/reactify-component app-root)))

