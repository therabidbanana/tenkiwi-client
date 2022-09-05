(ns tenkiwi.views.shared
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [oops.core :refer [oget]]

            [expo :as expo]
            [react :as React]
            [react-native :as ReactNative]
            ["expo-updates" :as expo-updates]
            ["@expo/vector-icons" :as AtExpo]
            [react-native-paper :as rn-paper]
            [expo-clipboard :as expo-clipboard]
            [expo-status-bar :as expo-status-bar]
            [expo-linear-gradient :as expo-linear-gradient]
            ["@expo/react-native-action-sheet" :as action-sheet-lib]
            [reanimated-bottom-sheet :as sheet-lib]
            [react-native-safe-area-context :as safe-area]
            [react-native-reanimated :as Reanimated]
            [react-native-picker-select :as RNPickerSelect]
            [react-native-gesture-handler :as react-native-gesture-handler]
            [react-native-markdown-display :as markdown-lib]
            [react-native-tab-view :as tab-lib]))

#_(set! *warn-on-infer* true)

(def ReactNative (js/require "react-native"))
(def expo (js/require "expo"))
(def expo-status-bar (js/require "expo-status-bar"))
(def expo-linear-gradient (js/require "expo-linear-gradient"))
(def AtExpo (js/require "@expo/vector-icons"))
(def Reanimated (js/require "react-native-reanimated"))
(def RNPickerSelect (js/require "react-native-picker-select"))
(def rn-gesture-handler (js/require "react-native-gesture-handler"))
(def clipboard (.-default (js/require "expo-clipboard")))
(def ionicons (.-Ionicons AtExpo))
(def ic (r/adapt-react-class ionicons))

(def animated-value (.-Value Reanimated))
(def animated-view (r/adapt-react-class (.-View (.-default Reanimated))))
(def use-animated-style (.-useAnimatedStyle Reanimated))
(def use-shared-value (.-useSharedValue Reanimated))
(def status-bar (r/adapt-react-class (.-StatusBar expo-status-bar)))
(def linear-gradient (r/adapt-react-class (.-LinearGradient expo-linear-gradient)))
(def call (.-call Reanimated))

(def platform (.-Platform ReactNative))
(defn os? [arg]
  (= (.-OS platform) arg))
(def web? (os? "web"))
(def android? (os? "android"))
(def ios? (os? "ios"))
(def expo-updates (js/require "expo-updates"))
(defn refresh []
  (if web?
    (.reload (.-location js/window))
    (.reloadAsync expo-updates)))
(def use-window-dimensions (.-useWindowDimensions ReactNative))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def safe-area (js/require "react-native-safe-area-context"))
(def safe-view (r/adapt-react-class (.-SafeAreaView safe-area)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def flat-list (r/adapt-react-class (.-FlatList ReactNative)))
(def refresh-control (r/adapt-react-class (.-RefreshControl ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def pressable (r/adapt-react-class (.-Pressable ReactNative)))
(def dimensions (.-Dimensions ReactNative))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def picker-select (r/adapt-react-class (.-default RNPickerSelect)))
(def Alert (.-Alert ReactNative))

(def React (js/require "react"))
(def use-state (.-useState React))

(def rn-paper (js/require "react-native-paper"))

(def action-sheet-lib (js/require "@expo/react-native-action-sheet"))
(def action-sheet-wrapper (.-connectActionSheet action-sheet-lib))
;; For fallback in react-native-web
(def swipeable (r/adapt-react-class (.-Swipeable rn-gesture-handler)))
(def fling-gesture-handler (r/adapt-react-class (.-FlingGestureHandler rn-gesture-handler)))
(def gesture-directions (.-Directions rn-gesture-handler))

(defn with-action-sheet [component]
  (r/adapt-react-class (action-sheet-wrapper component)))

(def text-input (r/adapt-react-class (.-TextInput rn-paper)))
(def para (r/adapt-react-class (.-Paragraph rn-paper)))
(def h1 (r/adapt-react-class (.-Title rn-paper)))
(def h2 (r/adapt-react-class (.-Subheading rn-paper)))
(def caption (r/adapt-react-class (.-Caption rn-paper)))
(def headline (r/adapt-react-class (.-Headline rn-paper)))
(def button (r/adapt-react-class (.-Button rn-paper)))
(def snackbar (r/adapt-react-class (.-Snackbar rn-paper)))
(def list-stuff (.-List rn-paper))
(def list-section (r/adapt-react-class (.-Section list-stuff)))
(def list-item (r/adapt-react-class (.-Item list-stuff)))
(def list-accordion (r/adapt-react-class (.-Accordion list-stuff)))
(def accordion-group (r/adapt-react-class (.-AccordionGroup list-stuff)))
(def list-icon (r/adapt-react-class (.-Icon list-stuff)))
(def list-header (r/adapt-react-class (.-Subheader list-stuff)))
(def card (r/adapt-react-class (.-Card rn-paper)))
(def surface (r/adapt-react-class (.-Surface rn-paper)))
(def fab (r/adapt-react-class (.-FAB rn-paper)))
(def portal (r/adapt-react-class (.-Portal rn-paper)))
(def chip (r/adapt-react-class (.-Chip rn-paper)))
(def progressbar (r/adapt-react-class (.-ProgressBar rn-paper)))
(def activity-indicator (r/adapt-react-class (.-ActivityIndicator rn-paper)))
(def card-content (r/adapt-react-class (.. rn-paper -Card -Content)))
(def card-cover (r/adapt-react-class (.. rn-paper -Card -Cover)))
(def card-actions (r/adapt-react-class (.. rn-paper -Card -Actions)))
(def card-title (r/adapt-react-class (.. rn-paper -Card -Title)))

(def sheet-lib (js/require "reanimated-bottom-sheet"))
(def bottom-sheet (r/adapt-react-class (.. sheet-lib -default)))

(def markdown-lib (js/require "react-native-markdown-display"))
(def markdown-base (r/adapt-react-class (.. markdown-lib -default)))
(defn markdown
  ([text]
   [markdown {} text])
  ([props text]
   (let [props (-> props
                   (assoc-in [:style :body :color]
                             (get-in props [:style :body :color] "white"))
                   (assoc-in [:style :link :color]
                             (get-in props [:style :link :color] "#64B5F6"))
                   )
         ]
     [markdown-base props text])))

(def tab-lib (js/require "react-native-tab-view"))
(def tab-view (r/adapt-react-class (.. tab-lib -TabView)))
(def tab-bar (r/adapt-react-class (.. tab-lib -TabBar)))
(def SceneMap (.. tab-lib -SceneMap))

(defn clean-tab-view [{:keys [render-scene on-index-change navigation-state]
                       :as   props}]
  ;; "window" dimensions wrong to start sometimes - height 36?
  ;;  note: useWindowDimensions hook did _not_ prevent this problem
  ;;  most likely reagent deferring a render and causing window to be small?
  (let [dimensions      (or (:dimensions props)
                       (.get dimensions "screen"))
        sizing          (if (os? "web")
                          {:min-height (.-height dimensions)
                           :width      "100%"}
                          {:min-height (.-height dimensions)
                           :width      (.-width dimensions)})
        tab-style       {:minHeight     24
                         :padding       6
                         :paddingBottom 9}
        bar-style       {:backgroundColor
                         "rgba(10,10,10,1.0)"
                         #_ "rgba(0,0,0,0.3)"}
        indicator-style {:borderRadius    2
                         :backgroundColor "rgba(30,49,68,0.9)"
                         :height          4
                         :bottom          3}
        tab-render      (fn [bar-props]
                          (let []
                       (r/as-element [tab-bar (merge (js->clj bar-props)
                                                     {:tab-style       tab-style
                                                      :indicator-style indicator-style
                                                      ;; Disable uppercase transform
                                                      ;; :get-label-text (fn [scene] (aget (aget scene "route") "title"))
                                                      :style           bar-style} )])))]
    [view {:style sizing}
     [tab-view
      (merge props
             {:initial-layout (if-not (os? "web")
                                (assoc sizing :height (.-height dimensions)))
              :scroll-enabled true
              :render-tab-bar tab-render})]]))

(defn collapse-scroll-view [props & children]
  (let [collapse (get props :collapse!)
        only-collapse! (get props :only-collapse!)
        scroller (if collapse
                   (fn [e]
                     (let [y (-> e
                                 (aget "nativeEvent")
                                 (aget "contentOffset")
                                 (aget "y"))]
                       (cond (<= 40 y)
                             (@collapse true)
                             (<= y 0)
                             (@collapse false)))))]
    (cond
      scroller
      (into [scroll-view (-> props
                             (assoc :scroll-event-throttle 100)
                             (assoc :on-scroll scroller))]
            children)
      only-collapse!
      (into [scroll-view
             (assoc props :on-scroll-begin-drag (partial @only-collapse! true))]
            children)
      :else
      (into [scroll-view props]
            children))))

(defn maybe-confirm! [confirm? on-true]
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
    (on-true)))

(defn -action-button [{:keys [dispatch action-valid? hide-invalid? mode]
                       :or   {mode "contained"}}
                      {:as   full-action
                       :keys [params confirm action text]
                       :or   {params {}}}]
  (let [valid? (action-valid? full-action)
        ;; Consider alternate colors?
        color  nil]
    (if (or valid? (not hide-invalid?))
      [button {:style    {:margin-top 4}
               :color    color
               :mode     (or mode "contained")
               :disabled (not (action-valid? full-action))
               :on-press (fn []
                           (maybe-confirm! confirm
                                           #(dispatch [:<-game/action! action params])))}
       text])))

;; Action tray is a bit weird because bottom sheet is weird with a render
;; content function
(defn -action-tray [{:as props
                     :keys [dispatch sizing actions action-valid?]}]
  (let [dispatch (or dispatch (get props "dispatch"))
        actions (or actions (get props "actions"))
        action-valid? (or action-valid?
                          (fn [{:keys [disabled?]}] (not disabled?)))]
    [surface {:elevation 2
              :style {:background-color "rgba(20,87,155,0.12)"
                      :padding 10
                      :padding-top 2
                      :height "100%"}}
     ;; TODO: Ugly - use image or something
     [text {:style {:text-align "center" :font-size 12
                    :color "white"
                    :padding-top 8
                    :padding-bottom 8}} ": : :"]
     [card-content 
      [scroll-view
       (map #(vector -action-button (merge props {:key %
                                                  :action-valid? action-valid?})
                     %) actions)]]]))

(defn actions-list [{:as props
                     :or {from :actions
                          mode "outlined"}
                     :keys [dispatch from mode action-valid?]}]
  (let [actions       (get props from [])
        action-valid? (or action-valid?
                          (fn [{:keys [disabled?]}] (not disabled?)))]
    [surface {:style {:background-color "rgba(20,87,155,0.12)"
                      :elevation 2
                      :padding 8}}
     (map #(vector -action-button (merge props {:key %
                                                :mode mode
                                                :action-valid? action-valid?})
                   %) actions)]))

(defn bottom-sheet-fixed [{:keys [start-collapsed? collapse!]
                           :or {start-collapsed? false}
                           :as props}]
  (let [collapsed?   (r/atom (or start-collapsed? false))
        on-open      #(reset! collapsed? false)
        on-close     #(reset! collapsed? true)
        ref          (r/atom nil)
        do-collapse! (fn collapse-it
                       ([]
                        (do
                          (swap! collapsed? not)
                          (if @ref
                            ((aget @ref "snapTo") (if @collapsed? 1 0)))))
                       ([e]
                        (if (boolean? e)
                          (collapse-it e e)
                          (collapse-it)))
                       ([maybe e]
                        (do
                          (reset! collapsed? maybe)
                          (if @ref
                            ((aget @ref "snapTo") (if @collapsed? 1 0))))))

        collapse!  (doto (or collapse! (r/atom nil))
                    (reset! do-collapse!))
        dimensions (.get dimensions "screen")]
    (if web?
      [view
       [view {:style {:min-height "20vh"
                      :visibility "hidden"}}
        [-action-tray props]]
       [portal
        [view {:style {:position         "fixed"
                       :bottom           0
                       :background-color "rgba(0,0,0,0.2)"
                       :height           "20vh"
                       :min-height       "20vh"
                       :width            "100%"}}
         [-action-tray props]]]]
      [portal
       [bottom-sheet {:snap-points                      ["20%" 64]
                      :ref                              #(reset! ref %)
                      :initial-snap                     0
                      :enabled-bottom-initial-animation false
                      :enabled-content-tap-interaction  false
                      :render-content                   (fn [p] (r/as-element [-action-tray props]))}]])))

(defn card-with-button [display]
  (let [parent-layout (r/atom {"height" 40 "width" 40})
        child-layout (r/atom {"height" 0 "width" 0})]
    (fn -card-with-button [display]
      (let [x-carded? (get display :x-card-active?)
            card-data (get display :card)
            border-color (get display :border-color)
            rules?    (get-in card-data [:tags :rules])
            regen?    (get display :regen-action)
            available-actions (get display :available-actions #{})

            dispatch (get display :dispatch)
            child-height (get @child-layout "height" 0)
            parent-height (get @parent-layout "height" 41)

            overflow? (> (+ 60 child-height)
                         parent-height)
            inputs     (get-in display [:card :inputs] [])
            all-inputs (into {}
                             (map #(vector (:name %) (:value %))
                                  inputs))

            prompt-options (get-in card-data [:prompt-options])
            additional-prompts (get-in display [:additional-prompts])
            card-text (get-in card-data [:text])
           ;; Only used in walking-deck - maybe extract?
            full-text (if additional-prompts
                        (clojure.string/join "\n\n * * * * * \n"
                                             (concat (reverse additional-prompts) [card-text]))
                        card-text)]
        [card {:elevation 4
               :on-layout (fn [e]
                            (reset! parent-layout
                                   ;; oget doesn't work in dev?
                                    (js->clj (aget (aget e "nativeEvent") "layout"))))
               :style (merge
                       {:margin 4
                        :margin-bottom 10
                        :padding 12
                        :padding-bottom 0
                        :font-size 16
                        :border-width 4
                        :flex 1
                        :border-style "solid"
                        :border-color "#212121"}
                       (cond
                         x-carded?
                         {:border-color "red"}
                         border-color
                         {:border-color border-color}
                        ;; rules?
                        ;; {:border-color "blue"}
                         ))}
         [view {:style {:flex-direction "column"
                        :flex 1}}
          [scroll-view {:scroll-enabled overflow?
                        :shows-vertical-scroll-indicator (not web?)
                        :style {:flex 1}}
           [card-content {:on-layout (fn [e]
                                       (reset! child-layout
                                               (js->clj (aget (aget e "nativeEvent") "layout"))))
                          :class (str (name (get-in card-data [:state] "blank"))
                                      " "
                                      (if x-carded?
                                        "x-carded"))}
            [markdown {:style {:body {:font-size 22
                                      :font-family (if android? "serif" "Georgia")}}}
             full-text]
            (if-not (empty? prompt-options)
              [list-section
               (map (fn [{:keys [text label description name value selected? label generator]}]
                      [list-item {:key name
                                  :title label
                                  :description description
                                  :on-press (if-not selected?
                                              #(dispatch [:<-game/action! :choose-option name]))
                                  :left (fn [p]
                                          (r/as-element
                                           [list-icon (merge (js->clj p)
                                                             {:icon (if selected? "radiobox-marked" "radiobox-blank")})]))}])
                    prompt-options)])
            (if-not (empty? inputs)
              [list-section
               (map (fn [{:keys [name value label generator]}]
                      [list-item {:key name
                                  :title label
                                  :description value
                                  :on-press (if regen?
                                              #(dispatch [:<-game/action! :regen (dissoc all-inputs name)]))
                                  :right (if regen?
                                           (fn [p] (r/as-element
                                                    [list-icon (merge (js->clj p)
                                                                      {:icon "refresh"})])))}])
                    inputs)])]
           (if (available-actions :x-card)
             [button {;;:style {:margin-bottom -12}
                      :disabled x-carded?
                      :on-press #(dispatch [:<-game/action! :x-card])} "X Card"])
           (if overflow? [view {:style {:height 10}}])]
          (if overflow?
            [view {:style {:height 5}}])
          (if overflow?
            [linear-gradient {:colors ["rgba(0,0,0,0.0)", "rgba(0,0,0,0.01)", "rgba(0,0,0, 0.18)"]
                              :style {:border-radius 4
                                      :position "absolute"
                                      :bottom 0
                                      :width "100%"
                                      :height 18}
                              :start {:x 0 :y 0}
                              :end {:x 0 :y 1}
                              :pointer-events "none"}])]
         [card-actions
         ;; Maybe move pass here?
          #_(if (available-actions :pass)
              [button {:on-press #(dispatch [:<-game/action! :pass])} "Pass Card"])]]))))

(defn bottom-sheet-card [{:as props :keys [start-collapsed?
                                           collapse!]}]
  (let [collapsed?   (r/atom (or start-collapsed? false))
        on-open      #(reset! collapsed? false)
        on-close     #(reset! collapsed? true)
        ref          (r/atom nil)
        do-collapse! (fn collapse-it
                       ([]
                        (do
                          (swap! collapsed? not)
                          (if @ref
                            ((aget @ref "snapTo") (if @collapsed? 2 0)))))
                       ([e]
                        (if (boolean? e)
                          (collapse-it e e)
                          (collapse-it)))
                       ([maybe e]
                        (do
                          (reset! collapsed? maybe)
                          (if @ref
                            ((aget @ref "snapTo") (if @collapsed? 2 0))))))

        collapse! (doto (or collapse! (r/atom nil))
                    (reset! do-collapse!))]
    (fn [props]
      (let [dimensions           (.get dimensions "screen")
            action-sheet-props   (clj->js
                                  {:options                  ["Pass" "Discard" "Cancel"]
                                   :destructive-button-index 0
                                   :cancel-button-index      2})
            action-sheet-action  (fn [index]
                                   (println index))
            trigger-action-sheet (fn [e]
                                   (println "props" props)
                                   ((:showActionSheetWithOptions props)
                                    action-sheet-props
                                    action-sheet-action))]
        (if web?
          [portal
           [view {:style {:position         "fixed"
                          :bottom           (if @collapsed? "-55vh" 0)
                          ;; :background-color "rgba(0,0,0,0.9)"
                          :background-color "rgba(30,49,68,0.98)"
                          :height           "65vh"
                          :min-height       "65vh"
                          :width            "100%"}}
            [text {:hit-slop 20
                   :style    {:padding      6
                              :padding-left 12
                              :font-weight  "bold"
                              :color        "white"}
                   :on-press @collapse!}
             (:turn-marker props)]
            [fling-gesture-handler {:on-handler-state-change (fn [e]
                                                               (let [nativeEvent (aget e "nativeEvent")
                                                                     move-y      (aget nativeEvent "translationY")
                                                                     inside?     (aget nativeEvent "pointerInside")]
                                                                 (cond
                                                                   (> -50 move-y)
                                                                   (@collapse! false)
                                                                   (and inside? (< 50 move-y))
                                                                   (@collapse! true)
                                                                   ;; :else
                                                                   ;; (println e)
                                                                   )))
                                    :direction               (bit-or (.-UP gesture-directions) (.-DOWN gesture-directions))}
             [card-with-button props]]]]
          [portal
           [bottom-sheet {:snap-points                     ["56%" "28%" 64]
                          :ref                             #(reset! ref %)
                          :initial-snap                    (if @collapsed? 2 0)
                          :border-radius                   8
                          :on-close-end                    on-close
                          :on-open-start                   on-open
                          ;; animation forces 25% snappoint 
                          ;; :enabled-bottom-initial-animation true
                          :enabled-content-tap-interaction false
                          ;; :render-header (if (:turn-marker props)
                          ;;                  (fn [p]
                          ;;                   (r/as-element [text (:turn-marker props)])))
                          :render-content                  (fn [p]
                                                             (r/as-element [view {:style {:height           "100%"
                                                                                          :padding-top      6
                                                                                          ;; :background-color "rgba(0,0,0,0.9)"
                                                                                          :background-color "rgba(30,49,68,0.98)"
                                                                                          }}
                                                                            [text {:on-press @collapse!
                                                                                   :style    {:padding      6
                                                                                              :padding-left 12
                                                                                              :font-weight  "bold"
                                                                                              :color        "white"}}
                                                                             (:turn-marker props)]
                                                                            [card-with-button props]]))}]])))))

;; (def bottom-sheet-card (with-action-sheet (r/reactify-component -bottom-sheet-card)))
