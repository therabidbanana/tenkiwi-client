(ns tenkiwi.games.push
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [oops.core :refer [oget oset!]]
            [goog.object]
            [tenkiwi.views.stress-scoreboard :as stress]
            [tenkiwi.views.oracle-box :as oracle-box]
            [tenkiwi.views.wordbank :as wordbank]
            [tenkiwi.views.shared :as ui]))

(defonce do-collapse! (r/atom (fn [])))

(defn- extract-display [game-state key-list]
  (let [{user-id        :id
         :as            data
         {:as   room
          :keys [game]} :current-room} game-state
        active? (= user-id (get-in game [:display :active-player :id]))
        display (if active?
                  (:active-display game)
                  (:inactive-display game))]
    (merge {:current-user-id user-id}
           (select-keys game key-list)
           {:display (merge display (:display game))})))

(re-frame/reg-sub
 :push-other
 (fn [db]
   (extract-display (:user db)
                    [:episode])))

(defn build-other-panel [game-state-atom dispatch]
  (fn -other-panel []
    (let [{:keys [episode display]} @game-state-atom
          dimensions        (.get ui/dimensions "screen")
          ]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       [ui/card {:style {:margin  8}}
        [ui/card-title {:title (str "Episode Details")} ]
        [ui/card-content
         [ui/markdown {} (str (:mission-text episode))]]
        [ui/card-title {:subtitle (str "Your Agenda")}]
        [ui/card-content {}
         [ui/markdown {} (str (:agenda-text episode))]]]
       [ui/actions-list (merge display {:mode "outlined"
                                        :dispatch dispatch
                                        :from :extra-actions})]

       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]])))

(defn other-panel []
  (let [game-state (re-frame/subscribe [:push-other])]
    (build-other-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :push-characters
 (fn [db]
   (extract-display (:user db) [:stage :phase])))

(defn build-character-panel [game-state-atom dispatch]
  (fn -character-panel []
    (let [{:keys [current-user-id]
           {:as display
            :keys [sheets
                   oracle-box
                   phase
                   sheets
                   player-names
                   intro-cards
                   story-details]} :display
           :as game-state} @game-state-atom
          dimensions (.get ui/dimensions "screen")]
      [ui/collapse-scroll-view {:collapse! do-collapse!
                                :style {:padding 12}}
       [ui/accordion-group {}
        (map (fn [[id sheet]]
               [ui/list-accordion {:key id
                                   :id id
                                   :title (or (get player-names id)
                                              (str "Player: " (get-in sheets [id :user-name])))}
                [ui/card {}
                 [ui/card-title {:subtitle (str "Player: " (get-in sheets [id :user-name]))}]
                 [ui/card-content {}
                  [ui/list-section {}
                   (map (fn [{:keys [name label value]}]
                          [ui/list-item {:key name :title value :description label}])
                        (:inputs sheet))]]]]
               )
             intro-cards)]
       [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
        [ui/text ""]]])))

(defn character-panel []
  (let [game-state (re-frame/subscribe [:push-characters])]
    (build-character-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :push-dice
 (fn [db]
   (extract-display (:user db) [:stage :phase])))

(defn build-dice-panel [game-state-atom dispatch]
  (fn -dice-panel []
    (let [{:keys [current-user-id]
           {:as display
            :keys [player-sheets
                   oracle-box
                   phase
                   active-player
                   story-details]} :display
           :as game-state} @game-state-atom
          active-player  (merge active-player (get player-sheets (:id active-player) {}))
          dimensions (.get ui/dimensions "screen")]
      [ui/collapse-scroll-view {:collapse! do-collapse!
                                :style {:padding 12}}
       [ui/accordion-group
        [ui/list-accordion {:id "oracle-actions"
                            :key "oracle-actions"
                            :title "Take Action"}
         [oracle-box/box-with-animation {:id :action :hide-log? true} (:action oracle-box) dispatch]]
        [ui/list-accordion {:id "oracle-oracle"
                            :key "oracle-oracle"
                            :title "Ask a Question"}
         [oracle-box/box-with-animation {:id :oracle :hide-log? true} (:oracle oracle-box) dispatch]]]
       [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
        [ui/text ""]]])))

(defn dice-panel []
  (let [game-state (re-frame/subscribe [:push-dice])]
    (build-dice-panel game-state re-frame/dispatch)))

(re-frame/reg-sub
 :push-main
 (fn [db]
   (extract-display (:user db) [:stage :phase :matrix :scene-count])))

(defn build-main-panel [game-state-atom dispatch]
  (fn -main-panel []
    (let [{:keys [current-user-id scene-count]
           {:as display
            :keys [matrix
                   challenge
                   player-sheets
                   phase
                   oracle-box
                   card
                   active-player
                   story-details]} :display
           :as game-state} @game-state-atom
          phase-names {:encounter "Determine the Challenge"
                       :descriptions "Set the Scene"
                       :actions "Take Action"}
          scene-count (or scene-count 1 )
          active-player  (merge active-player (get player-sheets (:id active-player) {}))
          dimensions (.get ui/dimensions "screen")
          display    (cond-> display
                       (get-in card [:tags :challenge])
                       (assoc-in [:border-color] "orange"))]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       [ui/view
        [ui/card {:style {:margin 8}}
         [ui/progressbar {:progress (/ scene-count 12)
                          :color    :blue}]
         [ui/card-title {:title matrix
                         :title-number-of-lines 2
                         :subtitle (str "Challenge - " challenge " / " (phase-names phase))}]
         [ui/card-content {}
          [ui/actions-list (assoc display
                                  :dispatch dispatch
                                  :hide-invalid? true)]]]
        [wordbank/-wordbank {} story-details]
        [ui/bottom-sheet-card
         (assoc display
                :collapse! do-collapse!
                :dispatch dispatch
                :turn-marker (str (:user-name active-player) "'s turn...")
                :regen-action true)]
        [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
         [ui/text ""]]]])))

(defn main-panel []
  (let [game-state (re-frame/subscribe [:push-main])]
    (build-main-panel game-state re-frame/dispatch)))


(re-frame/reg-sub
 :push-intro
 (fn [db]
   (extract-display (:user db) [:stage :episode :ready-players])))

(defn build-intro-panel [game-state-atom dispatch]
  (fn -intro-panel []
    (let [{:keys [current-user-id
                  episode
                  ready-players]
           {:as display
            :keys [player-sheets
                   stage
                   dice-bag
                   intro-cards
                   card
                   active-player
                   story-details]} :display
           :as game-state} @game-state-atom
          ready-players  (or ready-players {})
          active-player  (merge active-player (get player-sheets (:id active-player) {}))
          dimensions (.get ui/dimensions "screen")]
      [ui/collapse-scroll-view {:collapse! do-collapse!}
       (if (#{:intro} stage)
         [ui/card {:style {:margin 12}}
          [ui/card-title {:title "Introduction"
                          :subtitle "Setting and Gameplay"}]
          (map (fn [{:keys [tags text id]
                     :as card}]
                 (cond
                   (get tags :image) [ui/card-cover {:key (str "intro-" id)
                                                 :source {:uri text}} ]
                   :else
                   [ui/card-content {:key (str "intro-" id)}
                    [ui/markdown {} (str text)]]))
               (remove #(#{"character"} (:concept %)) (:intro episode)))])
       (if (#{:character} stage)
         [ui/view {:style {:padding 12}}
          [ui/card-with-button {:regen-action true
                                :dispatch dispatch
                                :card (get intro-cards current-user-id)}]])
       (if (#{:mission} stage)
         [ui/view {}
          [ui/card {:style {:margin 12}}
           [ui/card-title {:title "Episode"
                           :subtitle "This time on..."}]
           [ui/card-content
            [ui/markdown {}
             (str (:mission-text episode))]]]
          [ui/card {:style {:margin 12}}
           [ui/card-title {:title "Agenda"
                           :subtitle "What you're trying to do"}]
           [ui/card-content
            [ui/markdown {}
             (str (:agenda-text episode))]]]])
       [ui/bottom-sheet-fixed
        (assoc display
               :collapse! do-collapse!
               :action-valid? #(not (ready-players current-user-id))
               :dispatch dispatch
               :regen-action true)]
       [ui/view {:style {:height (* 0.7 (.-height dimensions))}}
        [ui/text ""]]])))

(defn intro-panel []
  (let [game-state (re-frame/subscribe [:push-intro])]
    (build-intro-panel game-state re-frame/dispatch)))

(defn push-game-panel []
  (let [tab-state (r/atom 0)
        dimensions (.get ui/dimensions "screen")
        game-state (re-frame/subscribe [:push-main])
        scene-map (ui/SceneMap (clj->js {:main (r/reactify-component main-panel)
                                         :dice (r/reactify-component dice-panel)
                                         :characters (r/reactify-component character-panel)
                                         :other (r/reactify-component other-panel)}))]
    (fn []
      (let [dimensions (.get ui/dimensions "screen")
            on-tab-change (fn [x] (reset! tab-state x))
            current-index @tab-state
            stage (get-in @game-state [:display :stage])]
        (cond
          (#{:intro :character :mission} stage)
          [intro-panel]
          #_[ui/clean-tab-view
           {:dimensions dimensions
            :scroll-enabled true
            :on-index-change on-tab-change
            :navigation-state {:index current-index
                               :routes [{:key "main"
                                         :title "Main"
                                         :icon "play-circle-outline"}
                                        {:key "other"
                                         :title "Extras"
                                         :icon "more-horiz"}]}
            :render-scene scene-map}]
          :else
          [ui/clean-tab-view
           {:dimensions dimensions
            :scroll-enabled true
            :on-index-change on-tab-change
            :navigation-state {:index current-index
                               :routes [{:key "main"
                                         :title "Main"
                                         :icon "play-circle-outline"}
                                        {:key "characters"
                                         :title "Players"
                                         :icon "bar-chart"}
                                        {:key "dice"
                                         :title "Oracles"
                                         :icon "bar-chart"}
                                        {:key "other"
                                         :title "Extras"
                                         :icon "more-horiz"}]}
            :render-scene scene-map}]

          )))))
