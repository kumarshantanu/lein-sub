(ns leiningen.sub
  (:require [clojure.string :as str]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]))

(defn proj-version [proj]
  [(symbol (:group proj) (:name proj)) (:version proj)])

(defn apply-task-to-subproject
  [sub-project task-name args]
  (println "Building project " (proj-version sub-project))
  (let [new-task-name (main/lookup-alias task-name sub-project)]
    (main/apply-task new-task-name sub-project args)))

(defn apply-task-to-subproject-dir
  [sub-project-dir task-name args]
  (let [ sub-project (project/init-project (project/read (str sub-project-dir "/project.clj")))]
    (apply-task-to-subproject sub-project task-name args)))

(defn read-all-subprojects
  [sub-proj-dirs]
  (map #(project/init-project (project/read (str % "/project.clj"))) sub-proj-dirs))

(defn perform-in-order [enriched-projects task-name args]
  "Applies tasks to all projects in a recursion, unless finished or a cycle was encountered"
  (let [ build-this-round (filter #(empty? (:internal-deps %)) enriched-projects)
         built-this-round (into #{} (map #(proj-version %) build-this-round))
         build-next-round (filter #(not (empty? (:internal-deps %))) enriched-projects)
         build-next-round (map #(assoc % 
          :internal-deps 
          (filter
            (fn [dep](not (contains? built-this-round dep))) 
            (:internal-deps %))) build-next-round)]
         (if (and (empty? build-this-round) (not (empty? build-next-round)))
          (main/abort (str "Cycle dependecy, cannot resolve proper build order for projects: " 
            (into #{} (map #(proj-version %) build-next-round))))
          (if (not (empty? build-this-round))
            (do
              (doseq [each build-this-round]
                (apply-task-to-subproject each task-name args))
              (perform-in-order build-next-round task-name args))))))


(defn enriched-projects [subprojects]
  "Adds :internal-deps vector with all dependencies that are within this build-tree"
  (let [all-subprojects (into #{} (map #(proj-version %) subprojects))
        enriched-projects
          (map (fn [project](assoc project 
            :internal-deps 
            (into [] (filterv #(contains? all-subprojects %) (:dependencies project))))) subprojects)]
        enriched-projects))

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
    (cond
      ;; -d would trigger "discovery mode" build order
      (= "-d" (first args))
      (perform-in-order (enriched-projects (read-all-subprojects subprojects)) task-name (rest args))
      ;; the normal way will follow artifacts order in the build script
      :else
      (doseq [each subprojects]
        (apply-task-to-subproject-dir each task-name args)))))
