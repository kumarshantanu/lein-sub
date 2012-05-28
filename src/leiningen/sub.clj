(ns leiningen.sub
  (:require [leiningen.core.main :as main]
            [leiningen.core.project :as project]))

(declare sub-task)

(defn- apply-task-to-subproject
  [sub-proj-dir sub-args task-name args]
  (println "Reading project from" sub-proj-dir)
  (let [sub-project (project/read (str sub-proj-dir "/project.clj"))
        sub-project (reduce project/apply-middleware
                            sub-project (:middleware sub-project))
        nested-subprojects (map #(str sub-proj-dir "/" %1) (:sub sub-project))]
    (if (and (some #{:recursive} sub-args)
             (not (empty? nested-subprojects)))
      (sub-task (assoc sub-project :sub nested-subprojects) sub-args task-name args)
      (main/apply-task task-name sub-project args))))

(defn- run-subproject [sub-args task-name args total-result sub]
  (if (zero? total-result)
    (let [result (apply-task-to-subproject sub sub-args task-name args)]
      (if (and (integer? result) (pos? result)) result 0))
    total-result))

(defn- sub-task
  [project sub-args task-name args]
  (reduce (partial run-subproject sub-args task-name args)
          0 (:sub project))
  (if (some #{:recursive} sub-args)
    (main/apply-task task-name project args)))

(defn sub
  "Run task for all subprojects

USAGE: lein sub [:recursive] task [ARGS...]
Executes the given task on each sub directory as specified via the :sub
key in project.clj

If the :recursive option is present, sub projects will also be scanned for
nested subprojects themselves and so on, and the task will be executed in all
of the projects in the directory tree, including the top level project"
  [project & args]
  (if (:sub project)
    (let [[sub-args rest]  (split-with #(= (first %1) \:) args)
          sub-args         (map #(keyword (subs %1 1)) sub-args)
          [task-name args] rest]
      (sub-task project sub-args task-name args))
    (println "No subprojects defined. Define with :sub key in project.clj, e.g.

      :sub [\"modules/dep1\" \"modules/proj-common\"]

Note: Each sub-project directory should have its own project.clj file")))
