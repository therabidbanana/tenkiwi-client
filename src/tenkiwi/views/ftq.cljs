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
(def dimensions (.-Dimensions ReactNative))
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


(def tab-lib (js/require "react-native-tab-view"))
(def tab-view (r/adapt-react-class (.. tab-lib -TabView)))
(def scene-helper (.. tab-lib -SceneMap))

(defn maybe-confirm! [confirm? on-true]
  (let [web? (= "web" (.-OS platform))]
    (cond
      (and confirm? web?)
      (if (js/confirm "Are you sure?")
        (on-true))
      confirm?
      (.alert Alert "Confirm"
              "Are you sure?"
              (clj->js
               [{:text "OK"
                 :onPress on-true}
                {:text "Cancel"
                 :style "cancel"}]))
      :else
      (on-true))))

(defn -action-button [dispatch act]
  (let [action (keyword (or
                         (.-action act)
                         (get act "action")))
        text (or
              (.-text act)
              (get act "text"))]
    [button {:style {:margin-top 4}
             :mode "contained"
             :on-press #(dispatch [:<-game/action! action])}
     text]))

#_(defn -secondary-action-button [dispatch act]
  (let [action (keyword (or
                         (.-action act)
                         (get act "action")
                         (get act :action)))
        text (or
              (.-text act)
              (get act "text")
              (get act :text))
        conf (or
              (.-confirm act)
              (get act "confirm")
              (get act :confirm))]
    [button {:style {:margin-top 4}
             :mode "outlined"
             :on-press #(dispatch [:<-game/action! action])}
     text]))

;; Action tray is a bit weird because bottom sheet is weird with a render
;; content function
(defn -action-tray [{:as props
                     :keys [dispatch actions]
                     }]
  (let [dispatch (or dispatch (get props "dispatch"))
        actions (or actions (get props "actions"))]
    (println props)
    [surface {:elevation 8
              :style {:background-color "rgba(0,0,0,0.2)"
                      :padding 10
                      :padding-top 18
                      :padding-bottom 18
                      :height "100%"}}
     (map #(vector -action-button dispatch %) actions)]))

(def ActionTray (r/reactify-component -action-tray))

(defn bottom-sheet-fixed [props]
  (let [web? (= "web" (.-OS platform))]
    (if web?
      [view
       [view {:style {:min-height "20vh"
                      :visibility "hidden"}}
        [-action-tray (js->clj (clj->js props))]]
       [portal
        [view {:style {:position "fixed"
                       :bottom 0
                       :background-color "rgba(0,0,0,0.2)"
                       :height "20vh"
                       :min-height "20vh"
                       :width "100%"}}
         [-action-tray (js->clj (clj->js props))]]]]
      [portal
       [bottom-sheet {:snap-points ["40%" "20%" 18]
                      :initial-snap 1
                      :enabled-content-tap-interaction false
                      :render-content #(r/create-element ActionTray (clj->js props))
                      }]])
    ))

(defn card-with-button [display]
  (let [x-carded? (get display :x-card-active?)
        card-data (get display :card)
        dispatch (get display :dispatch)]
    [-card {:elevation 4
              :style {:margin 4
                      :margin-bottom 16
                      :padding 18
                      :font-size 16}}
     [card-actions {:class (if x-carded? "active" "inactive")}
      [button {:on-press #(dispatch [:<-game/action! :x-card])} "X"]]
     [card-content {:class (str (name (get-in card-data [:state]))
                        " "
                        (if x-carded?
                          "x-carded"))}
      [markdown {:style {:body {:font-size 24
                                :font-family "Georgia"}}}
       (get-in card-data [:text])]]
     ]))

(defn -other-panel [{:as display
                     :keys [dispatch]}]
  [view
   #_[:img {:src (str (:text queen))}]
   (map (fn [{conf :confirm
              :keys [action class text]}]
          (with-meta
            (vector button
                    {:mode "outlined"
                     :on-press (fn []
                                 (maybe-confirm! conf
                                                 #(dispatch [:<-game/action! action])))} text)
            {:key action}))
        (get-in display [:extra-actions]))])

(defn -main-panel [display]
  [view
   [card-with-button display]
   [bottom-sheet-fixed display]]
  )

(defn -ftq-game-panel [user-data dispatch]
  (let [tab-state (r/atom 1)
        dimensions (.get dimensions "window")]
    (fn [user-data dispatch]
      (let [{user-id        :id
             :as            data
             {:as   room
              :keys [game]} :current-room} @user-data
            active?                        (= user-id (:id (:active-player game)))
            queen                          (:queen game)
            {:keys [actions card]
             :as display}                        (if active?
                                                   (:active-display game)
                                                   (:inactive-display game))
            x-carded?                      (:x-card-active? display)
            action-btn (partial -action-button dispatch)
            display (assoc display :dispatch dispatch)

            on-tab-change (fn [x] (reset! tab-state x))]
       [tab-view
        {:initial-layout {:width (.-width dimensions)}
         :on-index-change on-tab-change
         :navigation-state {:index @tab-state
                            :routes [{:key "main"
                                      :title "Game"}
                                     {:key "other"
                                      :title "Actions"}]}
         :render-scene (fn [props]
                         (let [key (.. props -route -key)]
                           (case key
                                 "main"
                                 (r/as-element [-main-panel display])
                                 "other"
                                 (r/as-element [-other-panel display])
                                 (r/as-element [text "WHAAAA"])
                                 )))}

        ]))))
