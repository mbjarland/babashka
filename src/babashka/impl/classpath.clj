(ns babashka.impl.classpath
  {:no-doc true}
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.util.jar JarFile]))

(set! *warn-on-reflection* true)

(defprotocol IResourceResolver
  (getResource [this path opts]))

(deftype DirectoryResolver [path]
  IResourceResolver
  (getResource [this resource-path {:keys [:url?]}]
    (let [f (io/file path resource-path)]
      (when (.exists f)
        (if url?
          (java.net.URL. (str "file:"
                              (.getCanonicalPath f)))
          {:file (.getCanonicalPath f)
           :source (slurp f)})))))

(defn path-from-jar
  [^java.io.File jar-file path {:keys [:url?]}]
  (with-open [jar (JarFile. jar-file)]
    (when-let [entry (.getEntry jar path)]
      (if url?
        (java.net.URL.
         (str "jar:file:" (.getCanonicalPath jar-file) "!/" path))
        {:file path
         :source (slurp (.getInputStream jar entry))}))))

(deftype JarFileResolver [path]
  IResourceResolver
  (getResource [this resource-path opts]
    (path-from-jar path resource-path opts)))

(defn part->entry [part]
  (if (str/ends-with? part ".jar")
    (JarFileResolver. (io/file part))
    (DirectoryResolver. (io/file part))))

(deftype Loader [entries]
  IResourceResolver
  (getResource [this resource-path opts]
    (some #(getResource % resource-path opts) entries)))

(defn loader [^String classpath]
  (let [parts (.split classpath (System/getProperty "path.separator"))
        entries (map part->entry parts)]
    (Loader. entries)))

(defn source-for-namespace [loader namespace opts]
  (let [ns-str (name namespace)
        ^String ns-str (munge ns-str)
        path (.replace ns-str "." (System/getProperty "file.separator"))
        ;; TODO: check .bb .clj etc while opening the jar file, this might be
        ;; faster.
        paths (map #(str path %) [".bb" ".clj" ".cljc"])]
    (some #(getResource loader % opts) paths)))

;;;; Scratch

(comment
  (def cp "src:feature-xml:feature-core-async:feature-yaml:feature-csv:feature-transit:feature-java-time:feature-java-nio:sci/src:babashka.curl/src:babashka.pods/src:resources:sci/resources:/Users/borkdude/.m2/repository/com/cognitect/transit-java/1.0.343/transit-java-1.0.343.jar:/Users/borkdude/.m2/repository/org/clojure/clojure/1.10.2-alpha1/clojure-1.10.2-alpha1.jar:/Users/borkdude/.m2/repository/commons-codec/commons-codec/1.10/commons-codec-1.10.jar:/Users/borkdude/.m2/repository/org/clojure/tools.analyzer/1.0.0/tools.analyzer-1.0.0.jar:/Users/borkdude/.m2/repository/org/clojure/tools.logging/0.6.0/tools.logging-0.6.0.jar:/Users/borkdude/.m2/repository/org/clojure/core.specs.alpha/0.2.44/core.specs.alpha-0.2.44.jar:/Users/borkdude/.m2/repository/org/clojure/spec.alpha/0.2.187/spec.alpha-0.2.187.jar:/Users/borkdude/.m2/repository/org/clojure/tools.cli/1.0.194/tools.cli-1.0.194.jar:/Users/borkdude/.m2/repository/org/clojure/tools.analyzer.jvm/1.0.0/tools.analyzer.jvm-1.0.0.jar:/Users/borkdude/.m2/repository/borkdude/graal.locking/0.0.2/graal.locking-0.0.2.jar:/Users/borkdude/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-cbor/2.10.2/jackson-dataformat-cbor-2.10.2.jar:/Users/borkdude/.m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar:/Users/borkdude/.m2/repository/org/flatland/ordered/1.5.9/ordered-1.5.9.jar:/Users/borkdude/.m2/repository/org/postgresql/postgresql/42.2.12/postgresql-42.2.12.jar:/Users/borkdude/.m2/repository/fipp/fipp/0.6.22/fipp-0.6.22.jar:/Users/borkdude/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.10.2/jackson-core-2.10.2.jar:/Users/borkdude/.m2/repository/org/yaml/snakeyaml/1.25/snakeyaml-1.25.jar:/Users/borkdude/.m2/repository/org/ow2/asm/asm/5.2/asm-5.2.jar:/Users/borkdude/.gitlibs/libs/clj-commons/conch/9aa7724e925cb8bf163e0b62486dd420b84e5f0b/src:/Users/borkdude/.m2/repository/org/javassist/javassist/3.18.1-GA/javassist-3.18.1-GA.jar:/Users/borkdude/.m2/repository/seancorfield/next.jdbc/1.0.424/next.jdbc-1.0.424.jar:/Users/borkdude/.m2/repository/org/clojure/data.xml/0.2.0-alpha6/data.xml-0.2.0-alpha6.jar:/Users/borkdude/.m2/repository/org/msgpack/msgpack/0.6.12/msgpack-0.6.12.jar:/Users/borkdude/.m2/repository/borkdude/edamame/0.0.11-alpha.9/edamame-0.0.11-alpha.9.jar:/Users/borkdude/.m2/repository/org/clojure/data.csv/1.0.0/data.csv-1.0.0.jar:/Users/borkdude/.m2/repository/com/cognitect/transit-clj/1.0.324/transit-clj-1.0.324.jar:/Users/borkdude/.m2/repository/clj-commons/clj-yaml/0.7.1/clj-yaml-0.7.1.jar:/Users/borkdude/.m2/repository/org/clojure/core.rrb-vector/0.1.1/core.rrb-vector-0.1.1.jar:/Users/borkdude/.m2/repository/persistent-sorted-set/persistent-sorted-set/0.1.2/persistent-sorted-set-0.1.2.jar:/Users/borkdude/.m2/repository/cheshire/cheshire/5.10.0/cheshire-5.10.0.jar:/Users/borkdude/.m2/repository/tigris/tigris/0.1.2/tigris-0.1.2.jar:/Users/borkdude/.m2/repository/org/clojure/tools.reader/1.3.2/tools.reader-1.3.2.jar:/Users/borkdude/.m2/repository/datascript/datascript/0.18.11/datascript-0.18.11.jar:/Users/borkdude/.m2/repository/org/hsqldb/hsqldb/2.4.0/hsqldb-2.4.0.jar:/Users/borkdude/.m2/repository/org/clojure/core.memoize/0.8.2/core.memoize-0.8.2.jar:/Users/borkdude/.m2/repository/org/clojure/data.priority-map/0.0.7/data.priority-map-0.0.7.jar:/Users/borkdude/.m2/repository/org/clojure/java.data/1.0.64/java.data-1.0.64.jar:/Users/borkdude/.m2/repository/borkdude/sci.impl.reflector/0.0.1/sci.impl.reflector-0.0.1.jar:/Users/borkdude/.m2/repository/nrepl/bencode/1.1.0/bencode-1.1.0.jar:/Users/borkdude/.m2/repository/org/clojure/core.cache/0.8.2/core.cache-0.8.2.jar:/Users/borkdude/.m2/repository/org/clojure/core.async/1.1.587/core.async-1.1.587.jar:/Users/borkdude/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-smile/2.10.2/jackson-dataformat-smile-2.10.2.jar:/Users/borkdude/.m2/repository/org/clojure/data.codec/0.1.0/data.codec-0.1.0.jar:/Users/borkdude/.m2/repository/javax/xml/bind/jaxb-api/2.3.0/jaxb-api-2.3.0.jar")
  (def l (loader cp))
  (source-for-namespace l 'babashka.impl.cheshire nil)
  (time (:file (source-for-namespace l 'cheshire.core nil))) ;; 20ms -> 4ms
  (def jf (JarFile. (io/file "/Users/borkdude/.m2/repository/cheshire/cheshire/5.10.0/cheshire-5.10.0.jar")))
  (.getEntry jf "cheshire/core.clj")
  )
