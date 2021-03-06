(ns book.demos.pre-merge.countdown
  (:require
    [com.fulcrologic.fulcro.data-fetch :as df]
    [book.demos.util :refer [now]]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.wsscode.pathom.connect :as pc]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SERVER:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def all-counters
  [{::counter-id 1 ::counter-label "A"}])

(pc/defresolver counter-resolver [env {::keys [counter-id]}]
  {::pc/input  #{::counter-id}
   ::pc/output [::counter-id ::counter-label]}
  (let [{:keys [id]} (-> env :ast :params)]
    (first (filter #(= id (::counter-id %)) all-counters))))

(def resolvers [counter-resolver])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CLIENT:
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsc Countdown [this {::keys   [counter-label]
                        :ui/keys [count]}]
  {:ident     [::counter-id ::counter-id]
   :query     [::counter-id ::counter-label :ui/count]
   :pre-merge (fn [{:keys [current-normalized data-tree]}]
                (merge
                  {:ui/count 5}
                  current-normalized
                  data-tree))}
  (dom/div
    (dom/h4 counter-label)
    (let [done? (zero? count)]
      (dom/button {:disabled done?
                   :onClick  #(m/set-value! this :ui/count (dec count))}
        (if done? "Done!" (str count))))))

(def ui-countdown (comp/factory Countdown {:keyfn ::counter-id}))

(defsc Root [this {:keys [counter]}]
  {:initial-state (fn [_] {})
   :query         [{:counter (comp/get-query Countdown)}]}
  (dom/div
    (dom/h3 "Counters")
    (if (seq counter)
      (ui-countdown counter)
      (dom/button {:onClick #(df/load! this [::counter-id 1] Countdown {:target [:counter]})}
        "Load one counter"))))

(defn initialize
  "To be used in :started-callback to pre-load things."
  [app])
