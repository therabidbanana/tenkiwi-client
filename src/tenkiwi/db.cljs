(ns tenkiwi.db)

(def default-db
  {:name         "re-frame"
   :app-url      nil
   :forms        {}
   :storage      {}
   :latest-toast {:visible false
                  :message "Test"}
   :user         {:user-name    ""
                  :current-room nil}
   :join         {:user-name ""
                  :room-code ""}})
