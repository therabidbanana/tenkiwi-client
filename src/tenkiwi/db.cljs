(ns tenkiwi.db)

(def default-db
  {:name "re-frame"
   :forms {}
   :user {:user-name ""
          :current-room nil}
   :join {:user-name ""
          :room-code ""}})
