(ns tenkiwi.views.debrief
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r :refer [atom]]
            [tenkiwi.views.shared :as ui :refer [scroll-view view para text button]]))


(defn -player-scoreboard-entry [display player-scores current-user-id player]
  (let [{:keys [id user-name dead? agent-name agent-codename agent-role]} player
        dispatch (:dispatch display)
        total-score (apply + (vals (player-scores id)))]
    [view
     [para {:title agent-name}
      [text (str "[ " total-score " ] " (if agent-name (str agent-codename ", " agent-role " ")) " (" user-name ")")]]
     [view
      ;; TODO - maybe this logic should come from gamemaster
      (if-not (= id current-user-id)
        [button
         {:on-press #(dispatch [:<-game/action! :downvote-player {:player-id id}])}
         [text " - "]])
      [text
       (str (get-in player-scores [id current-user-id]))]
      (if-not (= id current-user-id)
        [button
         {:on-press #(dispatch [:<-game/action! :upvote-player {:player-id id}])}
         [text " + "]])
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
                   stage-name
                   stage-focus
                   all-players
                   player-ranks
                   player-scores
                   company
                   players-by-id
                   mission
                   dossiers]}             game

           all-players    (map #(merge % (get dossiers (:id %) {}))
                               all-players)
           voting-active? (if-not (#{:intro} stage)
                            true
                            false)

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
                          :dispatch dispatch
                          :action-valid? valid-button?)
           on-tab-change (fn [x] (reset! tab-state x))
           current-index @tab-state
           sizing {:min-height (.-height dimensions)
                   :width (.-width dimensions)
                   :overflow-y "scroll"}
           ]
       [scroll-view
        [view
         [view
          [view
           [para (str stage-name)]
           [para (str stage-focus)]]
          [ui/card-with-button display]
          [ui/bottom-sheet-fixed display]]
         ]
        [view
         [view
          (if voting-active?
            (map (fn [player]
                   (with-meta
                     [-player-scoreboard-entry display player-scores user-id player]
                     {:key (:id player)}))
                 all-players))]
         #_[:div.company
          [:h2 "Round Themes"]
          [:ul
           (map
            (fn [val] (with-meta [:li val] {:key val}))
            (:values company))]]
         #_(if voting-active?
           [:div.mission-details
            [:h2 "More Details"]
            [:p (str (:text mission))]])
         #_(if (and voting-active? extra-details)
           [:div.extra-details
            (map (fn [{:keys [title items]}]
                   (with-meta
                     [:div.detail
                      [:h2 title]
                      [:ul
                       (map #(with-meta [:li %] {:key %}) items)]]
                     {:key title}))
                 extra-details
                 )])
         (map (fn [{conf  :confirm
                    :keys [action class text]}]
                (with-meta (vector button
                                   {:class class
                                    :on-press (fn [] (ui/maybe-confirm! conf #(dispatch [:<-game/action! action])))}
                                   text)
                  {:key action}))
              (get-in display [:extra-actions]))]]))))
