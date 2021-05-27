(ns your-project.system
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs.core.async :refer [go <!]]
            [re-frame.core :as re-frame]
            [your-project.socket-events :refer [event-msg-handler]]
            [your-project.components.sente :refer [new-channel-socket-client]]
            [your-project.components.ui :refer [new-ui-component]]))

(declare system)

(def store-lib (js/require "@react-native-async-storage/async-storage"))
(js/console.log store-lib)
(def AsyncStorage (.-default store-lib))
(js/console.log AsyncStorage)

(defn get-storage-item [key default]
  (go
    (let [item (<p! (.getItem AsyncStorage key))
          item (if item
                 (js->clj item)
                 default)]
      (.setItem AsyncStorage key (clj->js item))
      item)))

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
  (let [client-id (get-storage-item "device-id" (str (random-uuid)))]
    (component/system-map
     :sente-handler {:handler event-msg-handler}
     :sente (component/using
             (new-channel-socket-client "/chsk" ?csrf-token {:type      :ws
                                                             :packer    :edn
                                                             :host "sshinto.me"
                                                             :port 10555
                                                             :client-id client-id})
             [:sente-handler :client-id])
     :client-id client-id
     :app-root (new-ui-component))))

(defn init []
  (set! system (new-system)))

(defn start []
  (set! system (component/start system)))

(defn stop []
  (set! system (component/stop system)))

(defn ^:export go []
  (init)
  (println system)
  (start))

(defn reset []
  (stop)
  (go))

;; TODO: can I move this?
(re-frame/reg-cofx
 :system
 (fn [cofx component]
   (assoc cofx component (get-in system [component]))))

(re-frame/reg-fx
 :websocket
 (fn [chsk-args]
   (println system)
   (let [socket (get-in system [:sente :connected-socket])
         socket (if socket (deref socket))]
     ;; TODO: Add timeout, callback for response -> dispatch
     (if socket
       ((get socket :chsk-send!) chsk-args)
       (js/console.log "Not connected, but trying to send a message." chsk-args)))))
