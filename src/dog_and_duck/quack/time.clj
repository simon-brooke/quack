(ns dog-and-duck.quack.time
  "Time, gentleman, please! Recognising and validating date time values."
  (:require [dog-and-duck.quack.utils :refer [cond-make-fault-object
                                              make-fault-object
                                              truthy?]]
            [scot.weft.i18n.core :refer [get-message]]
            [taoensso.timbre :refer [warn error]])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter DateTimeParseException]
           [javax.xml.datatype DatatypeFactory]))

;;;     Copyright (C) Simon Brooke, 2023

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

(defn xsd-date-time?
  "Return `true` if `value` matches the pattern for an 
   [xsd:dateTime](https://www.w3.org/TR/xmlschema11-2/#dateTime), else `false`"
  [^String value]
  (try
    (if (LocalDateTime/from (.parse DateTimeFormatter/ISO_DATE_TIME value)) true false)
    (catch DateTimeParseException _
      (warn (get-message :bad-date-time) ":" value)
      false)
    (catch Exception e
      (error "Exception thrown while parsing date" value e)
      false)))

(defn xsd-duration?
  "Return `true` if `value` matches the pattern for an 
   [xsd:duration](https://www.w3.org/TR/xmlschema11-2/#duration), else `false`"
  [value]
  (truthy?
   (and (string? value)
        (try (.newDuration (DatatypeFactory/newInstance) value)
             (catch IllegalArgumentException _
               (warn (get-message :bad-duration) ":" value)
               false)
             (catch Exception e
               (error "Exception thrown while parsing duration" value e)
               false)))))

(defn date-time-property-or-fault
  "If the value of this `property` of object `x` is a valid xsd:dateTime 
   value, return a fault object with this `token` and `severity`. 
   
   If `required?` is false and there is no such property, no fault will be
   returned."
  [x property severity token required?]
  (let [value (property x)]
    (if (and required? (not (x property)))
      (make-fault-object severity token)
      (cond-make-fault-object
       (and value (xsd-date-time? value)) severity token))))
