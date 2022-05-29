(ns tenkiwi.system
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async.interop :refer [p->c] :refer-macros [<p!]]
            [cljs.core.async :as async :refer [go <! put! chan] :refer-macros [go-loop]]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.edn :as edn]
            [taoensso.sente.packers.transit :refer [get-transit-packer]]
            [tenkiwi.socket-events :refer [event-msg-handler]]
            [tenkiwi.components.sente :refer [new-channel-socket-client]]
            [tenkiwi.components.ui :refer [new-ui-component]]
            ["expo-linking" :as expo-linking]
            ["@react-native-async-storage/async-storage" :as store-lib]))

(declare system)

(def store-lib (js/require "@react-native-async-storage/async-storage"))
(def AsyncStorage (.-default store-lib))
(def expo-linking (js/require "expo-linking"))
(defonce timeouts (reagent/atom {}))

(defn get-storage-item [key default]
  (go
    (let [item (<p! (.getItem AsyncStorage key))
          item (if item
                 (js->clj item)
                 default)]
      (.setItem AsyncStorage key (clj->js item))
      item)))

(defn url-listener []
  (let [initial-chan (p->c (.getInitialURL expo-linking))
        other-chan (chan)
        cb   (fn [x]
               (put! other-chan (aget x "url")))
        _ (.addEventListener expo-linking "url" cb)]
    (go-loop [val (<! initial-chan)]
      (re-frame/dispatch [:update-url {:url val}])
      (if val
        (recur (<! other-chan))))))

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

;; We're explicitly accessing from a different origin
(def ?csrf-token "FAKE")

(defn new-system [on-boot]
  (let [client-id   (get-storage-item "device-id" (str (random-uuid)))
        server-info (async/map
                     (fn [x]
                       (let [info (get {"staging"
                                        {:host "sshinto.me"
                                         :port 10555}}
                                       (unstring x)
                                       {:protocol :https
                                        :host     "play.tenkiwi.com"})]
                         (println (str "Connection info: " info))
                         info))
                     [(get-storage-item (stringify :server) (str "prod"))])
        on-boot     (or on-boot (constantly true))]
    (component/system-map
     :sente-handler {:handler event-msg-handler}
     :sente (component/using
             (new-channel-socket-client "/chsk" ?csrf-token {:type      :ws
                                                             :client-id client-id
                                                             :packer    (get-transit-packer)})
             [:sente-handler :client-id :server-info])
     :app-url     (url-listener)
     :client-id   client-id
     :server-info server-info
     :ui-boot     on-boot
     :app-root    (component/using (new-ui-component) [:ui-boot]))))

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

(def supported-storage-keys #{:unlock-codes :server})
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

