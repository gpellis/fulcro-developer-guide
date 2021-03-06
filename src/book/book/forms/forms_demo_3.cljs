(ns book.forms.forms-demo-3
  (:require
    [clojure.string :as str]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [fulcro.ui.forms :as f :refer [defvalidator]]))

(declare ValidatedPhoneForm)

;; Sample validator that requires there be at least two words
(defn field-with-label
  "A non-library helper function, written by you to help lay out your form."
  ([comp form name label] (field-with-label comp form name label nil))
  ([comp form name label validation-message]
   (dom/div :.form-group {:className (when (f/invalid? form name) "has-error")}
     (dom/label :.col-sm-2 {:htmlFor name} label)
     ;; THE LIBRARY SUPPLIES f/form-field. Use it to render the actual field
     (dom/div :.col-sm-10 (f/form-field comp form name))
     (when (and validation-message (f/invalid? form name))
       (dom/span :.col-sm-offset-2.col-sm-10 {:className (str name)} validation-message)))))

(defn checkbox-with-label
  "A helper function to lay out checkboxes."
  ([comp form name label] (field-with-label comp form name label nil))
  ([comp form name label validation-message]
   (dom/div :.checkbox
     (dom/label (f/form-field comp form name) label))))

(f/defvalidator name-valid? [_ value args]
  (let [trimmed-value (str/trim value)]
    (str/includes? trimmed-value " ")))

(defvalidator us-phone?
  [sym value args]
  (seq (re-matches #"[(][0-9][0-9][0-9][)] [0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]" value)))

(defmutation add-phone [{:keys [id person]}]
  (action [{:keys [state]}]
    (let [new-phone    (f/build-form ValidatedPhoneForm {:db/id id :phone/type :home :phone/number ""})
          person-ident [:people/by-id person]
          phone-ident  (comp/ident ValidatedPhoneForm new-phone)]
      (swap! state (fn [s]
                     (-> s
                         (assoc-in phone-ident new-phone)
                         (m/integrate-ident* phone-ident :append (conj person-ident :person/phone-numbers))))))))

(defsc ValidatedPhoneForm [this form]
  {:initial-state (fn [params] (f/build-form this (or params {})))
   :query         [:db/id :phone/type :phone/number f/form-key]
   :ident         [:phone/id :db/id]
   :form-fields   [(f/id-field :db/id)
                   (f/text-input :phone/number :validator `us-phone?) ; Addition of validator
                   (f/dropdown-input :phone/type [(f/option :home "Home") (f/option :work "Work")])]}
  (dom/div :.form-horizontal
    (field-with-label this form :phone/type "Phone type:")
    ;; One more parameter to give the validation error message:
    (field-with-label this form :phone/number "Number:" "Please format as (###) ###-####")))

(def ui-vphone-form (comp/factory ValidatedPhoneForm {:keyfn :db/id}))

(defsc PersonForm [this {:keys [person/phone-numbers] :as props}]
  {:initial-state (fn [params] (f/build-form this (or params {})))
   :form-fields   [(f/id-field :db/id)
                   (f/subform-element :person/phone-numbers ValidatedPhoneForm :many)
                   (f/text-input :person/name :validator `name-valid?)
                   (f/integer-input :person/age :validator `f/in-range?
                     :validator-args {:min 1 :max 110})
                   (f/checkbox-input :person/registered-to-vote?)]
   ; NOTE: f/form-root-key so that sub-forms will trigger render here
   :query         [f/form-root-key f/form-key
                   :db/id :person/name :person/age
                   :person/registered-to-vote?
                   {:person/phone-numbers (comp/get-query ValidatedPhoneForm)}]
   :ident         [:people/by-id :db/id]}
  (dom/div :.form-horizontal
    (field-with-label this props :person/name "Full Name:" "Please enter your first and last name.")
    (field-with-label this props :person/age "Age:" "That isn't a real age!")
    (checkbox-with-label this props :person/registered-to-vote? "Registered?")
    (when (f/current-value props :person/registered-to-vote?)
      (dom/div "Good on you!"))
    (dom/div
      (mapv ui-vphone-form phone-numbers))
    (when (f/valid? props)
      (dom/div "All fields have had been validated, and are valid"))
    (dom/div :.button-group
      (dom/button :.btn.btn-primary {:onClick #(comp/transact! this
                                                 `[(add-phone ~{:id     (comp/tempid)
                                                                :person (:db/id props)})])}
        "Add Phone")
      (dom/button :.btn.btn-default {:disabled (f/valid? props) :onClick #(f/validate-entire-form! this props)}
        "Validate")
      (dom/button :.btn.btn-default {, :disabled (not (f/dirty? props)) :onClick #(f/reset-from-entity! this props)}
        "UNDO")
      (dom/button :.btn.btn-default {:disabled (not (f/dirty? props)) :onClick #(f/commit-to-entity! this)}
        "Submit"))))

(def ui-person-form (comp/factory PersonForm))

(defsc Root [this {:keys [person]}]
  {:initial-state (fn [params]
                    {:ui/person-id 1
                     :person       (comp/get-initial-state PersonForm
                                     {:db/id                      1
                                      :person/name                "Tony Kay"
                                      :person/age                 23
                                      :person/registered-to-vote? false
                                      :person/phone-numbers       [(comp/get-initial-state ValidatedPhoneForm
                                                                     {:db/id        22
                                                                      :phone/type   :work
                                                                      :phone/number "(123) 412-1212"})
                                                                   (comp/get-initial-state ValidatedPhoneForm
                                                                     {:db/id        23
                                                                      :phone/type   :home
                                                                      :phone/number "(541) 555-1212"})]})})
   :query         [:ui/person-id {:person (comp/get-query PersonForm)}]}
  (dom/div
    (when person
      (ui-person-form person))))

