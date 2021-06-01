(ns tenkiwi.views.shared
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]))

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
(def dimensions (.-Dimensions ReactNative))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def Alert (.-Alert ReactNative))

(def rn-paper (js/require "react-native-paper"))

(def text-input (r/adapt-react-class (.-TextInput rn-paper)))
(def para (r/adapt-react-class (.-Paragraph rn-paper)))
(def h1 (r/adapt-react-class (.-Title rn-paper)))
(def h2 (r/adapt-react-class (.-Subheading rn-paper)))
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

(defn -action-button [dispatch action-valid? {:as action
                                              :keys [params confirm action text]
                                              :or {params {}}}]
  (let []
    [button {:style {:margin-top 4}
             :mode "contained"
             :disabled (not (action-valid? action))
             :on-press (fn []
                         (maybe-confirm! confirm
                                         #(dispatch [:<-game/action! action params])))}
     text]))

;; Action tray is a bit weird because bottom sheet is weird with a render
;; content function
(defn -action-tray [{:as props
                     :keys [dispatch actions action-valid?]
                     }]
  (let [dispatch (or dispatch (get props "dispatch"))
        actions (or actions (get props "actions"))
        action-valid? (or action-valid (constantly true))]
    [surface {:elevation 8
              :style {:background-color "rgba(0,0,0,0.2)"
                      :padding 10
                      :padding-top 18
                      :padding-bottom 18
                      :height "100%"}}
     [scroll-view
      (map #(with-meta [-action-button dispatch action-valid? %]
              {:key %}) actions)]]))

(defn bottom-sheet-fixed [props]
  (let [web? (= "web" (.-OS platform))]
    (if web?
      [view
       [view {:style {:min-height "20vh"
                      :visibility "hidden"}}
        [-action-tray props]]
       [portal
        [view {:style {:position "fixed"
                       :bottom 0
                       :background-color "rgba(0,0,0,0.2)"
                       :height "20vh"
                       :min-height "20vh"
                       :width "100%"}}
         [-action-tray props]]]]
      [portal
       [bottom-sheet {:snap-points ["25%" 64]
                      :initial-snap 1
                      :enabled-bottom-initial-animation true
                      :enabled-content-tap-interaction false
                      :render-content (fn [p] (r/as-element [-action-tray props]))
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
     [card-content {:class (str (name (get-in card-data [:state] "blank"))
                        " "
                        (if x-carded?
                          "x-carded"))}
      [markdown {:style {:body {:font-size 24
                                :font-family "Georgia"}}}
       (get-in card-data [:text])]
      [view
       (map (fn [{:keys [name value label generator]}]
              (with-meta
                [view
                 [h2 label]
                 [para value]
                 ;; [:input {:name name :value value}]
                 ]
                {:key name}))
            (get-in display [:card :inputs]))]
      ]
     ]))
