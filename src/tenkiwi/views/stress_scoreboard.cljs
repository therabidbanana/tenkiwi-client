(ns tenkiwi.views.stress-scoreboard
  (:require [tenkiwi.views.shared :as ui]))

(defn -player-scoreboard-entry [display]
  (let [{:keys [id text label actions
                score max-score dispatch]} display]
    [ui/surface {:style {:margin         4
                         :flex-direction "row"
                         :align-items    "center"}}
     [ui/view {:style {:flex        1
                       :padding     4
                       :align-items "center"}}
      [ui/h1 {} (str score)]]
     [ui/view {:style {:flex    6
                       :padding 4}}
      [ui/view {:style {:border-bottom-style "dashed"
                        :border-bottom-color "#bebebe"
                        :border-bottom-width 1
                        :padding             4}}
       [ui/h2 {} label]
       (if text
         [ui/markdown {} text])
       (if max-score
         [ui/progressbar {:progress (/ score max-score)} ])
       ]
      [ui/view {:style {:flex-direction "row"
                        :align-items    "center"}}
       (map (fn [{:keys [action params text]}]
              [ui/button
               {:key      (str action "-" id)
                :style    {:flex 1}
                :on-press #(dispatch [:<-game/action! action params])}
               text])
            actions)
       ]]]))

(defn -scoreboard [{:keys [title]
                    :as   props}
                   scoreboard-display
                   dispatch]
  [ui/view {}
   (if title
     [ui/h1
      {:theme {:colors {:text "white"}}
       :style {:padding-top  4
               :padding-left 4}}
      title])
   [ui/view
    (map (fn [player]
           (with-meta
             [-player-scoreboard-entry (assoc player :dispatch dispatch)]
             {:key (:id player)}))
         scoreboard-display)]])
