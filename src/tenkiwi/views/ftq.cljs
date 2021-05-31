(ns tenkiwi.views.ftq
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [markdown-to-hiccup.core :as m]))

(def ReactNative (js/require "react-native"))
(def expo (js/require "expo"))
(def AtExpo (js/require "@expo/vector-icons"))
(def ionicons (.-Ionicons AtExpo))
(def ic (r/adapt-react-class ionicons))

(def platform (.-Platform ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def safe-view (r/adapt-react-class (.-SafeAreaView ReactNative)))
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
(def -card (r/adapt-react-class (.-Card rn-paper)))
(def surface (r/adapt-react-class (.-Surface rn-paper)))
(def fab (r/adapt-react-class (.-FAB rn-paper)))
(def portal (r/adapt-react-class (.-Portal rn-paper)))
(def card-content (r/adapt-react-class (.. rn-paper -Card -Content)))
(def card-actions (r/adapt-react-class (.. rn-paper -Card -Actions)))

(def sheet-lib (js/require "reanimated-bottom-sheet"))
(def bottom-sheet (r/adapt-react-class (.. sheet-lib -default)))

(def markdown-lib (js/require "react-native-markdown-display"))
(def markdown (r/adapt-react-class (.. markdown-lib -default)))

(defn -action-button [dispatch act]
  [button {:style {:margin-top 4}
           :mode "contained"
           :on-press #(dispatch [:<-game/action! (keyword (.-action act))])}
   (.-text act)])

(defn -action-tray [{:as props
                     :keys [dispatch actions]}]
  (let []
    [surface {:elevation 8
              :style {:background-color "#bbb"
                      :padding 4
                      :padding-top 18
                      :height "100%"}}
     (map (partial -action-button dispatch) actions)]))

(def ActionTray (r/reactify-component -action-tray))

(defn card-with-button [display]
  (let [x-carded? (get display :x-card-active?)
        card-data (get display :card)
        dispatch (get display :dispatch)]
    [-card {:elevation 4
              :style {:margin 4
                      :margin-bottom 16
                      :padding 18
                      :font-size 16}}
     [card-actions {:text-align "right"
                    :class (if x-carded? "active" "inactive")}
      [button {:on-press #(dispatch [:<-game/action! :x-card])} "X"]]
     [card-content {:class (str (name (get-in card-data [:state]))
                        " "
                        (if x-carded?
                          "x-carded"))}
      [markdown {:style {:body {:font-size 24
                                :font-family "Georgia"}}}
       (get-in card-data [:text])]]
     ]))

(defn -ftq-game-panel [user-data dispatch]
  (let [{user-id        :id
         :as            data
         {:as   room
          :keys [game]} :current-room} @user-data
        ;; TODO - fix active?
        active?                        (= user-id (:id (:active-player game)))
        queen                          (:queen game)
        {:keys [actions card]
         :as display}                        (if active?
                                               (:active-display game)
                                               (:active-display game))
        x-carded?                      (:x-card-active? display)
        action-btn (partial -action-button dispatch)]
    [view
     [card-with-button (assoc display :dispatch dispatch)]
     [portal
      [bottom-sheet
       {:snap-points ["40%" "20%" 18]
        :enabled-content-tap-interaction false
        :render-content #(r/create-element ActionTray (clj->js {:dispatch dispatch :actions actions}))
        }
       ]]
     [view
      #_[:img {:src (str (:text queen))}]
      (map (fn [{conf :confirm
                 :keys [action class text]}]
             (with-meta
               (vector button
                       {:mode "outlined"
                        :on-press #(if (or (not conf) (js/confirm "Are you sure?"))
                                                              (dispatch [:<-game/action! action]))} text)
               {:key action}))
           (get-in display [:extra-actions]))]]))
