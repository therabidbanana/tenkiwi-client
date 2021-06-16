(ns tenkiwi.system
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs.core.async :refer [go <!]]
            [re-frame.core :as re-frame]
            [taoensso.sente.packers.transit :refer [get-transit-packer]]
            [tenkiwi.socket-events :refer [event-msg-handler]]
            [tenkiwi.components.sente :refer [new-channel-socket-client]]
            [tenkiwi.components.ui :refer [new-ui-component]]
            ["@react-native-async-storage/async-storage" :as store-lib]))

(declare system)

(def store-lib (js/require "@react-native-async-storage/async-storage"))
(def AsyncStorage (.-default store-lib))

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
(defn new-system [on-boot]
  (let [client-id (get-storage-item "device-id" (str (random-uuid)))
        on-boot (or on-boot (constantly true))]
    (component/system-map
     :sente-handler {:handler event-msg-handler}
     :sente (component/using
             (new-channel-socket-client "/chsk" ?csrf-token {:type      :ws
                                                             :packer    (get-transit-packer)
                                                             ;; Next 2 - prod-mode
                                                             :protocol :https
                                                             :host "play.tenkiwi.com"
                                                             ;; Next 2 - dev mode
                                                             ;; :host "sshinto.me"
                                                             ;; :port 10555
                                                             :client-id client-id})
             [:sente-handler :client-id])
     :client-id client-id
     :ui-boot  on-boot
     :app-root (component/using (new-ui-component)
                                [:ui-boot]))))

(defn init [on-boot]
  (set! system (new-system on-boot)))

(defn start []
  (set! system (component/start system)))

(defn stop []
  (set! system (component/stop system)))

(defn ^:export go [on-boot]
  (init on-boot)
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
   (let [socket (get-in system [:sente :connected-socket])
         socket (if socket (deref socket))]
     ;; TODO: Add timeout, callback for response -> dispatch
     (if socket
       ((get socket :chsk-send!) chsk-args)
       (js/console.log "Not connected, but trying to send a message." chsk-args)))))
