(ns leiningen.sub
  (:use [leiningen.core :only [apply-task exit read-project
                               task-not-found]]))


(defn run-sub-proj
  [sub-proj-dir task-name args]
  (println "Reading project from" sub-proj-dir)
  (let [sub-project (read-project (str sub-proj-dir "/project.clj"))]
    (apply-task task-name sub-project args task-not-found)))


(defn sub
  "Run task for all subprojects"
  [project task-name & args]
  (if (empty? (:sub project))
    (println "No subprojects defined. Define with :sub key in project.clj, e.g.

      :sub [\"modules/dep1\" \"modules/proj-common\"]

Note: Each sub-project directory should have its own project.clj file")
    (when-let [code (reduce (fn [a b]
                              (let [a (if (= 0 a) nil a)
                                    b (if (= 0 b) nil b)]
                                (or a b)))
                            (map #(let [r (run-sub-proj % task-name args)]
                                    (if (and (integer? r) (pos? r)) r 0))
                                 (:sub project)))]
      (exit code))))
