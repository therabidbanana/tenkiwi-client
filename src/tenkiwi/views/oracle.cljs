(ns tenkiwi.views.oracle
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [oops.core :refer [oget]]
            [tenkiwi.views.shared :as ui]))


(defn- extract-display [game-state key-list]
  (let [{user-id        :id
         :as            data
         {:as   room
          :keys [game]} :current-room} game-state
        active? (= user-id (get-in game [:active-player :id]))
        display (if active?
                  (:active-display game)
                  (:inactive-display game))]
    (merge {:current-user-id user-id}
           (select-keys game key-list)
           {:display display})))

(defn oracle-form-panel [form-atom form-config dispatch]
  (let [{conf      :confirm
         disabled? :disabled
         :keys     [action class text params inputs]
         :as       action-form} form-config

        params (merge params
                      (-> form-atom
                          deref
                          (get action {})))

        update-val  (fn [name val]
                      (dispatch [:forms/set-params (assoc {:action action}
                                                          name val)]))
        form-option (fn [name val]
                      [ui/chip
                       {:on-press #(update-val name val)
                        :key    val
                        :selected (= val (get params name))}
                       val])
        tag-option  (fn [name val]
                      (let [current-vals (into #{} (get params name))
                            selected?    (if (current-vals val)
                                           true false)
                            with-val     (conj current-vals val)
                            without-val  (disj current-vals val)]
                        [ui/chip
                         {:on-press #(if selected?
                                               (update-val name without-val)
                                               (update-val name with-val))
                          :key (str name val)
                          :selected selected?}
                         val]))]
    [ui/card
     #_{:on-submit #(if (and (not disabled?)
                           (or (not conf) (js/confirm "Are you sure?")))
                    (do
                      (dispatch [:<-game/action! action params])
                      (.preventDefault %)))}
     (map
      (fn [{:keys [type label name options value nested]}]
        [ui/view {:key name}
         (cond
           (#{:select} type)
           [ui/card {}
            [ui/card-content 
             [ui/text label]
             (if (map? options)
               (map (fn [[group-name opts]]
                      (if (or
                           (and nested (#{(get params nested)} group-name))
                           (nil? nested))
                        [ui/view {:key group-name
                                  :label group-name}
                         (map #(form-option name %) opts)]))
                    options)
               (map #(form-option name %) options))]]
           (#{:tag-select} type)
           [ui/card {}
            [ui/card-content 
             [ui/text label]
             (if (map? options)
               (map (fn [[group-name opts]]
                      (if (or
                           (and nested (#{(get params nested)} group-name))
                           (nil? nested))
                        [ui/view {:label group-name
                                  :key group-name}
                         (map #(with-meta (tag-option name %) {:key %}) opts)])) options)
               (map #(with-meta (tag-option name %) {:key %}) options))]]
           :else
           [ui/text-input {:on-change-text #(update-val name %)
                           :label     label
                           :name      name
                           :default-value     (get params name)}])
         ])
      inputs)
     [ui/button {:mode "outlined"
                 :on-press #(if (and (not disabled?)
                                     (or (not conf) (js/confirm "Are you sure?")))
                              (dispatch [:<-game/action! action params]))}
      text]]))

(re-frame/reg-sub
 :oracle-other
 (fn [db]
   (extract-display (:user db)
                    [:stage :company :mission])))

(defn build-other-panel [form-atom game-state-atom dispatch]
  (fn -other-panel []
    (let [{:keys [extra-actions]} (:display @game-state-atom)
          {:keys [company stage mission]} @game-state-atom
          dimensions (.get ui/dimensions "screen")
          ]

      [ui/scroll-view
       [ui/card {:style {:padding 4
                         :margin 8}}
        (map (fn [{conf      :confirm
                   disabled? :disabled
                   :keys     [action class text params inputs]
                   :as       action-form}]
               (if inputs
                 (with-meta
                   [oracle-form-panel form-atom action-form dispatch]
                   {:key (str action params)})
                 (vector ui/button
                         {:class class
                          :key (str action params)
                          :mode "outlined"
                          :on-press (fn [] (ui/maybe-confirm! conf #(dispatch [:<-game/action! action params])))}
                         text)))
             (remove :inputs extra-actions))]

       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]]
      )))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:oracle-other])
        forms (re-frame/subscribe [:forms])]
    (build-other-panel forms game-state re-frame/dispatch)))

(re-frame/reg-sub
 :oracle-decks
 (fn [db]
   (extract-display (:user db)
                    [])))

(defn build-deck-panel [form-atom game-state-atom dispatch]
  (fn -deck-panel []
    (let [{:keys [extra-actions]} (:display @game-state-atom)
          dimensions (.get ui/dimensions "screen")
          ]

      [ui/scroll-view
       [ui/card {:style {:padding 4
                         :margin 8}}
        (map (fn [{conf      :confirm
                   disabled? :disabled
                   :keys     [action class text params inputs]
                   :as       action-form}]
               (if inputs
                 (with-meta
                   [oracle-form-panel form-atom action-form dispatch]
                   {:key (str action params)})
                 (vector ui/button
                         {:class class
                          :key (str action params)
                          :mode "outlined"
                          :on-press (fn [] (ui/maybe-confirm! conf #(dispatch [:<-game/action! action params])))}
                         text)))
             (filter :inputs extra-actions))]

       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]]
      )))

(defn deck-panel []
  (let [game-state (re-frame/subscribe [:oracle-other])
        forms (re-frame/subscribe [:forms])]
    (build-deck-panel forms game-state re-frame/dispatch)))

(re-frame/reg-sub
 :oracle-main
 (fn [db]
   (extract-display (:user db)
                    [:stage :stage-name :stage-focus
                     :player-ranks])))

(defn build-main-panel [game-state-atom dispatch]
  (fn -main-panel []
    (let [{:keys [stage
                  stage-name
                  stage-focus
                  current-user-id
                  player-ranks]
           {:as display
            :keys [extra-details]} :display
           :as game-state} @game-state-atom
          
          box-style {:margin-top 8 :padding 10}
          dimensions (.get ui/dimensions "screen")

          valid-button? (fn [{:keys                 [action params disabled?]
                              {:keys [id rank act]} :params
                              :as                   button}]
                          (not disabled?))
          ]
        [ui/scroll-view
         [ui/view
          [ui/view
           [ui/para {:theme {:colors {:text "white"}}
                     :style {:padding-top 4
                             :padding-left 8}}
            (str stage-name "\n" stage-focus)]]
          #_[ui/card-with-button (assoc display :dispatch dispatch)]
          [ui/actions-list (assoc display
                                  :dispatch dispatch
                                  :action-valid? valid-button?)]
          [ui/bottom-sheet-card
           (assoc display :dispatch dispatch)]
          (if extra-details
            [ui/view {:style {:padding 2
                              :padding-top 8}}
             (map (fn [[{title1 :title items1 :items}
                        {title2 :title items2 :items}]]
                    (with-meta
                      [ui/view {:flex-direction "row"}
                       (if title1
                         [ui/surface {:style (assoc box-style
                                                    :background-color "rgba(150,150,190,0.7)"
                                                    :margin 4
                                                    :flex 1)}
                          [ui/h1 title1]
                          [ui/view
                           (map #(with-meta [ui/para %] {:key %}) items1)]])
                       (if title2
                         [ui/surface {:style (assoc box-style
                                                    :background-color "rgba(150,150,190,0.7)"
                                                    :margin 4
                                                    :flex 1)}
                          [ui/h1 title2]
                          [ui/view
                           (map #(with-meta [ui/para %] {:key %}) items2)]])]
                      {:key (str title1 title2)}))
                  (partition-all 2 extra-details)
                  )])
          [ui/view {:height (* 0.7 (.-height dimensions))}
           [ui/text ""]]
          ]])))

(defn main-panel []
  (let [game-state (re-frame/subscribe [:oracle-main])]
    (build-main-panel game-state re-frame/dispatch)))


(defn oracle-game-panel []
  (let [tab-state (r/atom 0)
        dimensions (.get ui/dimensions "screen")
        scene-map (ui/SceneMap (clj->js {:main (r/reactify-component main-panel)
                                         :decks (r/reactify-component deck-panel)
                                         :other (r/reactify-component other-panel)}))]
    (fn []
     (let [;; "window" dimensions wrong to start sometimes - height 36?
           ;;  note: useWindowDimensions hook did _not_ prevent this problem
           ;;  most likely reagent deferring a render and causing window to be small?
           dimensions (.get ui/dimensions "screen")
           on-tab-change (fn [x] (reset! tab-state x))
           current-index @tab-state
           sizing (if ui/web?
                    {:min-height (.-height dimensions)
                     :width "100%"}
                    {:min-height (.-height dimensions)
                     :width (.-width dimensions)})
           ]
       [ui/view {:style sizing}
        [ui/tab-view
         {:initial-layout (if-not ui/web?
                            (assoc sizing :height (.-height dimensions)))
          :scroll-enabled true
          :on-index-change on-tab-change
          ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
          :navigation-state {:index current-index
                             :routes [{:key "main"
                                       :title " "}
                                      {:key "decks"
                                       :title " "}
                                      {:key "other"
                                       :title " "}]}
          :render-scene scene-map}
         ]
        ]))))
