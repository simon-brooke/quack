(ns dog-and-duck.quack.core
  (:require [clojure.data.json :as json :refer [read-str]]
            [clojure.java.io :refer [resource]]
            [clojure.pprint :as pprint]
            [clojure.string :refer [join]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.walk :refer [keywordize-keys]]
            [dog-and-duck.quack.control-variables :refer [*reify-refs*]]
            [dog-and-duck.quack.constants :refer [severity]]
            [dog-and-duck.quack.objects :refer [object-faults]]
            [dog-and-duck.quack.utils :refer [filter-severity safe-keyword]]
            [hiccup.core :refer [html]]
            [scot.weft.i18n.core :refer [*config*
                                         get-message
                                         parse-accept-language-header]]
            [trptr.java-wrapper.locale :as locale])
  (:gen-class))

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

(def ^:const stylesheet-url
  ;; TODO: fix this to github pages before go live
  ;;"https://simon-brooke.github.io/quack/css/style.css"
  "../css/style.css")

(def cli-options
  ;; An option with a required argument
  [["-i" "--input SOURCE" (get-message :cli-help-input)
    :default "standard in"]
   ["-o" "--output DEST" (get-message :cli-help-output)
    :default "standard out"]
   ["-f" "--format FORMAT" (get-message :cli-help-format)
    :default :edn
    :parse-fn #(safe-keyword %)
    :validate [#(#{:csv :edn :json :html} %) (get-message :cli-expected-format)]]
   ["-l" "--language LANG" (get-message :cli-help-language)
    :default (-> (locale/get-default) locale/to-language-tag)
    :validate [#(try (parse-accept-language-header %)
                     (catch Exception _ false))
               (get-message :cli-expected-language)]]
   ["-s" "--severity LEVEL" (get-message :cli-help-severity)
    :default :info
    :parse-fn #(safe-keyword %)
    :validate [#(severity %) (join " "
                                   (cons
                                    (get-message :cli-expected-one)
                                    (map name severity)))]]
   ["-r" "--reify" (get-message :cli-help-reify)]
   ["-h" "--help" (get-message :cli-help-help)]])

(defn validate
  [source]
  (println (str "Reading " source))
  (let [input (read-str (slurp source))]
    (cond (map? input) (object-faults (keywordize-keys input))
          (and (coll? input)
               (every? map? input)) (map #(object-faults
                                           (keywordize-keys %)
                                           input)))))

(defn output-csv
  [faults]
  (let [cols (set (reduce concat (map keys faults)))]
    (with-out-str
      (if-not (empty? faults)
        (doall
         (println (join ", " (map name cols)))
         (map
          #(println (join ", " (map (fn [p] (p %)) cols)))
          faults))
        (println (get-message :no-faults-found))))))

(defn output-json
  [faults]
  (with-out-str
    (json/pprint (if-not (empty? faults)
                   faults
                   (get-message :no-faults-found)))))

(defn html-header-row
  [cols]
  (apply vector (cons :tr (map #(vector :th (name %)) cols))))

(defn html-fault-row
  [fault cols]
  (apply
   vector
   (cons :tr
         (cons
          {:class (name (or (:severity fault) :info))}
          (map (fn [col] (vector :td (col fault))) cols)))))

(defn- version-string []
  (join
   " "
   ["dog-and-duck/quack"
    (try
      (some->>
       (resource "META-INF/maven/dog-and-duck/quack/pom.properties")
       slurp
       (re-find #"version=(.*)")
       second)
      (catch Exception _ nil))]))

(defn- output-html-text-analysed
  [options]
  [:div
   {:class "text-analysed"}
   [:h2 (get-message :text-analysed)]
   [:pre {:class "ft-syntax-highlight"
          :data-syntax "javascript"
          :data-syntax-theme "bootstrap"
          :data-ui-theme "light"}
    (with-out-str
      (slurp
       (:input options)))]])

(defn output-html
  [faults options]
  (let [source-name (if (= (:input options) *in*) "standard in" (str (:input options)))
        title (join " " [(get-message :validation-report-for) source-name])
        cols (set (reduce concat (map keys faults)))
        version (version-string)]
    (str
     "<!DOCTYPE html>"
     (html
      [:html
       [:head
        [:title title]
        [:meta {:name "generator" :content version}]
        [:link {:rel "stylesheet" :media "screen" :href stylesheet-url :type "text/css"}]]
       [:body
        [:h1 title]
        [:p (join " " (remove nil? [(get-message :generated-on)
                                    (java.time.LocalDateTime/now)
                                    (get-message :by)
                                    version]))]
        [:h2 (join  " " (list (get-message :the-following)
                              (count faults)
                              (get-message :faults-found)))]
        (if-not
         (empty? faults)
          (apply
           vector
           :table
           (html-header-row cols)
           (map
            #(html-fault-row % cols)
            faults))
          [:p (get-message :no-faults-found)])
        (when-not (= (:input options) *in*)
          (output-html-text-analysed options))]]))))

(defn output
  "Output this `content` as directed by these `options`."
  [content options]
  (let [faults (filter-severity content (:severity options))]
    (spit (:output options)
          (case (:format options)
            :html (output-html faults options)
            :csv (output-csv faults)
            :json (output-json faults)
            (with-out-str (if-not (empty? faults)
                            (pprint/pprint faults)
                            (println (get-message :no-faults-found))))))))

(defn -main
  "Parse command line `args`, and, using the options found therein,
   validate one ActivityStreams document and exit."
  [& args]
  (let [opts (parse-opts args cli-options)
        options (assoc (:options opts)
                       :input (if (= (:input (:options opts)) "standard in")
                                *in*
                                (:input (:options opts)))
                       :output (if (= (:output (:options opts)) "standard out")
                                 *out*
                                 (:output (:options opts))))]
    ;;(println options)
    (when (:help options)
      (println (:summary opts)))
    (when (:errors opts)
      (println (:errors opts)))
    (when-not (or (:help options) (:errors options))
      (binding [*config* (assoc *config* :default-language (:language options))
                *reify-refs* (:reify options)]
        (output
         (validate (:input options))
         options)))))