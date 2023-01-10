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