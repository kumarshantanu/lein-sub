(def version (clojure.string/trim (:out (clojure.java.shell/sh "git" "describe" "--always"))))

(defproject subtest/child3 version
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [subtest/child2 ~version]])
