(ns tenkiwi.components.sente
  (:require [com.stuartsierra.component :as component]
            [cljs.core.async :refer [go <!]]
            [taoensso.sente :as sente]))

(defrecord ChannelSocketClient [path csrf-token options]
  component/Lifecycle
  (start [component]
    (let [connected-socket (atom nil)
          ch
          (go
            (let [handler (get-in component [:sente-handler :handler])
                  client-id (<! (get-in component [:client-id]))
                  {:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket-client! path csrf-token (assoc options :client-id client-id))
                  socket {:client-id client-id
                          :chsk chsk
                          :ch-chsk ch-recv ; ChannelSocket's receive channel
                          :chsk-send! send-fn ; ChannelSocket's send API fn
                          :chsk-state state}]
              (if handler
                (reset! connected-socket (assoc socket :router (sente/start-chsk-router! ch-recv handler)))
                socket)))]
      (assoc component :connected-socket connected-socket)))
  (stop [component]
    ;; Bugfix on this line - submit upstream?
    (when-let [chsk (:chsk @(:connected-socket component))]
      (sente/chsk-disconnect! chsk))
    (when-let [stop-f (:router @(:connected-socket component))]
      (stop-f))
    (assoc component :connected-socket nil)))

(defn new-channel-socket-client
  ([csrf-token]
   (new-channel-socket-client "/chsk" csrf-token))
  ([path csrf-token]
   (new-channel-socket-client path csrf-token {}))
  ([path csrf-token options]
   (map->ChannelSocketClient {:path path
                              :csrf-token csrf-token
                              :options options})))
