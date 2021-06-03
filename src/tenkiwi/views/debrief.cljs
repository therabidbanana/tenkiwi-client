(ns tenkiwi.views.debrief
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [tenkiwi.views.shared :as ui]))

(defn -player-scoreboard-entry [display player-scores player]
  (let [{:keys [id user-name dead? agent-name agent-codename agent-role]} player
        dispatch (:dispatch display)
        current-user-id (:current-user-id display)
        total-score (apply + (vals (player-scores id)))]
    [ui/card {:style {:margin 8}}
     [ui/card-content
      [ui/para {:title agent-name}
       (str "[ " total-score " ] " (if agent-name (str agent-codename ", " agent-role " ")) " (" user-name ")")]]
     [ui/card-actions
      ;; TODO - maybe this logic should come from gamemaster
      (if-not (= id current-user-id)
        [ui/button
         {:on-press #(dispatch [:<-game/action! :downvote-player {:player-id id}])}
         " - "])
      [ui/text
       (str (get-in player-scores [id current-user-id]))]
      (if-not (= id current-user-id)
        [ui/button
         {:on-press #(dispatch [:<-game/action! :upvote-player {:player-id id}])}
         " + "])
      ]]))

(defn -other-panel [{:keys []}
                    {:keys [dispatch extra-actions] :as display}]
  [ui/view
   (map (fn [{conf  :confirm
              :keys [action class text]}]
          (with-meta (vector ui/button
                             {:class class
                              :on-press (fn [] (ui/maybe-confirm! conf #(dispatch [:<-game/action! action])))}
                             text)
            {:key action}))
        extra-actions)])

(defn -scoreboard-panel [{:keys [dossiers all-players
                                 stage player-scores
                                 company mission]
                          :as game}
                         display]
  (let [all-players    (map #(merge % (get dossiers (:id %) {}))
                            all-players)

        voting-active? (if-not (#{:intro} stage)
                         true
                         false)
        box-style {:margin-top 8 :padding 10}
        dimensions (.get ui/dimensions "window")]

    [ui/scroll-view
     [ui/view 
      (if voting-active?
        (map (fn [player]
               (with-meta
                 [-player-scoreboard-entry display player-scores player]
                 {:key (:id player)}))
             all-players))]
     [ui/surface {:style box-style}
        [ui/h1 "Round Themes"]
        [ui/view
         (map
          (fn [val] (with-meta [ui/para val] {:key val}))
          (:values company))]]
     (if voting-active?
       [ui/surface {:style box-style}
        [ui/h1 "More Details"]
        [ui/markdown (str (:text mission))]])
     [ui/view {:height (* 0.2 (.-height dimensions))}
      [ui/text ""]]
     ]))

(defn -main-panel [{:keys [stage stage-name stage-focus]}
                   {:as display
                    :keys [extra-details]}]
  (let [voting-active? (if-not (#{:intro} stage)
                         true
                         false)
        box-style {:margin-top 8 :padding 10}
        dimensions (.get ui/dimensions "window")]
    [ui/scroll-view
     [ui/view
      [ui/view
       [ui/para (str stage-name)]
       [ui/para (str stage-focus)]]
      [ui/card-with-button display]
      [ui/bottom-sheet-fixed display]
      (if (and voting-active? extra-details)
        [ui/view
         (map (fn [{:keys [title items]}]
                (with-meta
                  [ui/surface {:style box-style}
                   [ui/h1 title]
                   [ui/view
                    (map #(with-meta [ui/para %] {:key %}) items)]]
                  {:key title}))
              extra-details
              )])
      [ui/view {:height (* 0.2 (.-height dimensions))}
       [ui/text ""]]
      ]]))

(defn -debrief-game-panel [user-data dispatch]
  (let [tab-state (r/atom 0)
        dimensions (.get ui/dimensions "window")]
    (fn [user-data dispatch]
     (let [{user-id        :id
            :as            data
            {:as   room
             :keys [game]} :current-room} @user-data
           active?                        (= user-id (:id (:active-player game)))
           {:keys [stage
                   player-ranks
                   player-scores
                   company
                   players-by-id
                   mission
                   dossiers]}             game

           {:keys [extra-details]
            :as   display} (if active?
                             (:active-display game)
                             (:inactive-display game))
           x-carded?       (:x-card-active? display)

           ;; TODO - hide self-vote or push to server
           self-vote?    (fn [{:keys                 [action params]
                               {:keys [id rank act]} :params
                               :as                   button}]
                           (and (#{:rank-player} action)
                                (= user-id id)))
           valid-button? (fn [{:keys                 [action params disabled?]
                               {:keys [id rank act]} :params
                               :as                   button}]
                           (cond
                             (#{:rank-player} action)
                             (and
                              (not= user-id id)
                              (nil? (get-in player-ranks [user-id act rank]))
                              (not= id (get-in player-ranks [user-id act :best])))
                             :else
                             (not disabled?)))

           display (assoc display
                          :current-user-id user-id
                          :dispatch dispatch
                          :action-valid? valid-button?)
           on-tab-change (fn [x] (reset! tab-state x))
           current-index @tab-state
           sizing {:min-height (.-height dimensions)
                   :width (.-width dimensions)}

           score-state (select-keys game
                                         [:dossiers :all-players
                                          :stage :player-scores
                                          :company :mission])
           main-state (select-keys game [:stage-name
                                         :stage :stage-focus])
           ]
       [ui/view {:style sizing}
        [ui/tab-view
         {:initial-layout sizing
          :scroll-enabled true
          :on-index-change on-tab-change
          ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
          :navigation-state {:index current-index
                             :routes [{:key "main"
                                       :title " "}
                                      {:key "scoreboard"
                                       :title " "}
                                      {:key "other"
                                       :title " "}]}
          ;; Does this mess up performance because of as-element? Maybe we should reactify-component instead?
          ;; Main problem is likely that render scene has to change each time to update display / main-state vars?
          ;; If we could make them part of props that'd be better.
          :render-scene (fn [props]
                          (let [key (.. props -route -key)]
                            (case key
                              "main"
                              (r/as-element [-main-panel main-state display])
                              "scoreboard"
                              (r/as-element [-scoreboard-panel score-state display])
                              "other"
                              (r/as-element [-other-panel {} display])
                              (r/as-element [ui/text "WHAAAA"])
                              )
                            ))}
         ]
        ]))))
