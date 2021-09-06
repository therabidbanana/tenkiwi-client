(ns tenkiwi.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 :name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 :user
 (fn [db]
   (:user db)))

(re-frame/reg-sub
 :storage
 (fn [db]
   (:storage db)))

(re-frame/reg-sub
 :toast
 (fn [db]
   (:latest-toast db)))

(re-frame/reg-sub
 :room
 (fn [db]
   (get-in db [:user :current-room])))

(re-frame/reg-sub
 :game
 (fn [db]
   (get-in db [:user :current-room :game])))

(re-frame/reg-sub
 :user->game-type
 (fn [db]
   (get-in db [:user :current-room :game :game-type])))

(re-frame/reg-sub
 :join
 (fn [db]
   (:join db)))

(re-frame/reg-sub
 :forms
 (fn [db]
   (:forms db)))

(re-frame/reg-sub
 :form
 (fn [db [_ key]]
   (get-in db [:forms key])))
