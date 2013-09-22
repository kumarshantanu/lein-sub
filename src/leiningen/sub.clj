(ns leiningen.sub
  (:require [clojure.string :as str]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]))


(defn apply-task-to-subproject
  [sub-proj-dir task-name args]
  (println "Reading project from" sub-proj-dir)
  (let [sub-project (project/init-project
                     (project/read (str sub-proj-dir "/project.clj")))
        new-task-name (main/lookup-alias task-name sub-project)]
    (main/apply-task new-task-name sub-project args)))


(defn resolve-subprojects
  "Parse `args` and return [sub-projects task-name args]"
  [project task-name args]
  (cond
   ;; -s "sub1:sub2" & more
   (= "-s" task-name)
   (if-not (> (count args) 1)
     (main/abort "Expected: -s <subprojects> task-name [args]")
     [(->> #"(?<!\\):"
           (str/split (first args))
           (map #(str/replace % #"\\:" ":"))
           vec)
      (second args)
      (drop 2 args)])
   ;; project contains :sub
   (seq (:sub project))
   [(:sub project) task-name args]
   ;; otherwise error
   :else
   (main/abort "No subprojects defined. Define with :sub key in project.clj, e.g.

    :sub [\"modules/dep1\" \"modules/proj-common\"]

or specify subproject dirs via command line:

    $ lein sub -s \"modules/dep1:modules/proj-common\" <task-name> [args]

Note: Each sub-project directory should have its own project.clj file")))


(defn sub
  "Run task for all subprojects"
  [project task-name & args]
  (let [[subprojects task-name args] (resolve-subprojects project task-name args)]
    (doseq [each subprojects]
      (apply-task-to-subproject each task-name args))))
