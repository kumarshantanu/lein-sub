(ns leiningen.sub
  (:require [leiningen.core.main :as main]
            [leiningen.core.project :as project]))


(defn apply-task-to-subproject
  [sub-proj-dir task-name args]
  (println "Reading project from" sub-proj-dir)
  (let [sub-project (project/init-project
                     (project/read (str sub-proj-dir "/project.clj")))
        new-task-name (main/lookup-alias task-name sub-project)]
    (main/apply-task new-task-name sub-project args)))

(defn sub
  "Run task for all subprojects"
  [project task-name & args]
  (if-let [subprojects (seq (:sub project))]
    (doseq [sub subprojects]
      (apply-task-to-subproject sub task-name args))
    (throw
     (ex-info
      "No subprojects defined. Define with :sub key in project.clj, e.g.

      :sub [\"modules/dep1\" \"modules/proj-common\"]

Note: Each sub-project directory should have its own project.clj file"
      {:exit-code 1}))))
