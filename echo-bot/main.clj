(ns main
    (:require [telegrambot-lib.core :as bot]
              [taoensso.timbre :as timbre]))

(def token (clojure.edn/read-string (slurp "config.edn")))
(def mybot (bot/create token))
(def config {:timeout 10 :sleep 1000}) ; timeout - seconds, sleep - milliseconds
(defonce update-id (atom nil))
(defn set-id! [id] (reset! update-id id))

(defn poll-updates
  [bot offset]
  (let [resp (bot/get-updates bot {:offset offset :timeout (:timeout config)})]
    resp))

(defn send-message [bot chat-id text]
  (bot/send-message bot {:chat_id chat-id :text text}))

(defn callback [bot]
  (timbre/info "Checking for updates")
  (let [updates (poll-updates bot @update-id)
        messages (:result updates)]
    (doseq [msg messages]
      (timbre/info msg)
      (let [text (get-in msg [:message :text])
            cmd-length (-> (get-in msg [:message :entities]) first :length)
            command (if (nil? cmd-length) "" (subs text 0 cmd-length))
            chat-id (get-in msg [:message :chat :id])]
        (if (= command "/echo")
          (send-message bot chat-id (subs text (inc cmd-length)))
          (send-message bot chat-id "I'm an echo bot. I only know the /echo command.")))
      (-> msg :update_id inc set-id!))))

(defn main []
  (timbre/info "Echobot service started")
  (while true
    (do
      (callback mybot)
      (Thread/sleep (:sleep config)))))

(main)
