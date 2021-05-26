(ns your-project.system
  (:require [com.stuartsierra.component :as component]
            [re-frame.core :as re-frame]
            [your-project.socket-events :refer [event-msg-handler]]
            [your-project.components.sente :refer [new-channel-socket-client]]
            [your-project.components.ui :refer [new-ui-component]]))

(declare system)

;;;; Scrape document for a csrf token to boot sente with
#_(def ?csrf-token
  (when-let [el (or (.getElementById js/document "sente-csrf-token")
                    "wA25k5v3gxuYtmEiKtcDZWZaeg9weWTSfThOWoJzhQFG2FeXQ8Q0mD5IGZEoTUGxkdltUI46QRc%2BdPa3")]
    (.getAttribute el "data-csrf-token")))

(def ?csrf-token
  "wA25k5v3gxuYtmEiKtcDZWZaeg9weWTSfThOWoJzhQFG2FeXQ8Q0mD5IGZEoTUGxkdltUI46QRc+dPa3")

(defn ?client-id []
  (let [client-id (js->clj (.getItem js/localStorage "device-id"))
        client-id (or client-id (str (random-uuid)))
        as-str    (clj->js client-id)]
    (do
      (.setItem js/localStorage "device-id" as-str)
      client-id)))

;; TODO: CHSK should probably live in here (prevent CSRF failures on figwheel?)
(defn new-system []
  (component/system-map
   :sente-handler {:handler event-msg-handler}
   :sente (component/using
           (new-channel-socket-client "/chsk" ?csrf-token {:type      :ws
                                                           :packer    :edn
                                                           :host "sshinto.me"
                                                           :port 10555
                                                           :client-id (?client-id)})
           [:sente-handler])
   :client-id (?client-id)
   :app-root (new-ui-component)))

(defn init []
  (set! system (new-system)))

(defn start []
  (set! system (component/start system)))

(defn stop []
  (set! system (component/stop system)))

(defn ^:export go []
  (init)
  (start))

(defn reset []
  (stop)
  (go))

;; TODO: can I move this?
(re-frame/reg-cofx
 :system
 (fn [cofx component]
   (println component)
   (assoc cofx component (get-in system [component]))))

(re-frame/reg-fx
 :websocket
 (fn [chsk-args]
   (let [chsk-send! (get-in system [:sente :chsk-send!])]
     ;; TODO: Add timeout, callback for response -> dispatch
     (chsk-send! chsk-args))))
