(ns tenkiwi.views.wordbank
  (:require [tenkiwi.views.shared :as ui]))

(defn -wordbank [{:keys [box-style]
                  :or {box-style {:margin-top 8 :padding 10}}}
                 extra-details]
  (if extra-details
    [ui/view {:style {:padding 2
                      :padding-top 8}}
     (map (fn [[{title1 :title items1 :items}
                {title2 :title items2 :items}]]
            (with-meta
              [ui/view {:flex-direction "row"}
               (if title1
                 [ui/surface {:style (assoc box-style
                                            ;; :background-color "rgba(20,87,155,0.12)"
                                            :elevation 1
                                            :margin 4
                                            :flex 1)}
                  [ui/h1 title1]
                  [ui/view
                   (map #(with-meta [ui/para %] {:key %}) items1)]])
               (if title2
                 [ui/surface {:style (assoc box-style
                                            ;; :background-color "rgba(20,87,155,0.12)"
                                            :elevation 1
                                            :margin 4
                                            :flex 1)}
                  [ui/h1 title2]
                  [ui/view
                   (map #(with-meta [ui/para %] {:key %}) items2)]])]
              {:key (str title1 title2)}))
          (partition-all 2 extra-details))]))
