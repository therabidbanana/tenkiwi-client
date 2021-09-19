(ns tenkiwi.system
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs.core.async :refer [go <!]]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.edn :as edn]
            [taoensso.sente.packers.transit :refer [get-transit-packer]]
            [tenkiwi.socket-events :refer [event-msg-handler]]
            [tenkiwi.components.sente :refer [new-channel-socket-client]]
            [tenkiwi.components.ui :refer [new-ui-component]]
            ["@react-native-async-storage/async-storage" :as store-lib]))

(declare system)

(def store-lib (js/require "@react-native-async-storage/async-storage"))
(def AsyncStorage (.-default store-lib))
(defonce timeouts (reagent/atom {}))

(defn get-storage-item [key default]
  (go
    (let [item (<p! (.getItem AsyncStorage key))
          item (if item
                 (js->clj item)
                 default)]
      (.setItem AsyncStorage key (clj->js item))
      item)))

(defn set-storage-item [key str]
  (.setItem AsyncStorage (clj->js key) (clj->js str)))

(defn- stringify [val] (clj->js (prn-str val)))
(defn- unstring [str] (edn/read-string str))

(defn load-storage-items [keys callback]
  (let [key-arr (mapv stringify keys)

        cb (fn [errs vals]
             (let [mapped (reduce #(assoc %1
                                          (unstring (first %2))
                                          (unstring (last %2)))
                                  {}
                                  (js->clj vals))]
               (callback mapped)))]
    (.multiGet AsyncStorage (clj->js key-arr) cb)))

(defn set-storage-items [map]
  (let [keystrs (mapv stringify (keys map))
        valstrs (mapv stringify (vals map))
        tuples  (mapv #(array %1 %2) keystrs valstrs)]
    (.multiSet AsyncStorage (clj->js tuples))))

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

(re-frame/reg-cofx
 :storage
 (fn [cofx key]
   (assoc cofx :storage {key (get-storage-item (name key) "")})))

(re-frame/reg-fx
 :websocket
 (fn [chsk-args]
   (let [socket (get-in system [:sente :connected-socket])
         socket (if socket (deref socket))
         socket (if (-> socket :chsk-state deref :open?)
                  socket
                  nil)]
     ;; TODO: Add timeout, callback for response -> dispatch
     (if socket
       ((get socket :chsk-send!) chsk-args)
       (js/console.log "Not connected, but trying to send a message." chsk-args)))))

(def supported-storage-keys #{:unlock-codes})
(re-frame/reg-fx
 :set-storage
 (fn [map]
   (let [bad-keys (remove supported-storage-keys (keys map))]
     (doseq [bad-key bad-keys]
       (println "Attempting set on unsupported key" bad-key (get map bad-key)))
     (set-storage-items map))))

(re-frame/reg-fx
 :load-storage
 (fn [_]
   (let [keys supported-storage-keys
         handle-vals (fn [pairs] (re-frame/dispatch [:sync-storage pairs]))]
     (load-storage-items keys handle-vals))))

(re-frame/reg-fx
 :soundboard
 (fn [{:keys [sound]}]
   (let []
     ;; TODO: Work out sound playing via Expo
     #_(.play (.getElementById js/document (str "sound-" (name sound)))))))

(re-frame/reg-fx
 :timeout
 (fn [{:keys [id event time]}]
   (when-some [existing (get @timeouts id)]
     (js/clearTimeout existing)
     (swap! timeouts dissoc id))
   (when (some? event)
     (swap! timeouts assoc id
            (js/setTimeout
             (fn []
               (re-frame/dispatch event))
             time)))))

