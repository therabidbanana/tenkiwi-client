(ns tenkiwi.views.walking-deck
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
                           :default-value     (get params name)}])])
      inputs)
     [ui/button {:mode "outlined"
                 :on-press #(if (and (not disabled?)
                                     (or (not conf) (js/confirm "Are you sure?")))
                              (dispatch [:<-game/action! action params]))}
      text]]))

(re-frame/reg-sub
 :walking-deck-other
 (fn [db]
   (extract-display (:user db)
                    [:stage :company :mission])))

(defn build-other-panel [form-atom game-state-atom dispatch]
  (fn -other-panel []
    (let [{:keys [extra-actions]} (:display @game-state-atom)
          {:keys [company stage mission]} @game-state-atom
          dimensions (.get ui/dimensions "screen")]

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
        [ui/text ""]]])))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:walking-deck-other])
        forms (re-frame/subscribe [:forms])]
    (build-other-panel forms game-state re-frame/dispatch)))

(re-frame/reg-sub
 :walking-deck-cast
 (fn [db]
   (extract-display (:user db)
                    [:active-player :ready-players :next-players])))

(defn -cast-member [ready-players
                    {:keys [active? id user-name dead? character] :as player}]
  (let [ready? (ready-players id)
        player-name (str
                     ;; (if active? "* ")
                     (if ready? " ✅ ")
                     (:title character) " (" user-name ")"
                     (if dead? " 💀"))]
    [ui/card {:style {:padding 8}}
     [ui/text player-name]]))

(defn build-cast-panel [game-state-atom dispatch]
  (fn -cast-panel []
    (let [{:keys [ready-players
                  active-player
                  next-players]} @game-state-atom
          dimensions             (.get ui/dimensions "screen")
          players                (cons (assoc active-player :active? true)
                                       next-players)
          _ (println players)]
      [ui/scroll-view
       (map-indexed (fn [i p] (with-meta [-cast-member (or ready-players {}) p] {:key i}))
                    (sort-by :order players))
       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]])))

(defn cast-panel []
  (let [game-state (re-frame/subscribe [:walking-deck-cast])]
    (build-cast-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :walking-deck-main
 (fn [db]
   (extract-display (:user db)
                    [:act :act-prompt])))

(defn build-main-panel [game-state-atom dispatch]
  (fn -main-panel []
    (let [{:keys [act
                  act-prompt
                  current-user-id
                  player-ranks]
           {:as display
            :keys [extra-details]} :display
           :as game-state} @game-state-atom

          box-style {:margin-top 8 :padding 10}
          dimensions (.get ui/dimensions "screen")
          prompt-options (get-in display [:card :prompt-options])

          valid-button? (fn [{:keys                 [action params disabled?]
                              {:keys [id rank act]} :params
                              :as                   button}]
                          (cond
                            (#{:done} action)
                            (and
                             (not disabled?)
                             (or (empty? prompt-options) (some :selected? prompt-options)))
                            :else
                            (not disabled?)))]
      [ui/scroll-view
       [ui/view
        [ui/view
         [ui/para {:theme {:colors {:text "white"}}
                   :style {:padding-top 4
                           :padding-left 8}}
          (if (> act 3)
            (str "End Game")
            (str act-prompt))]]
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
                (partition-all 2 extra-details))])
        [ui/view {:height (* 0.7 (.-height dimensions))}
         [ui/text ""]]]])))

(defn main-panel []
  (let [game-state (re-frame/subscribe [:walking-deck-main])]
    (build-main-panel game-state re-frame/dispatch)))

(defn walking-deck-game-panel []
  (let [tab-state (r/atom 0)
        scene-map (ui/SceneMap (clj->js {:main (r/reactify-component main-panel)
                                         :cast (r/reactify-component cast-panel)
                                         :other (r/reactify-component other-panel)}))]
    (fn []
      (let [on-tab-change (fn [x] (reset! tab-state x))
            current-index @tab-state]
        [ui/clean-tab-view
         {:on-index-change on-tab-change
         ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
          :navigation-state {:index current-index
                             :routes [{:key "main"
                                       :title "Main"}
                                      {:key "cast"
                                       :title "Cast"}
                                      {:key "other"
                                       :title "Extras"}]}
          :render-scene scene-map}]))))
