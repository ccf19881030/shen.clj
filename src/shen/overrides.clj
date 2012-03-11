(clojure.core/ns shen
  (:refer-clojure :only [])
  (:use [shen.primitives])
  (:require [clojure.core :as core]))

(clojure.core/comment "src/shen/overrides.clj")

(def ^:dynamic *language* "Clojure")
(def ^:dynamic *implementation* (core/str "Clojure " (core/clojure-version)
                                          " [jvm "(System/getProperty "java.version")"]"))
(def ^:dynamic *port* "0.1.0-SNAPSHOT")
(def ^:dynamic *porters* "Håkan Råberg")

(def ^:dynamic *stinput* core/*in*)
(def ^:dynamic *home-directory* (System/getProperty "user.dir"))

(defun
 (intern "@p")
 (V706 V707)
 (core/object-array ['shen-tuple V706 V707]))

(defun
 variable?
 (V702)
 (and (core/symbol? V702) (Character/isUpperCase (.charAt (core/name V702) 0))))

(defun
 boolean?
 (V746)
 (core/condp = V746
             true true
             false true
             (core/symbol "true") true
             (core/symbol "false") true
             false))

(defun
  macroexpand
  (V510)
  (let
      Y
    (shen-compose (core/drop-while core/symbol?
                                   (core/map value (value '*macros*))) V510)
    (if (= V510 Y) V510 (shen-walk macroexpand Y))))

(core/defn -main []
  (shen-shen))