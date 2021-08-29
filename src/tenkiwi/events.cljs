(ns tenkiwi.events
  (:require [re-frame.core :as re-frame]
            [tenkiwi.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   ;; TODO: unclear if ever called successfully. See initialize-system instead
   (println "Initializing....")
   db/default-db))

(re-frame/reg-event-fx
 :initialize-system
 (fn  [db _]
   {:db (merge db/default-db db)
    :fx [[:websocket [:user/connected!]]]}))

(re-frame/reg-event-fx
 :user/connected!
 [(re-frame/inject-cofx :system :sente)]
 (fn [{:as x :keys [db sente]} [_ params]]
   {:db (update-in db [:user] assoc
                   :id (-> sente :connected-socket deref :client-id)
                   :connected? true)}))

(re-frame/reg-event-db
 :join/set-params
 (fn [db [_ params]]
   (let [{:keys [user-name
                 room-code]} params
         room-code (clojure.string/lower-case (or room-code ""))
         params (assoc params :room-code room-code)]
     (update-in db [:join] merge params))))

(re-frame/reg-event-db
 :forms/set-params
 (fn [db [_ params]]
   (let [{:keys [action]} params
         remaining-params (dissoc params :action)]
     (println remaining-params)
     (update-in db [:forms action] merge remaining-params))))

(re-frame/reg-event-db
 :->user/room-joined!
 (fn [db [_ params]]
   (update-in db [:user] assoc :current-room params)))

(re-frame/reg-event-db
 :->user/booted!
 (fn [db [_ params]]
   (update-in db [:user] assoc :current-room nil)))

(re-frame/reg-event-fx
 :<-game/action!
 (fn [db [_ action & params]]
   (let [room-id (get-in db [:user :current-room :id])]
     {:fx [[:websocket [:game/action! {:action  action
                                       :params  (first params)
                                       :room-id room-id}]]]})))

;; Inbound short-term events

(re-frame/reg-event-fx
 :->sound/trigger!
 (fn [{:keys [db]} [_ sound]]
   {:db db
    :fx [[:soundboard {:sound sound}]]}))

(re-frame/reg-event-fx
 :->toast/show!
 (fn [{:keys [db]} [_ message]]
   {:db (assoc db :latest-toast {:visible true
                                 :message message})
    :timeout {:id    :toast
              :event [:hide-toast]
              :time  7000}}
   ))

(re-frame/reg-event-db
 :hide-toast
 (fn [db [_]]
   (assoc-in db [:latest-toast :visible] false)))

;;; -------

(re-frame/reg-event-db
 :->game/changed!
 (fn [db [_ params]]
   (let [current-room (get-in db [:user :current-room])]
     (if (= (:room-code params) (:room-code current-room))
       (do
         (update-in db [:user] assoc :current-room params))))))

(re-frame/reg-event-db
 :->game/started!
 (fn [db [_ params]]
   (let [current-room (get-in db [:user :current-room])]
     (if (= (:room-code params) (:room-code current-room))
       (do
         (update-in db [:user] assoc :current-room params))))))

(re-frame/reg-event-db
 :->room/user-joined!
 (fn [db [_ params]]
   (let [current-room (get-in db [:user :current-room])]
     (if (= (:room-code params) (:room-code current-room))
       (update-in db [:user] assoc :current-room params)
       db))))

(re-frame/reg-event-db
 :->room/user-left!
 (fn [db [_ params]]
   (let [current-room (get-in db [:user :current-room])]
     (if (= (:room-code params) (:room-code current-room))
       (update-in db [:user] assoc :current-room params)
       db))))

(re-frame/reg-event-fx
 :<-game/start!
 (fn [{:keys [db]} [_ id & params]]
   {:fx [[:websocket [:game/start! {:game-type id
                                    :params    (first params)}]]]}))

(re-frame/reg-event-fx
 :<-join/join-room!
 (fn [{:keys [db]} [_ val]]
   (let [join (:join db)]
     {:fx [[:websocket [:room/join-room! join]]]})))

(re-frame/reg-event-fx
 :<-room/boot-player!
 (fn [{:keys [db]} [_ val]]
   {:fx [[:websocket [:room/boot-player! val]]]}))
