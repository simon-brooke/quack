(ns dog-and-duck.quack.scratch
  "Development scratchpad"
  (:require [dog-and-duck.quack.objects :refer [object-expected-properties]]
            [dog-and-duck.quack.utils :refer [concat-non-empty]]))

(defn missing-messages
  [language]
  (let [tokens (set
                (reduce
                 concat-non-empty
                 (map #(list
                        (second (:if-invalid %))
                        (second (:if-missing %)))
                      (vals object-expected-properties))))
        found (read-string (slurp (str "resources/i18n/" language ".edn")))]
    (sort (remove found tokens))))

;; (def f {:arguments {:reports ({:@context "https://simon-brooke.github.io/dog-and-duck/codox/Validation_Faults.html", 
;;                                :id "https://illuminator.local/fault/25785:1673378166063", :type "Fault", :severity :should, :fault :no-context, :narrative "Section 3 of the ActivityPub specification states Implementers SHOULD include the ActivityPub context in their object definitions`."}
;;                               {:@context "https://simon-brooke.github.io/dog-and-duck/codox/Validation_Faults.html", 
;;                                :id "https://illuminator.local/fault/25785:1673378166063", :type "Fault", :severity :minor, :fault :no-id-transient, :narrative "The ActivityPub specification allows objects without `id` fields only if they are intentionally transient; even so it is preferred that the object should have an explicit null id."} 
;;                               ({:@context "https://simon-brooke.github.io/dog-and-duck/codox/Validation_Faults.html", 
;;                                 :id "https://illuminator.local/fault/25785:1673378166069", :type "Fault", :severity :must, :fault :invalid-type, :narrative "invalid-type"})), :severity :info}})