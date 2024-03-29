(ns tenkiwi.views.home-screen
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [oops.core :refer [oget]]
            [clojure.string :as str]
            [tenkiwi.views.shared :as ui]))


(defn secure-rand-id [alphabet number]
  (str (str/join "" (take number (shuffle alphabet)))
       ""
       (str/join "" (take number (shuffle alphabet)))))

(defonce do-collapse! (r/atom (fn [])))

(defn -join-panel [form-state join dispatch]
  (let [random-room (secure-rand-id "abcdefghijklmnopqrstuvwxyz23456789"
                                    2)
        dimensions (.get ui/dimensions "screen")]
    [ui/collapse-scroll-view {:style {:padding 24}
                              ;:collapse! do-collapse!
                              :only-collapse! do-collapse!}
     [ui/card {:style {:padding 18}}
      [ui/card-title {:title "What's your name?"}]
      [ui/card-content
       [ui/view
        [ui/text-input {:name           "game-user-name"
                        :label          "Name"
                        :mode           "outlined"
                        :default-value  (-> join deref :user-name)
                        :on-change-text #(dispatch [:join/set-params {:user-name %}])}]
        (cond
          (= :name @form-state)
          [ui/view
           [ui/view {:style {:margin-top 8}}
            [ui/button {:mode     "contained"
                        :disabled (empty? (-> join deref :user-name))
                        :on-press #(do
                                     (dispatch [:join/set-params {:room-code random-room}])
                                     (reset! form-state :host))}
             "Host a Game"]]
           [ui/view {:style {:margin-top 8}}
            [ui/button {:mode     "outlined"
                        :disabled (empty? (-> join deref :user-name))
                        :on-press #(do
                                     (dispatch [:join/set-params {:room-code ""}])
                                     (reset! form-state :join))}
             "Join Someone"]]]
          (= :host @form-state)
          [ui/view
           [ui/para {:style {:margin-top    12
                             :margin-bottom 4}}
            "A code friends will use to join the game:"]
           [ui/text-input {:name            "game-lobby-code"
                           :label           "Lobby Code"
                           :mode            "outlined"
                           :auto-capitalize "none"
                           :auto-correct    false
                           :default-value   (-> join deref :room-code)
                           :on-change-text  #(dispatch [:join/set-params {:room-code (str/lower-case %)}])}]
           [ui/view  {:style {:margin-top 8}}
            [ui/button
             {:mode     "contained"
              :disabled (< (-> join deref :room-code count) 4)
              :on-press #(do
                           (dispatch [:<-join/join-room!]))}
             "Start"]
            [ui/button
             {:style    {:margin-top 4}
              :on-press #(reset! form-state :name)}

             "Go back"]]]
          :else
          [ui/view
           [ui/para {:style {:margin-top    12
                             :margin-bottom 4}}
            "The host will be able to tell you the code:"]
           [ui/text-input {:name            "game-lobby-code"
                           :label           "Lobby Code"
                           :mode            "outlined"
                           :auto-capitalize "none"
                           :auto-correct    false
                           :default-value   (-> join deref :room-code)
                           :on-change-text  #(dispatch [:join/set-params {:room-code (str/lower-case %)}])}]
           [ui/view {:style {:margin-top 8}}
            [ui/button
             {:mode     "contained"
              :disabled (< (-> join deref :room-code count) 4)
              :on-press #(do
                           (dispatch [:<-join/join-room!]))}
             "Join"]
            [ui/button
             {:style    {:margin-top 4}
              :on-press #(reset! form-state :name)}

             "Go back"]]])]]]
     [ui/view {:height (* 0.7 (.-height dimensions))}
      [ui/text ""]]]))

(defn join-panel []
  (let [user-atom   (re-frame/subscribe [:join])
        form-state (r/atom :name)]
    [-join-panel form-state user-atom re-frame/dispatch]))

(defn welcome-panel []
  (let [dimensions (.get ui/dimensions "screen")]
    [ui/collapse-scroll-view {:style {:padding 16}
                              :collapse! do-collapse!}
     [ui/card {}
      [ui/card-cover {:source (.-welcome js/assetLibrary) #_{:uri "https://play.tenkiwi.com/images/base/loading.png"}}]
      [ui/card-title {:title "How to play"
                      :subtitle ""}]
      [ui/card-content
       [ui/para
        "Tenkiwi games are played by showing a series of \"cards\", like the one below, and taking turns reading them. When it's your turn, you'll read the card, then click a button to finish your turn."]
       [ui/view {:style {:margin 16}}
        [ui/button {:mode "outlined"} "Finish Turn"]]
       [ui/para
        "Games have multiple screens you can flip between. Swipe left and right or use the tab bar up top to switch over.\n\n"
        "Some games might have additional actions you can take, such as giving each other points, passing a card to someone else, or generating new prompts.\n\n"
        "There may also be some randomly generated text snippets to help you craft your story on the fly."]]]
     [ui/view {:height (* 0.7 (.-height dimensions))}
      [ui/text ""]]
     [ui/bottom-sheet-card
      {:dispatch re-frame/dispatch
       ;; :start-collapsed? true
       :collapse! do-collapse!
       :turn-marker "Swipe up or down (or tap here)"
       :card {:text (str
                     "Tenkiwi is an app for playing storytelling games with friends.\n\n"
                     "For this to work, you're going to need a way to talk to each other, either being in the same room or in a video conference app will work.\n\n"
                     "Swipe this card down to read more about how to play, or click \"Play\" up above to start.")}}]]))

(defn build-settings-panel [form storage app-url dispatch]
  (fn -settings-panel []
    (let [dimensions    (.get ui/dimensions "screen")
          current-codes (get (deref storage) :unlock-codes [])]
      [ui/collapse-scroll-view {:style          {:padding 16}
                                :only-collapse! do-collapse!}
       [ui/accordion-group 
        ;; TODO: Should this be a controlled component?
        ;; {:expanded-id "personal"}
        [ui/list-accordion
         {:title "Settings"
          :id "settings"}
         [ui/card {:style {}}
          [ui/card-title {:title "Add Personal Game"}]
          [ui/card-content
           [ui/view
            [ui/para {:style {}}
             "Paste a custom game short-code/URL here"]
            [ui/text-input {:name            "personal-game"
                            :label           "URL"
                            :mode            "outlined"
                            :auto-capitalize "none"
                            :auto-correct    false
                            :default-value   (or (-> form deref :unlock-code)
                                                 #_(-> storage deref :unlock-codes))
                            :on-change-text  #(dispatch [:forms/set-params {:action      :settings
                                                                            :unlock-code %}])}]
            [ui/view  {:style {:margin-top 8}}
             [ui/button
              {:mode     "contained"
               :on-press #(do
                            (dispatch [:save-settings!]))}
              "Save Personal Game"]
             #_[ui/button
                {:style    {:margin-top 4}
                 :on-press #(reset! form-state :name)}

                "Go back"]]
            [ui/markdown {}
             (str "Previously saved games:\n\n* "
                  (clojure.string/join "\n* " current-codes))]]]]]
        [ui/list-accordion
         {:title "Credits"
          :id "credits"}
         [ui/card {:style {:margin-top 12}}
          [ui/card-title {:title    "Tenkiwi"}]
          [ui/card-content
           [ui/para
            "Tenkiwi is a hybrid storytelling game app built by David Haslem."]]]
         [ui/card {:style {:margin-top 18}}
          [ui/card-title {:title "Descended from the Queen"}]
          [ui/card-content
           [ui/markdown
            (str
             "Some games in this work are based on [For the Queen](http://www.forthequeengame.com/)"
             ", product of Alex Roberts and Evil Hat Productions, and licensed for our use under the "
             "[Creative Commons Attribution 3.0 Unported license](http://creativecommons.org/licenses/by/3.0/).")]]]
         [ui/card {:style {:margin-top 18
                           }}
          [ui/card-title {:title "Wretched and Alone"}]
          [ui/card-content
           [ui/markdown
            (str
             "Some games in this work are based on The Wretched (found at http://loottheroom.itch.io/wretched), product of Chris Bissette and Loot The Room, and licensed for our use under the "
             "[Creative Commons Attribution 3.0 Unported license](http://creativecommons.org/licenses/by/3.0/).")]]]
         [ui/card {:style {:margin-top 18
                           }}
          [ui/card-title {:title "Redacted Materials"}]
          [ui/card-content
           [ui/markdown
            (str
             "Some games in this work use material from the External Containment Bureau roleplaying game (found at [https://mythicgazetteer.itch.io/external-containment-bureau](https://mythicgazetteer.itch.io/external-containment-bureau)), designed by Lexi Antoku, Eric Brunsell, Michael Elliott, Justin Ford, and Eli Kurtz, and published by Mythic Gazetteer LLC, pursuant to the open license available at [mythicgazetteer.com/redacted-material](http://mythicgazetteer.com/redacted-material/)")]]]
         [ui/card {:style {:margin-top 18
                           }}
          [ui/card-title {:title "X-Card"}]
          [ui/card-content
           [ui/markdown
            (str
             "This application adapts the X-Card, originally by John Stavropoulos"
             "\n\n[http://tinyurl.com/x-card-rpg](http://tinyurl.com/x-card-rpg).")]]]]
        [ui/list-accordion
         {:title "Debug"
          :id "debug"}
         [ui/card {:style {:margin-top 18
                           }}
          [ui/card-title {:title "Debug"}]
          [ui/card-content
           [ui/markdown
            (str
             "Opened with url: "
             "\n\n"
             @app-url
             )]
           (case (:server @storage)
             "staging"
             [ui/button
              {:mode     "contained"
               :on-press #(do
                            (re-frame/dispatch-sync [:set-storage! {:server "prod"}])
                            (ui/refresh))}
              "Switch to Prod"]
             [ui/button
              {:mode     "contained"
               :on-press #(do
                            (re-frame/dispatch-sync [:set-storage! {:server "staging"}])
                            (ui/refresh))}
              "Switch to Staging"]
             )]]]]
       [ui/view {:height (* 0.7 (.-height dimensions))}
        [ui/text ""]]])))

(defn settings-panel []
  (let [random-room "blay"
        dispatch  re-frame/dispatch
        join      (re-frame/subscribe [:join])
        form      (re-frame/subscribe [:form :settings])
        storage   (re-frame/subscribe [:storage])
        app-url   (re-frame/subscribe [:app-url])
        dimensions (.get ui/dimensions "screen")
        current-codes (get (deref storage) [:unlock-codes] [])]
    (build-settings-panel form storage app-url dispatch)))

(defn opening-panel []
  (let [tab-state (r/atom 0)
        scene-map (ui/SceneMap (clj->js {:welcome (r/reactify-component welcome-panel)
                                         :play (r/reactify-component join-panel)
                                         :settings (r/reactify-component settings-panel)}))]
    (fn []
      (let [on-tab-change (fn [x]
                            (@do-collapse! true)
                            (reset! tab-state x))
            current-index @tab-state]
        [ui/clean-tab-view
         {:on-index-change on-tab-change
          ;; :content-container-style {:margin-bottom (* 0.25 (.-height dimensions))}
          :navigation-state {:index current-index
                             :routes [{:key "welcome"
                                       :title "Welcome"}
                                      {:key "play"
                                       :title "Play"}
                                      {:key "settings"
                                       :title "More"}]}
          :render-scene scene-map}]))))
