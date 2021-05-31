(ns env.index
  (:require [env.dev :as dev]))

;; undo main.js goog preamble hack
(set! js/window.goog js/undefined)

(-> (js/require "../../../js/figwheel-bridge")
    (.withModules #js {"react-native-markdown-display" (js/require "react-native-markdown-display"), "./assets/icons/loading.png" (js/require "../../../assets/icons/loading.png"), "expo" (js/require "expo"), "./assets/images/cljs.png" (js/require "../../../assets/images/cljs.png"), "./assets/icons/app.png" (js/require "../../../assets/icons/app.png"), "@react-native-async-storage/async-storage" (js/require "@react-native-async-storage/async-storage"), "reanimated-bottom-sheet" (js/require "reanimated-bottom-sheet"), "react-native-tab-view" (js/require "react-native-tab-view"), "react-native" (js/require "react-native"), "react" (js/require "react"), "react-native-paper" (js/require "react-native-paper"), "create-react-class" (js/require "create-react-class"), "@expo/vector-icons" (js/require "@expo/vector-icons")}
)
    (.start "main" "expo" "10.0.0.94"))
