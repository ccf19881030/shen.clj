(ns shen.core
  (:use [clojure.java.io :only (file reader writer)]
        [clojure.pprint :only (pprint)]
        [clojure.set :only (intersection)])
  (:require [clojure.string :as string])
  (:import [java.io StringReader PushbackReader]
           [java.util.regex Pattern]))

(def ^:dynamic *language* "Clojure")
(def ^:dynamic *implementation* (str "Clojure " (clojure-version)
                           " [jvm "(System/getProperty "java.version")"]"))
(def ^:dynamic *port* "0.1.0")
(def ^:dynamic *porters* "Håkan Råberg")

(def shen-namespaces '[sys
                       core
                       writer
                       load
                       macros
                       prolog
                       reader
                       sequent
                       toplevel
                       track
                       t-star
                       yacc
                       declarations
                       types])

(def cleanup-symbols-pattern
  (re-pattern (str "(\\s+|\\()("
                   (string/join "|" (map #(Pattern/quote %) [":" ";" "{" "}" ":-"
                                                             "/." "@p" "@s" "@v"
                                                             "shen-@s-macro"
                                                             "shen-@v-help"
                                                             "shen-i/o-macro"
                                                             "shen-put/get-macro"
                                                             "XV/Y"]))
                   ")(\\s*\\)|\\s+?)"
                   "(?!~)")))

(defn cleanup-symbols-before
  [kl] (string/replace kl
                       cleanup-symbols-pattern
                       "$1(intern \"$2\")$3"))

(defn cleanup-symbols-after
  ([clj] (cleanup-symbols-after clj #{}))
  ([clj scope]
     (condp some [clj]
       scope clj
       symbol? (list 'quote clj)
       list? (if (empty? clj)
               clj
               (let [[fst & rst] clj
                     scope (condp get fst
                             #{'defun} (into scope (flatten (take 2 rst)))
                             #{'let 'lambda} (conj scope (first rst))
                             scope)
                     fst (if (list? fst)
                           (if (= 'intern (first fst))
                             (list 'clojure.core/resolve fst)
                             (cleanup-symbols-after fst scope))
                           fst)]
                 (cons fst (map #(cleanup-symbols-after % scope) rst))))
       clj)))

(defn read-kl [kl]
  (with-open [r (PushbackReader. (StringReader. (cleanup-symbols-before kl)))]
    (doall
     (map cleanup-symbols-after
          (take-while (complement nil?)
                      (repeatedly #(read r false nil)))))))

(defn read-kl-file [file]
  (try
    (cons (list 'clojure.core/println (str file)) (read-kl (slurp file)))
    (catch Exception e
      (println file e))))

(defn kl-files-in [dir]
  (filter #(re-find #".*.kl$" (str %))
          (file-seq (file dir))))

(defn read-all-kl-files
  ([] (read-all-kl-files "shen/klambda"))
  ([dir] (map read-kl-file (kl-files-in dir))))

(defn header [namespace exclusions]
  (list 'ns namespace
        (cons :use '[shen.primitives])
        (list :refer-clojure :exclude (vec exclusions))))

(def missing-declarations '#{shen-absarray? shen-kl-to-lisp byte->string FORMAT READ-CHAR})

(defn declarations [clj]
  (into missing-declarations
        (filter symbol?
               (map second (filter #(= 'defun (first %)) clj)))))

(defn write-clj-file [dir name forms exclusions]
  (with-open [w (writer (file dir (str name ".clj")))]
    (binding [*out* w]
      (doseq [form (cons (header (symbol name) exclusions) forms)]
        (pprint form)
        (println)))))

(defn write-all-kl-files-as-clj
  ([] (write-all-kl-files-as-clj "shen/klambda" "shen/platforms/clj"))
  ([dir to-dir]
     (.mkdirs (file to-dir))
     (doseq [f (kl-files-in dir)]
       (let [name (string/replace (.getName f) #".kl$" "")]
         (write-clj-file to-dir name (read-kl-file f))))))

(defn ns-symbols [ns]
  (set (map first (ns-publics ns))))

(defn write-all-kl-files-as-single-clj
  ([] (write-all-kl-files-as-single-clj "shen/klambda" "shen/platforms/clj"))
  ([dir to-dir]
     (doall
      (.mkdirs (file to-dir))
      (let [shen (mapcat read-kl-file (map #(file dir (str % ".kl")) shen-namespaces))
            dcl (declarations shen)
            exclusions (intersection (into (ns-symbols 'shen.primitives) dcl) (ns-symbols 'clojure.core))]
        (write-clj-file to-dir "shen" (cons (cons 'clojure.core/declare dcl) (remove string? shen)) (sort exclusions))))))
