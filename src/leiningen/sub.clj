(ns leiningen.sub
  (:require [leiningen.core.main :as main]
            [leiningen.core.project :as project]))

(declare sub)

(defn apply-task-to-subproject
  [sub-proj-dir task-name args]
  (println "Reading project from" sub-proj-dir)
  (let [sub-project (project/read (str sub-proj-dir "/project.clj"))
        nested-subprojects (:sub sub-project)]
    (if nested-subprojects
      (apply sub sub-project task-name args)
      (main/apply-task task-name sub-project args))))

(defn run-subproject [task-name args total-result sub]
  (if (zero? total-result)
    (let [result (apply-task-to-subproject sub task-name args)]
      (if (and (integer? result) (pos? result)) result 0))
    total-result))

(defn sub
  "Run task for all subprojects"
  [project task-name & args]
  (if-let [subprojects (:sub project)]
    (reduce (partial run-subproject task-name args)
            0 subprojects)
    (println "No subprojects defined. Define with :sub key in project.clj, e.g.

      :sub [\"modules/dep1\" \"modules/proj-common\"]

Note: Each sub-project directory should have its own project.clj file")))
