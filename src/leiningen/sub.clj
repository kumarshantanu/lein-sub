(ns leiningen.sub
  (:require [clojure.string :as str]
            [leiningen.core.main :as main]
            [leiningen.core.project :as project]
            [com.stuartsierra.dependency :as dep]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.tools.cli :as cli])
  (:import (java.io PushbackReader File)))

(def ^:private cache-path "target/sub-cache.edn")

(defn ^:private to-message
  [^Throwable throwable]
  (or (.getMessage throwable)
      (-> throwable class .getName)))

;; Caching on md5 checksum, rather than DTM, would yield fewer
;; false positives on the invalid check, but probably not worth
;; the time & effort.

(defn ^:private add-source
  "Adds a source to the :sources key of the cached dependency structure;
  path key and DTM value."
  [m path]
  (assoc-in m [:sources path] (-> path io/file .lastModified)))

(defn ^:private source-is-valid?
  [path date-time-modified]
  (let [file (io/file path)]
    (and (.exists file)
         (= date-time-modified (.lastModified file)))))

(defn ^:private cache-is-valid?
  "Checks the sources to ensure that all of them exist and match the provided
  date-time-modified."
  [cache]
  (->> cache
       :sources
       (every? (fn [[path dtm]] (source-is-valid? path dtm)))))

(defn ^:private normalize-dependency
  "Noticed that in some cases, a unqualified dependency comes out as foo/foo instead of just foo.
  Specifically saw this with clout. Not sure why."
  [[artifact & rest :as dependency]]
  (let [simple-name (name artifact)]
    (if (= (namespace artifact) simple-name)
      (apply vector (symbol simple-name) rest)
      dependency)))

(defn ^:private build-type
  "Categorize the build, so that we can ensure that all CLJS builds occur last.
  Currently, when bulding a multi-project, once any project uses the lein-clsbuild
  plugin, all subsequent projects will attempt to build using cljsbuild - and generate
  warnings about missing :cljsbuild entry, etc."
  [project]
  (if (:cljsbuild project)
    :cljs
    :clj))

(defn ^:private gen-project-db
  "Generates the initial project database, which will ultimately be written as the cache file.

  The :projects key contains a map from project name (a symbol, not the string name of the
  sub directory) to the read (and initialized) sub-project.

  The :sources key is a map from file path to file DTM, used to determine if the cached
  data is still valid. As project files are read, the project.clj is added as a source.

  sub-module-paths
  : from the :sub key of the root project"
  [sub-module-paths]
  (let [start-ms (System/currentTimeMillis)
        result (do
                 (print "Determining project dependency ordering: ")
                 (flush)
                 (->>
                   sub-module-paths
                   ;; Should be able to do a pmap here, but then we only see
                   ;; partial dependencies (seems like nothing from a :dependency-set
                   ;; makes it). Leiningen is not thread safe and it's hard to
                   ;; debug or figure out what's truly going on.
                   (mapv (fn [dir]
                           (let [file (str dir "/project.clj")
                                 ;; Because of the lein-parent plugin, and use of managed dependencies,
                                 ;; it is necessary to fully read and initialize the project, in order
                                 ;; to determine dependencies.
                                 project (project/read file [:base :system :provided :dev])
                                 {proj-group-str :group
                                  proj-name-str :name} project
                                 project-name (symbol proj-group-str proj-name-str)]
                             (print ".")
                             (flush)
                             (-> project
                                 (select-keys [:group :name :version :root :dependencies])
                                 (update-in [:dependencies] #(map normalize-dependency %))
                                 (assoc :relative-dir dir
                                        :project-name project-name
                                        :build-type (build-type project))
                                 (vary-meta assoc ::source file)))))
                   (reduce (fn [m project]
                             (let [source (-> project meta ::source)]
                               (-> m
                                   (add-source source)
                                   (assoc-in [:projects (:project-name project)] project))))
                           (add-source {} "project.clj"))))]
    (printf " [%.2f s]%n"
            (-> (System/currentTimeMillis)
                (- start-ms)
                float
                (/ 1000.)))
    (flush)
    result))

(defn ^:private order-projects
  [projects]
  (let [project-artifacts (-> projects keys set)
        tuples (for [from-proj (vals projects)
                     :let [from-artifact (:project-name from-proj)
                           to-artifacts (->> from-proj
                                             :dependencies
                                             (map first)
                                             (keep project-artifacts))]
                     ;; Projects only exist in the graph if there's an edge, so always ensure
                     ;; there's at least one edge.
                     to-artifact (conj to-artifacts ::primordial)]
                 [from-artifact to-artifact])
        graph (reduce (fn [g [from to]]
                        (dep/depend g from to))
                      (dep/graph)
                      tuples)]
    (->> graph
         dep/topo-sort
         ;; And convert each project name back into the project data
         ;; This is where the ::primordial is filtered out.
         (keep projects))))


(defn ^:private create-and-cache-project-data
  "Return project build order by performing a depth-first
  walk of the dependency tree - returning paths from leaf to root.

  cache-path
  : path to where the cache should be written

  sub-module-paths
  : seq of strings, subfolders that contain projects to be included"
  [sub-module-paths]
  (let [project-db (gen-project-db sub-module-paths)
        subs-ordered (->> project-db
                          :projects
                          order-projects
                          ;; Make sure CLJS builds are last.
                          (group-by :build-type)
                          ((juxt :clj :cljs))
                          (apply concat)
                          (map :relative-dir))
        cache-data (assoc project-db :build-order subs-ordered)]
    (when-not (= (count sub-module-paths)
                 (count subs-ordered))
      (main/abort (format "%d subs after ordering, vs. %d before."
                          (count subs-ordered)
                          (count sub-module-paths))))

    (io/make-parents cache-path)

    (with-open [s (io/writer (io/file cache-path))]
      (pprint/write cache-data :stream s))

    cache-data))

(defn ^:private read-cache-file
  "Reads the project database cache, if present.
  Checks all sources to see if they have changed.
  If all are current, returns the cached dependency data.

  Returns nil otherwise."
  []
  (let [^File cache-file (io/file cache-path)]

    (if (not (.exists cache-file))
      nil
      (let [project-data
            (try
              (main/debug (format "Reading from `%s'." cache-path))

              (with-open [reader (io/reader cache-file)
                          pushback-reader (PushbackReader. reader)]
                (edn/read pushback-reader))
              (catch Throwable t
                (main/warn (format "Unable to read from `%s': %s"
                                   cache-file
                                   (to-message t)))
                nil))]
        (cond

          (nil? project-data)
          nil

          (cache-is-valid? project-data)
          project-data)))))

(defn read-project-data
  "Gets the project data, starting from the root project.

  Returns data from the cache or (failing that) does the more expensive
  job of building the data fresh."
  [root-project]
  (or
    (read-cache-file)
    (create-and-cache-project-data (:sub root-project))))

(defn ^:private apply-task-to-subproject
  [sub-proj-dir task-name args]
  (main/info "Reading project from" sub-proj-dir)
  (let [sub-project (project/init-project
                     (project/read (str sub-proj-dir "/project.clj")))
        new-task-name (main/lookup-alias task-name sub-project)]
    (main/apply-task new-task-name sub-project args)))

(def ^:private cli-options
  [["-s" "--submodules LIST" "Execute task in just the indicated projects (colon separated list)."]
   ["-h" "--help" "This usage summary."]])

(defn ^:private resolve-arguments
  "Parse `args` and return [sub-projects task-name args]"
  [project args]
  (let [{:keys [options arguments errors summary]} (cli/parse-opts args cli-options)
        [task-name & task-arguments] arguments
        usage (fn [errors]
                (println "lein sub [options] task-name [arguments]")
                (println summary)

                (when (seq errors)
                  (println)
                  (doseq [e errors] (println e)))

                nil)]
    (cond

      (or (:help options)
          errors)
      (usage errors)

      (nil? task-name)
      (usage ["No task name was specified"])

      ;; For explicitly named subprojects, we expect the user to supply
      ;; valid names and execution order
      (:subprojects options)
      [(->> #"(?<!\\):"
            (str/split (:subprojects options))
            (map #(str/replace % #"\\:" ":"))
            vec)
       task-name
       task-arguments]

      ;; project contains :sub
      (seq (:sub project))
      [(-> project
           read-project-data
           :build-order)
       task-name
       task-arguments]

      ;; otherwise error
      :else
      (do
        (main/abort "No subprojects defined. Define with :sub key in project.clj, e.g.

    :sub [\"modules/dep1\" \"modules/proj-common\"]

or specify subproject dirs via command line:

    $ lein sub -s \"modules/dep1:modules/proj-common\" <task-name> [args]

Note: Each sub-project directory should have its own project.clj file")))))

(defn sub
  "Run task for all subprojects in dependency order"
  {:pass-through-help true}
  [project & args]
  (let [[subprojects task-name args] (resolve-arguments project args)]
    ;; subprojects will be nil if there's a parse error
    (doseq [each subprojects]
      (apply-task-to-subproject each task-name args))))
