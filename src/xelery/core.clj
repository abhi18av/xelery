(ns xelery.core
  (:require [clojure.xml :as xml]
            [clojure.zip :as zip]))

; Some constants
(def SIMPLE_TYPE com.sun.org.apache.xerces.internal.xs.XSTypeDefinition/SIMPLE_TYPE)
(def COMPLEX_TYPE com.sun.org.apache.xerces.internal.xs.XSTypeDefinition/COMPLEX_TYPE)
(def ELEMENT_DECLARATION
    com.sun.org.apache.xerces.internal.xs.XSConstants/ELEMENT_DECLARATION)

(defn resource-location[f]
  "gets the locaton of a resource on the classpath"
  (-> (clojure.java.io/resource f) .getFile ))

(defn read-file[f] (with-open [r (-> (clojure.java.io/resource f) .openStream)]
                     (xml/parse r)))

(defn get-schema[r]
  "Reads schema from XSD esource on the classpath"
  (System/setProperty org.w3c.dom.bootstrap.DOMImplementationRegistry/PROPERTY
      "com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl")
  (let [registry (org.w3c.dom.bootstrap.DOMImplementationRegistry/newInstance)
        impl (.getDOMImplementation registry "XS-Loader")
        schemaLoader (.createXSLoader impl nil)]
    (.loadURI schemaLoader (resource-location r))))

(defn components
  ([sc n] (let[c (.getComponents sc n)] (for[i (range (.getLength c))] (.item c i))))
    ([sc] (components sc ELEMENT_DECLARATION)))

(declare read-element)
(declare model-group-elements)
(defmulti type-def (fn[_ td] (class td)))
(defmethod type-def com.sun.org.apache.xerces.internal.xs.XSComplexTypeDefinition
  [m td] (let[model-group (-> td .getParticle .getTerm)] 
     (assoc m :elements (model-group-elements model-group ))))
(defmethod type-def com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition
  [m td] (assoc m :type (.getTypeName td)))
 
(defn- model-group-elements[mgi]
  (let [fParticles (.getParticles mgi)
        n (.getLength fParticles)]
    (vec (for [i (range n)] (let[particleDecl (.item fParticles i)] 
      (read-element (.fValue particleDecl)))))))

(defn read-element[eld]
 (let[ m {:name (.getName eld)}]
  (type-def m (.getTypeDefinition eld))))