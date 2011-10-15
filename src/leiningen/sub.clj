(ns leiningen.sub
  (:use [leiningen.core :only [apply-task read-project task-not-found]]))


(defn run-sub-proj
  [sub-proj-dir]
  (println "Reading project from " sub-proj-dir)
  (let [sub-project (read-project (str sub-proj-dir "/project.clj"))]
    (apply-task task-name sub-project args task-not-found)))


(defn sub
  "Run task for all subprojects"
  [project task-name & args]
  (if (empty? (:sub project))
    (println "No subprojects defined. Define with :sub key in project.clj, e.g.

      :sub [\"modules/dep1\" \"modules/proj-common\"]

Note: Each sub-project directory should have its own project.clj file")
    (reduce or
            (map run-sub-proj (:sub project)))))
