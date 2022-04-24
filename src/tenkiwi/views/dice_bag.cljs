(ns tenkiwi.views.dice-bag
  (:require [tenkiwi.views.shared :as ui]))

(defn -with-log [{:keys [title]
                  :as   props}
                 {:keys [actions log current]}
                   dispatch]
  (let [{current-label :label current-text :text
         :or           {current-label "Click to Roll"
                        current-text  "(results show here)"}} current]
    [ui/view {:style {:margin-bottom 8}}
    (if title
      [ui/h1
       {:theme {:colors {:text "white"}}
        :style {:padding-left   4
                :padding-bottom 8}}
       title])
     [ui/card
      [ui/card-title {:title current-label :subtitle current-text}]
      [ui/card-actions
       (map-indexed (fn [id {:keys [action params text]}]
                      [ui/button
                       {:key      (str action "-" id)
                        :style    {:flex 1}
                        :on-press #(dispatch [:<-game/action! action params])}
                       text])
                    actions)]
      [ui/card-content
       [ui/list-accordion
        {:title "Roll Log"}
        (map-indexed (fn [id {:keys [label text]}]
                       [ui/list-item
                        {:key         (str "log-" id)
                         :title       label
                         :description text}])
                     log)]]]]))
