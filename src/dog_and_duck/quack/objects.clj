(ns dog-and-duck.quack.objects
  (:require [clojure.data.json :as json]
            [clojure.set :refer [union]]
            [clojure.walk :refer [keywordize-keys]]
            [dog-and-duck.quack.constants :refer [actor-types
                                                  noun-types
                                                  re-rfc5646]]
            [dog-and-duck.quack.control-variables :refer [*reify-refs*]]
            [dog-and-duck.quack.time :refer [xsd-date-time?
                                             xsd-duration?]]
            [dog-and-duck.quack.utils :refer [concat-non-empty
                                              cond-make-fault-object
                                              fault-list?
                                              has-activity-type?
                                              has-context?
                                              has-type?
                                              has-type-or-fault
                                              make-fault-object
                                              nil-if-empty
                                              object-or-uri?
                                              truthy?
                                              xsd-non-negative-integer?]]
            [taoensso.timbre :refer [warn]])
  (:import [java.io FileNotFoundException]
           [java.net URI URISyntaxException]))

;;;     Copyright (C) Simon Brooke, 2022

;;;     This program is free software; you can redistribute it and/or
;;;     modify it under the terms of the GNU General Public License
;;;     as published by the Free Software Foundation; either version 2
;;;     of the License, or (at your option) any later version.

;;;     This program is distributed in the hope that it will be useful,
;;;     but WITHOUT ANY WARRANTY; without even the implied warranty of
;;;     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
;;;     GNU General Public License for more details.

;;;     You should have received a copy of the GNU General Public License
;;;     along with this program; if not, write to the Free Software
;;;     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

(declare object-faults)

(defn- xsd-float?
  [pv]
  (or (integer? pv) (float? pv)))


(def maybe-reify
  "If `*reify-refs*` is `true`, return the object at this `target` URI.
   Returns `nil` if
   
   1. `*reify-refs*` is false;
   2. the object was not found;
   3. access to the object was not permitted.
   
   Consequently, use with care."
  (memoize
   (fn [target]
     (try (let [uri (URI. target)]
            (when *reify-refs*
              (keywordize-keys (json/read-str (slurp uri)))))
          (catch URISyntaxException _
            (warn "Reification target" target "was not a valid URI.")
            nil)
          (catch FileNotFoundException _
            (warn "Reification target" target "was not found.")
            nil)))))

(defn maybe-reify-or-faults
  "If `*reify-refs*` is `true`, runs basic checks on the object at this 
   `target` URI, if it is found, or a list containing a fault object with
   this `severity` and `token` if it is not."
  [value expected-type severity token]
  (let [object (maybe-reify value)]
    (cond object
          (object-faults object expected-type)
          *reify-refs* (list (make-fault-object severity token)))))

(defn object-reference-or-faults
  "If this `value` is either 
   
   1. an object of `expected-type`;
   2. a URI referencing an object of  `expected-type`; or
   3. a link object referencing an object of  `expected-type`
  
   and no faults are returned from validating the linked object, then return
   `nil`; else return a sequence comprising a fault object with this `severity`
   and `token`, prepended to the faults returned.
   
   As with `has-type-or-fault` (q.v.), `expected-type` may be passed as a
   string, as a set of strings, or `nil` (indicating the type of the 
   referenced object should not be checked).
   
   **NOTE THAT** if `*reify-refs*` is `false`, referenced objects will not
   actually be checked."
  ([value expected-type severity token]
   (let [faults (cond
                  (string? value) (maybe-reify-or-faults value severity token expected-type)
                  (map? value) (if (has-type? value "Link")
                                 (cond
                                  ;; if we were looking for a link and we've 
                                  ;; found a link, that's OK.
                                   (= expected-type "Link") nil
                                   (and (set? expected-type) (expected-type "Link")) nil
                                   (nil? expected-type) nil
                                   :else
                                   (object-reference-or-faults
                                    (:href value) expected-type severity token))
                                 (object-faults value expected-type))
                  :else (throw
                         (ex-info
                          "Argument `value` was not an object or a link to an object"
                          {:arguments {:value value}
                           :expected-type expected-type
                           :severity severity
                           :token token})))]
     (when faults (cons (make-fault-object severity token) faults)))))

(defn coll-object-reference-or-faults
  "As object-reference-or-fault, except `value` argument may also be a list of
    objects and/or object references."
  [value expected-type severity token]
  (cond
    (string? value) (maybe-reify-or-faults value expected-type severity token)
    (map? value) (object-reference-or-faults value expected-type severity token)
    (coll? value) (concat-non-empty
                   (map
                    #(object-reference-or-faults
                      % expected-type severity token)
                    value))
    :else (throw
           (ex-info
            "Argument `value` was not an object, a link to an object, nor a list of these."
            {:arguments {:value value}
             :expected-type expected-type
             :severity severity
             :token token}))))


(def object-expected-properties
  "Requirements of properties of object, cribbed from
   https://www.w3.org/TR/activitystreams-vocabulary/#properties
   
   Note the following sub-key value types:
   
   * `:collection` opposite of `:functional`: if true, value should be a
      collection (in the Clojure sense), not a single object;
   * `:functional` if true, value should be a single object; if false, may
      be a single object or a sequence of objects, but each must pass 
      validation checks;
   * `:if-invalid` a sequence of two keywords, first indicating severity,
      second being a message key;
   * `:if-missing` a sequence of two keywords, first indicating severity,
      second being a message key;
   * `:required` a boolean, or a function of one argument returning a 
      boolean, in which case the function will be applied to the object
      having the property;
   * `:validator` either a function of one argument returning a boolean, or
      a function of one argument returning either `nil` or a list of faults,
      which will be applied to the value or values of the identified property."
  {:accuracy {:functional false
              :if-invalid [:must :invalid-number]
              :validator (fn [pv] (and (xsd-float? pv)
                                       (>= pv 0)
                                       (<= pv 100)))}
   :actor {:functional false
           :if-invalid [:must :invalid-actor]
           :if-missing [:must :no-actor]
           :required has-activity-type?
           :validator (fn [pv] (coll-object-reference-or-faults pv
                                                                actor-types
                                                                :must
                                                                :invalid-actor))}
   :altitude {:functional false
              :if-invalid [:must :invalid-number]
              :validator xsd-float?}
   :anyOf {:collection true
           :functional false
           ;; a Question should have a `:oneOf` or `:anyOf`, but at this layer
           ;; that's hard to check.
           :if-invalid [:must :invalid-option]
           :validator (fn [pv] (coll-object-reference-or-faults pv nil
                                                                :must
                                                                :invalid-actor))}
   :attachment {:functional false
                :if-invalid [:must :invalid-attachment]
                :validator (fn [pv] (coll-object-reference-or-faults pv nil
                                                                     :must
                                                                     :invalid-attachment))}
   :attributedTo {:functional false
                  :if-invalid [:must :invalid-attribution]
                  :validator (fn [pv] (coll-object-reference-or-faults pv nil
                                                                       :must
                                                                       :invalid-attribution))}
   :audience {:functional false
              :if-invalid [:must :invalid-audience]
              :validator (fn [pv] (coll-object-reference-or-faults pv nil
                                                                   :must
                                                                   :invalid-audience))}
   :bcc {:functional false
         :if-invalid [:must :invalid-audience] ;; do we need a separate message for bcc, cc, etc?
         :validator (fn [pv] (coll-object-reference-or-faults pv nil :must :invalid-audience))}
   :cc {:functional false
        :if-invalid [:must :invalid-audience] ;; do we need a separate message for bcc, cc, etc?
        :validator (fn [pv] (coll-object-reference-or-faults pv nil :must :invalid-audience))}
   :closed {:functional false
            :if-invalid [:must :invalid-closed]
            :validator (fn [pv] (truthy? (or (object-or-uri? pv)
                                             (xsd-date-time? pv)
                                             (#{"true" "false"} pv))))}
   :content {:functional false
             :if-invalid [:must :invalid-content]
             :validator string?}
   :context {:functional false
             :if-invalid [:must :invalid-context]
             :validator (fn [pv] (coll-object-reference-or-faults pv nil :must :invalid-context))}
   :current {:functional true
             :if-missing [:minor :paged-collection-no-current]
             :if-invalid [:must :paged-collection-invalid-current]
             :required (fn [x] ;; if an object is a collection which has pages,
                                 ;; it ought to have a `:current` page. But 
                                 ;; 1. it isn't required to, and
                                 ;; 2. there's no certain way of telling that it
                                 ;;    does have pages - although if it has a
                                 ;;    `:first`, then it is.
                         (and
                          (or (has-type? x "Collection")
                              (has-type? x "OrderedCollection"))
                          (:first x)))
             :validator (fn [pv] (object-reference-or-faults pv
                                                             #{"CollectionPage"
                                                               "OrderedCollectionPage"}
                                                             :must
                                                             :paged-collection-invalid-current))}
   :deleted {:functional true
             :if-missing [:minor :tombstone-missing-deleted]
             :if-invalid [:must :invalid-deleted]
             :required (fn [x] (has-type? x "Tombstone"))
             :validator xsd-date-time?}
   :describes {:functional true
               :required (fn [x] (has-type? x "Profile"))
               :if-invalid [:must :invalid-describes]
               :validator (fn [pv] (object-reference-or-faults pv nil
                                                               :must
                                                               :invalid-describes))}
   :duration {:functional false
              :if-invalid [:must :invalid-duration]
              :validator xsd-duration?}
   :endTime {:functional true
             :if-invalid [:must :invalid-date-time]
             :validator xsd-date-time?}
   :first {:functional true
           :if-missing [:minor :paged-collection-no-first]
           :if-invalid [:must :paged-collection-invalid-first]
           :required (fn [x] ;; if an object is a collection which has pages,
                                 ;; it ought to have a `:first` page. But 
                                 ;; 1. it isn't required to, and
                                 ;; 2. there's no certain way of telling that it
                                 ;;    does have pages - although if it has a
                                 ;;    `:last`, then it is.
                       (and
                        (or (has-type? x "Collection")
                            (has-type? x "OrderedCollection"))
                        (:last x)))
           :validator (fn [pv] (object-reference-or-faults pv #{"CollectionPage"
                                                                "OrderedCollectionPage"}
                                                           :must
                                                           :paged-collection-invalid-first))}
   :formerType {:functional false
                :if-missing [:minor :tombstone-missing-former-type]
                :if-invalid [:must :invalid-former-type]
                :required (fn [x] (has-type? x "Tombstone"))
                ;; The narrative of the spec says this should be an `Object`,
                ;; but in all the provided examples it's a string. Furthermore,
                ;; it seems it must name a known object type within the context.
                ;; So TODO I'm assuming an error in the spec here.
                :validator string?}
   :generator {:functional false
               :if-invalid [:must :invalid-generator]
               :validator #(try (uri? (URI. %))
                                (catch Exception _ false))}
   :height {:functional false
            :if-invalid [:must :invalid-non-negative]
            :validator xsd-non-negative-integer?}
   :href {:functional false
          :if-invalid [:must :invalid-href]
          :validator (fn [pv] (try (uri? (URI. pv))
                                   (catch URISyntaxException _ false)))}
   :hreflang {:validator (fn [pv] (truthy? (re-matches re-rfc5646 pv)))}
   :icon {:functional false
          :if-invalid [:must :invalid-icon]
          ;; an icon is also expected to have a 1:1 aspect ratio, but that's
          ;; too much detail at this level of verification
          :validator (fn [pv] (coll-object-reference-or-faults pv "Image"
                                                               :must
                                                               :invalid-icon))}
   :id {:functional true
        :if-missing [:minor :no-id-transient]
        :if-invalid [:must :invalid-id]
        :validator (fn [pv] (try (uri? (URI. pv))
                                 (catch URISyntaxException _ false)))}
   :image {:functional false
           :if-invalid [:must :invalid-image]
           :validator (fn [pv] (coll-object-reference-or-faults pv "Image"
                                                                :must
                                                                :invalid-image))}
   :inReplyTo {:functional false
               :if-invalid [:must :invalid-in-reply-to]
               :validator (fn [pv] (coll-object-reference-or-faults pv noun-types
                                                                    :must
                                                                    :invalid-in-reply-to))}
   :instrument {:functional false
                :if-invalid [:must :invalid-instrument]
                :validator (fn [pv] (coll-object-reference-or-faults pv nil
                                                                     :must
                                                                     :invalid-instrument))}
   :items {:collection true
           :functional false
           :if-invalid [:must :invalid-items]
           :if-missing [:must :no-items-or-pages]
           :required (fn [x] (or (has-type? x "CollectionPage")
                                 (and (has-type? x "Collection")
                                      ;; if it's a collection and has pages,
                                      ;; it doesn't need items.
                                      (not (:current x))
                                      (not (:first x))
                                      (not (:last x)))))
           :validator (fn [pv] (and (coll? pv) (every? object-or-uri? pv)))}
   :last {:functional true
          :if-missing [:minor :paged-collection-no-last]
          :if-invalid [:must :paged-collection-invalid-last]
          :required (fn [x] (if (and
                                 (string? x)
                                 (try (uri? (URI. x))
                                      (catch URISyntaxException _ false)))
                              true
                                 ;; if an object is a collection which has pages,
                                 ;; it ought to have a `:last` page. But 
                                 ;; 1. it isn't required to, and
                                 ;; 2. there's no certain way of telling that it
                                 ;;    does have pages - although if it has a
                                 ;;    `:first`, then it is.
                              (and
                               (has-type? x #{"Collection"
                                              "OrderedCollection"})
                               (:first x))))
          :validator (fn [pv] (object-reference-or-faults pv #{"CollectionPage"
                                                               "OrderedCollectionPage"}
                                                          :must
                                                          :paged-collection-invalid-last))}
   :latitude {:functional true
              :if-invalid [:must :invalid-latitude]
              ;; The XSD spec says this is an IEEE 754-2008, and the IEEE
              ;; wants US$104 for me to find out what that is. So I don't
              ;; strictly know that an integer is valid here.
              :validator xsd-float?}
   :location {:functional false
              :if-invalid [:must :invalid-location]
              :validator (fn [pv] (coll-object-reference-or-faults pv #{"Place"}
                                                                   :must
                                                                   :invalid-location))}
   :longitude {:functional true
               :if-invalid [:must :invalid-longitude]
               :validator xsd-float?}
   :mediaType {:functional true
               :if-invalid [:must :invalid-mime-type]
               :validator (fn [pv] (truthy? (re-matches #"\w+/[-.\w]+(?:\+[-.\w]+)?" pv)))}
   :name {:functional false
          :if-invalid [:must :invalid-name]
          :validator string?}
   :next {:functional true
          :if-invalid [:must :invalid-next-page]
          :validator (fn [pv] (object-reference-or-faults pv #{"CollectionPage"
                                                               "OrderedCollectionPage"}
                                                          :must
                                                          :invalid-next-page))}
   :object {:functional false
            :if-invalid [:must :invalid-direct-object]
            :validator (fn [pv]
                         (coll-object-reference-or-faults pv nil
                                                          :must
                                                          :invalid-direct-object))}
   :oneOf {:collection true
           :functional false
           ;; a Question should have a `:oneOf` ot `:anyOf`, but at this layer
           ;; that's hard to check.
           :if-invalid [:must :invalid-option]
           :validator (fn [pv]
                        (coll-object-reference-or-faults pv nil
                                                         :must
                                                         :invalid-option))}
   :orderedItems {:collection true
                  :functional false
                  :if-invalid [:must :invalid-items]
                  :if-missing [:must :no-items-or-pages]
                  :required (fn [x] (or (has-type? x "OrderedCollectionPage")
                                        (and (has-type? x "OrderedCollection")
                                      ;; if it's a collection and has pages,
                                      ;; it doesn't need items.
                                             (not (:current x))
                                             (not (:first x))
                                             (not (:last x)))))
                  :validator (fn [pv] (and (coll? pv) (every? object-or-uri? pv)))}
   :origin {:functional false
            :if-invalid [:must :invalid-origin]
            :validator (fn [pv] (coll-object-reference-or-faults pv nil :must :invalid-origin))}
   :partOf {:functional true
            :if-missing [:must :missing-part-of]
            :if-invalid [:must :invalid-part-of]
            :required object-or-uri?
            :validator (fn [pv] (object-reference-or-faults pv #{"Collection"
                                                                 "OrderedCollection"}
                                                            :must
                                                            :invalid-part-of))}
   :prev {:functional true
          :if-invalid [:must :invalid-prior-page]
          :validator (fn [pv] (object-reference-or-faults pv #{"CollectionPage"
                                                               "OrderedCollectionPage"}
                                                          :must
                                                          :invalid-prior-page))}
   :preview {:functional false
             :if-invalid [:must :invalid-preview]
             ;; probably likely to be an Image or Video, but that isn't stated.
             :validator (fn [pv] (coll-object-reference-or-faults pv nil :must :invalid-preview))}
   :published {:functional true
               :if-invalid [:must :invalid-date-time]
               :validator xsd-date-time?}
   :replies {:functional true
             :if-invalid [:must :invalid-replies]
             :validator (fn [pv] (object-reference-or-faults pv #{"Collection"
                                                                  "OrderedCollection"}
                                                             :must
                                                             :invalid-replies))}
   :radius {:functional true
            :if-invalid [:must :invalid-positive-number]
            :validator (fn [pv] (and (xsd-float? pv) (> pv 0)))}
   :rel {:functional false
         :if-invalid [:must :invalid-link-relation]
         ;; TODO: this is not really good enough.
         :validator (fn [pv] (truthy? (re-matches #"[a-zA-A0-9_\-\.\:\?/\\]*" pv)))}
   :relationship {;; this exists in the spec, but it doesn't seem to be required and it's
                  ;; extremely hazily specified. 
                  }
   :result {:functional false
            :if-invalid [:must :invalid-result]
            :validator (fn [pv]
                         (coll-object-reference-or-faults pv nil
                                                          :must
                                                          :invalid-result))}
   :startIndex {:functional true
                :if-invalid [:must :invalid-start-index]
                :validator xsd-non-negative-integer?}
   :start-time {:functional true
                :if-invalid [:must :invalid-date-time]
                :validator xsd-date-time?}
   :subject {:functional true
             :if-invalid [:must :invalid-subject]
             :if-missing [:minor :no-relationship-subject]
             :required (fn [x] (has-type? x "Relationship"))
             :validator (fn [pv] (object-reference-or-faults pv nil
                                                             :must
                                                             :invalid-subject))}
   :summary {:functional false
             :if-invalid [:must :invalid-summary]
             ;; TODO: HTML formatting is allowed, but other forms of formatting
             ;; are not. Can this be validated?
             :validator string?}
   :tag {:functional false
         :if-invalid [:must :invalid-tag]
         :validator (fn [pv]
                      (coll-object-reference-or-faults pv nil
                                                       :must
                                                       :invalid-tag))}
   :target {:functional false
            :if-invalid [:must :invalid-target]
            :validator (fn [pv]
                         (coll-object-reference-or-faults pv nil
                                                          :must
                                                          :invalid-target))}
   :to {:functional false
        :if-invalid [:must :invalid-to]
        :validator (fn [pv]
                     (coll-object-reference-or-faults pv actor-types
                                                      :must
                                                      :invalid-to))}
   :totalItems {:functional true
                :if-invalid [:must :invalid-total-items]
                :validator xsd-non-negative-integer?}
   :type {:functional false
          :if-missing [:minor :no-type]
          :if-invalid [:must :invalid-type]
          ;; strictly, it's an `anyURI`, but realistically these are not checkable.
          :validator string?}
   :units {:functional true
           :if-invalid [:must :invalid-units]
           ;; the narrative says that `anyURI`, but actually unless it's a recognised
           ;; unit the property is useless. These are the units explicitly specified.
           :validator (fn [pv] (truthy? (#{"cm" "feet" "inches" "km" "m" "miles"} pv)))}
   :updated {:functional true
             :if-invalid [:must :invalid-updated]
             :validator xsd-date-time?}
   :url {:functional false
         :if-invalid [:must :invalid-url-property]
         :validator (fn [pv] (object-or-uri? pv "Link"))}
   :width {:functional true
           :if-invalid [:must :invalid-width]
           :validator xsd-non-negative-integer?}})

(defn check-property-required
  "Check whether this `prop` of this `obj` is required with respect to 
   this `clause`; if it is both required and missing, return a list of
   one fault; else return `nil`."
  [obj prop clause]
  (let [required (:required clause)
        [severity token] (:if-missing clause)]
    (when required
      (when
       (and (apply required (list obj)) (not (obj prop)))
        (list (make-fault-object severity token))))))

(defn check-property-valid
  "Check that this `prop` of this `obj` is valid with respect to this `clause`.
   
   return `nil` if no faults are found, else a list of faults."
  [obj prop clause]
  ;; (info "obj" obj "prop" prop "clause" clause)
  (let [val (obj prop)
        validator (:validator clause)
        [severity token] (:if-invalid clause)]
    (when (and val validator)
      (let [r (apply validator (list val))
            f (list (make-fault-object severity token))]
        (cond
          (true? r) nil
          (nil? r) nil ;; that's OK, too, because it's a return
                       ;; from an 'or-faults' function which did not
                       ;; return faults
          (fault-list? r) (concat f r)
          (false? r) f
          :else (doall
                 (warn "Unexpected return value from validator"
                       {:return r
                        :arguments {:object obj
                                    :property prop
                                    :clause clause}})
                 f))))))

(defn check-property [obj prop]
  (assert (map? obj))
  (assert (keyword? prop))
  (let [clause (object-expected-properties prop)]
    (concat-non-empty
     (check-property-required obj prop clause)
     (check-property-valid obj prop clause))))

(defn properties-faults
  "Return a lost of faults found on properties of the object `x`, or
   `nil` if none are."
  [x]
  (apply
   concat-non-empty
   (let [props (set (keys x))
         required (set
                   (filter
                    #((object-expected-properties %) :required)
                    (keys object-expected-properties)))]
     (map
      (fn [p] (check-property x p))
      (union props required)))))

(defn object-faults
  "Return a list of faults found in object `x`, or `nil` if none are.
   
   If `expected-type` is also passed, verify that `x` has `expected-type`.
   `expected-type` may be passed as a string or as a set of strings. Detailed
   verification of the particular features of types is not done here."

  ;; TODO: many more properties which are nor required, nevertheless have required
  ;; property TYPES as detailed in
  ;; https://www.w3.org/TR/activitystreams-vocabulary/#properties
  ;; if these properties are present, these types should be checked.
  ([x]
   (concat-non-empty
    (remove empty?
            (list
             (when-not (map? x)
               (make-fault-object :critical :not-an-object))
             (when-not
              (has-context? x)
               (make-fault-object :should :no-context))
             (when-not (:type x)
               (make-fault-object :minor :no-type))
             (when-not (and (map? x) (contains? x :id))
               (make-fault-object :minor :no-id-transient))))
    (properties-faults x)))
  ([x expected-type]
   (concat-non-empty
    (object-faults x)
    (when expected-type
      (list
       (has-type-or-fault x expected-type :critical :unexpected-type))))))

