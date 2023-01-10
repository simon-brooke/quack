(ns dog-and-duck.quack.core
  (:require [clojure.data.json :as json :refer [read-str]]
            [clojure.java.io :refer [resource]]
            [clojure.pprint :as pprint]
            [clojure.string :refer [join]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.walk :refer [keywordize-keys]]
            [dog-and-duck.quack.constants :refer [severity]]
            [dog-and-duck.quack.objects :refer [object-faults]]
            [dog-and-duck.quack.utils :refer [filter-severity]]
            [hiccup.core :refer [html]]
            [scot.weft.i18n.core :refer [get-message *config*]]
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
  ;;"https://simon-brooke.github.io/quack/style.css"
  "resources/public/css/style.css")

(def cli-options
  ;; An option with a required argument
  [["-i" "--input SOURCE" "The file or URL to validate"
    :default "standard input"]
   ["-o" "--output DEST" "The file to write to, defaults to standard out"
    :default "standard output"]
   ["-f" "--format FORMAT" "The format to output, one of `edn` `csv` `html`"
    :default :edn
    :parse-fn #(keyword %)
    :validate [#(#{:csv :edn :html} %) "Expect one of `edn` `csv` `html`"]]
   ["-l" "--language LANG" "The ISO 639-1 code for the language to output"
    :default (-> (locale/get-default) locale/to-language-tag)]
   ["-s" "--severity LEVEL" "The minimum severity of faults to report"
    :default :info
    :parse-fn #(keyword %)
    :validate [#(severity %) (join " "
                                   (cons
                                    "Expected one of"
                                    (map name severity)))]]
   ["-h" "--help"]])

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
   [:h2 :text-analysed]
   [:pre {:class "ft-syntax-highlight"
          :data-syntax "javascript"
          :data-syntax-theme "bootstrap"
          :data-ui-theme "light"}
    (with-out-str
      (json/pprint
       (read-str
        (slurp
         (:input options)))))]])

(defn output-html
  [faults options]
  (let [source-name (if (= (:input options) *in*) "Standard input" (str (:input options)))
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
        [:h2 (get-message :faults-found)]
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
                       :input (if (= (:input (:options opts)) "standard input")
                                *in*
                                (:input (:options opts)))
                       :output (if (= (:output (:options opts)) "standard output")
                                 *out*
                                 (:output (:options opts))))]
    ;;(println options)
    (when (:help options)
      (println (:summary opts)))
    (when (:errors opts)
      (println (:errors opts)))
    (when-not (or (:help options) (:errors options))
      (binding [*config* (assoc *config* :default-language (:language options))]
        (output
         (validate (:input options))
         options)))))