;; @author Antonio Garote
;; @email antoniogarrote@gmail.com
;; @date 04.05.2010

(ns plaza.rdf.sparql
  (:use (plaza.rdf core)
        (plaza utils)))

;; main SPARQL framework
;; This value must be set when starting the application
(def *sparql-framework* nil)

;; sets the root bindings, useful when reading configuration.
(defn alter-root-sparql-framework
  "Alters the root binding for the default model. This function
   should only be used when setting up the application, with-model
   macro should be used by default"
  ([new-framework]
     (alter-var-root #'*sparql-framework* (fn [_] new-framework))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;                  SPARQL                     ;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol SparqlFramework
  "An RDF list of triples where some values has been replaced by variables. It is also possible to
   define an optional part in the pattern"
  (parse-sparql-to-query [framework sparql] "Parses a SPARQL query and builds a query")
  (parse-sparql-to-pattern [framework sparql] "Parses a SPARQL query and builds a pattern")
  (build-filter [framework filter] "Transforms a filter representation into a filter Java object")
  (build-query [framework query] "Transforms a query reprsentation into a query Java object")
  (is-var-expr [framework expr] "Checks wether an object is a valid var expression for this framework")
  (var-to-keyword [framework var-expr] "Transforms an object var into a keyword"))


;; common variable names
(def ?s :?s)
(def ?p :?p)
(def ?o :?o)
(def ?a :?a)
(def ?b :?b)
(def ?c :?c)
(def ?d :?d)
(def ?e :?e)
(def ?f :?f)
(def ?g :?g)
(def ?h :?h)
(def ?i :?i)
(def ?j :?j)
(def ?k :?k)
(def ?l :?l)
(def ?m :?m)
(def ?n :?n)
(def ?q :?q)
(def ?r :?r)
(def ?t :?t)
(def ?u :?u)
(def ?v :?v)
(def ?w :?w)
(def ?x :?x)
(def ?y :?y)
(def ?z :?z)

(defn keyword-to-variable
  "Transforms a symbol ':?t' into a variable name 't'"
  ([kw]
     (if (.startsWith (keyword-to-string kw) "?")
       (aget (.split (keyword-to-string kw) "\\?") 1)
       (keyword-to-string kw))))

(defn sparql-to-pattern
  "Wraps the SPARQL framework type function that transforms a SPARQL string into a pattern"
  ([sparql] (parse-sparql-to-pattern *sparql-framework* sparql)))

(defn sparql-to-query
  "Wraps the SPARQL framework type function that transforms a SPARQL string into a query"
  ([sparql] (parse-sparql-to-query *sparql-framework* sparql)))


(defn make-pattern
  "Builds a new pattern representation"
  ([triples]
     (with-meta
       (flatten-1
        (map
         (fn [triple]
           (let [tmp (rdf-triple triple)
                 optional (:optional (meta triple))]
             (if (coll? (first tmp))
               (map #(with-meta %1 {:optional optional}) tmp)
               (with-meta tmp {:optional optional}))))
         triples))
       {:pattern true})))

;; (defn make-query
;;   "Builds a new query representation"
;;   ([] {})
;;   ([kind vars pattern & args]
;;      (let [opts (apply hash-map args)]
;;        {:kind kind :vars vars :pattern (with-model (build-model)
;;                                          (make-pattern pattern))})))

;; root sparql query

(defmacro defquery
  "Sets up a default query for a set of form that describe the query incrementally"
  ([& rest]
     `(-> {} ~@rest)))

(defn query-set-type
  "Sets the type of a query"
  ([kind]
     (fn [query] (query-set-type query kind)))
  ([query kind]
     (assoc query :kind kind)))

(defn query-set-vars
  "Set a collection of vars for the query"
  ([query vars]
     (assoc query :vars vars))
  ([vars]
     (fn [query] (query-set-vars query vars))))

(defn query-set-filters
  "Set a collection of filters for the query"
  ([query filters]
     (assoc query :filters filters))
  ([filters]
     (fn [query] (query-set-filters query filters))))

(defn query-set-limit
  "Sets a limit in the number of results"
  ([query limit]
     (assoc query :limit limit))
  ([limit]
     (fn [query] (query-set-limit limit))))

(defn chain-limit
  "Used to conditionally insert limit in the threaded definition of a query"
  ([h limit]
     (if (nil? limit) h (assoc h :limit (if (string? (first limit)) (Integer/parseInt (first limit)) (first limit))))))

(defn query-unset-limit
  "Removes the limit constrain in the number of results"
  ([query]
     (dissoc query :limit))
  ([]
     (fn [query] (query-unset-limit))))

(defn query-set-distinct
  "Sets a distinct contrain in the results"
  ([query]
     (assoc query :distinct true))
  ([]
     (fn [query] (query-set-distinct))))

(defn query-unset-distinct
  "Removes the distinct constrain in the results"
  ([query]
     (dissoc query :distinct))
  ([]
     (fn [query] (query-unset-distinct))))

(defn query-set-reduced
  "Sets a reduced constrain in the results"
  ([query]
     (assoc query :reduced true))
  ([]
     (fn [query] (query-set-reduced))))

(defn query-unset-reduced
  "Removes the reduced constrain in the number results"
  ([query]
     (dissoc query :reduced))
  ([]
     (fn [query] (query-unset-reduced))))

(defn query-set-offset
  "Sets an offset in the results"
  ([query offset]
     (assoc query :offset offset))
  ([offset]
     (fn [query] (query-set-offset offset))))

(defn chain-offset
  "Used to conditionally insert offset in the threaded definition of a query"
  ([h offset]
     (if (nil? offset) h (assoc h :offset (if (string? (first offset)) (Integer/parseInt (first offset)) (first offset))))))

(defn query-unset-offset
  "Removes the offset constraint in the results"
  ([query]
     (dissoc query :offset))
  ([]
     (fn [query] (query-unset-offset))))

(defn query-remove-var
  "Removes a var from the collection of vars in the query"
  ([query var]
     (assoc query :vars (remove #(= %1 var) (:vars query))))
  ([vars]
     (fn [query] (query-remove-var query vars))))

(defn query-add-var
  "Adds a var to the collection of vars in the query"
  ([query var]
     (assoc query :vars (conj (:vars query) var)))
  ([vars]
     (fn [query] (query-add-var query vars))))

(defn query-set-pattern
  "Sets the pattern for the query"
  ([query pattern]
     (assoc query :pattern pattern))
  ([pattern]
     (fn [query] (query-set-pattern query pattern))))

(defn query-set-order-by
  "Sets the pattern for the query"
  ([query order-by]
     (assoc query :order-by order-by))
  ([order-by]
     (fn [query] (query-set-order-by query order-by))))

;; Parsing a SPARQL string query into a query pattern representation

(defn- var-to-keyword-fn
  ([v]
     (if (keyword? v)
       v
       (var-to-keyword *sparql-framework* v))))

(defn- collect-var
  "Auxiliary function for pattern-collect-vars"
  ([rs atom]
     (if (is-var-expr *sparql-framework* atom)
       (let [katom (var-to-keyword-fn atom)]
         (if (some #(= katom %1) rs)
           rs
           (conj rs katom)))
       rs)))

(defn pattern-collect-vars
  "Returns an array with all the vars in a pattern"
  ([pattern]
     (reduce (fn [acum [s p o]]
               (let [acump   (collect-var acum s)
                     acumpp  (collect-var acump p)
                     acumppp (collect-var acumpp o)]
                 acumppp))
             [] pattern)))


(defn make-filter
  "Makes a new filter expression"
  ([name & args]
     {:expression (keyword name)
      :args args
      :kind (if  (= (count args) 1) :one-part :two-parts)}))

(defn f
  "Shortcut for make-filter"
  ([name & args]
     (apply make-filter (cons name args))))

(defn limit
  "Defines limit as a filter"
  ([max]
     (:expression :limit :args max)))

(defn offset
  "Defines offset as a filter"
  ([val]
     (:expression :offset :args val)))

;; Querying a model

(defn model-query
  "Queries a model and returns a map of bindings"
  ([model user-query]
     (query model user-query)))

(declare pattern-bind)
(defn model-query-triples
  "Queries a model and returns a list of triple sets with results binding variables in que query pattern"
  ([model query]
     (query-triples model query)))


;; Triples <-> Pattern transformations


(defn pattern-bind
  "Binds variables in a pattern with some values"
  ([pattern binding-map]
     (vec
      (map (fn [t]
             (let [[s p o] t
                   sp (if (get binding-map s)
                        (triple-subject (get binding-map s))
                        s)
                   pp (if (get binding-map p)
                        (triple-predicate (get binding-map p))
                        p)
                   op (if (get binding-map o)
                        (triple-object (get binding-map o))
                        o)]
               (if (:optional (meta t))
                 (with-meta [sp pp op] {:optional true})
                 [sp pp op])))
           pattern))))

(defn pattern-reject-unbound
  "Binds variables in a pattern rejecting the pattern triples that has not been bound"
  ([pattern]
     (filter (fn [[s p o]]
               (and (not (keyword? s))
                    (not (keyword? p))
                    (not (keyword? o)))) pattern)))


(defn pattern-apply
  "Applies a pattern to a set of triples"
  ([triples pattern-or-vector & filters-pre]
     (let [pattern-pre (if (:pattern (meta pattern-or-vector)) pattern-or-vector (make-pattern pattern-or-vector))
           vars-pre (pattern-collect-vars pattern-pre)
           vars (if-not (empty? vars-pre) vars-pre [:p])
           [pattern filterspp] (if-not (empty? vars-pre)
                                 [pattern-pre filters-pre]
                                 (let [s (nth (first pattern-pre) 0)
                                       p (nth (first pattern-pre) 1)
                                       o (nth (first pattern-pre) 2)]
                                   [(cons [s ?p o] (rest pattern-pre))
                                    (cons (f :sameTerm  ?p p) filters-pre)]))
           [limit filtersp] (if (some #(= :limit (:expression %1)) filterspp)
                              [(:args (first (filter #(= :limit (:expression %1)) filterspp))) (filter #(not (= :limit (:expression %1))) filterspp)]
                              [nil filterspp])
           [offset filters] (if (some #(= :offset (:expression %1)) filtersp)
                              [(:args (first (filter #(= :offset (:expression %1)) filtersp))) (filter #(not (= :offset (:expression %1))) filtersp)]
                              [nil filtersp])
           query (if (empty? filters)
                   (defquery
                     (query-set-pattern pattern)
                     (query-set-type :select)
                     (query-set-vars vars)
                     (query-set-distinct)
                     (chain-limit limit)
                     (chain-offset offset))
                   (defquery
                     (query-set-pattern pattern)
                     (query-set-type :select)
                     (query-set-vars vars)
                     (query-set-distinct)
                     (chain-limit limit)
                     (chain-offset offset)
                     (query-set-filters filters)))]
       (distinct (model-query-triples (defmodel (model-add-triples triples))
                                      query)))))

(defn query-to-string
  "Returns the string representation of a query"
  ([query]
     (str (build-query *sparql-framework* query))))

(defn model-pattern-apply
  "Applies a pattern to a Model returning triples"
  ([model pattern-or-vector & filters-pre]
     (let [pattern-pre (if (:pattern (meta pattern-or-vector)) pattern-or-vector (make-pattern pattern-or-vector))
           vars-pre (pattern-collect-vars pattern-pre)
           vars (if-not (empty? vars-pre) vars-pre [:p])
           [pattern filterspp] (if-not (empty? vars-pre)
                                 [pattern-pre filters-pre]
                                 (let [s (nth (first pattern-pre) 0)
                                       p (nth (first pattern-pre) 1)
                                       o (nth (first pattern-pre) 2)]
                                   [(cons [s ?p o] (rest pattern-pre))
                                    (cons (f :sameTerm  ?p p) filters-pre)]))
           [limit filtersp] (if (some #(= :limit (:expression %1)) filterspp)
                              [(:args (first (filter #(= :limit (:expression %1)) filterspp))) (filter #(not (= :limit (:expression %1))) filterspp)]
                              [nil filterspp])
           [offset filters] (if (some #(= :offset (:expression %1)) filtersp)
                              [(:args (first (filter #(= :offset (:expression %1)) filtersp))) (filter #(not (= :offset (:expression %1))) filtersp)]
                              [nil filtersp])
           query (if (empty? filters)
                   (defquery
                     (query-set-pattern pattern)
                     (query-set-type :select)
                     (query-set-vars vars)
                     (query-set-distinct)
                     (chain-limit limit)
                     (chain-offset offset))
                   (defquery
                     (query-set-pattern pattern)
                     (query-set-type :select)
                     (query-set-vars vars)
                     (query-set-distinct)
                     (chain-limit limit)
                     (chain-offset offset)
                     (query-set-filters filters)))]
       (distinct (model-query-triples model
                                      query)))))

(defn- subject-vars
  ([pattern]
     (reduce (fn [ac [s p o]] (if (is-var-expr *sparql-framework* s) (conj ac s) ac)) [] pattern)))

(defn- predicate-vars
  ([pattern]
     (reduce (fn [ac [s p o]] (if (is-var-expr *sparql-framework* p) (conj ac p) ac)) [] pattern)))

(defn- object-vars
  ([pattern]
     (reduce (fn [ac [s p o]] (if (is-var-expr *sparql-framework* o) (conj ac o) ac)) [] pattern)))

(defn not-empty-column-vars
  ([pattern]
     (let [sv (subject-vars pattern)]
       (if (> 1 (count sv))
         (let [pv (predicate-vars pattern)]
           (if (> 1 (count pv))
             (distinct (object-vars pattern))
             (distinct pv)))
         (distinct sv)))))


(defn make-cw-filters
  "Creates a list of filters to ensure all variables are considered distinct in a pattern"
  ([pattern]
     (let [vars (not-empty-column-vars pattern)]
       (if (> (count vars) 1)
         (loop [acum []
                vars vars]
           (if (> (count vars) 1)
             (recur (conj acum (f :!= (first vars) (second vars)))
                    (rest vars))
             acum))
         '()))))

(defn model-pattern-apply-cws
  "Applies a pattern to a Model using close world semantics (all variables are considered to be distinct"
  ([model pattern-or-vector & filters-pre]
     (let [pattern-pre (if (:pattern (meta pattern-or-vector)) pattern-or-vector (make-pattern pattern-or-vector))
           vars-pre (pattern-collect-vars pattern-pre)
           vars (if-not (empty? vars-pre) vars-pre [:p])
           [pattern filterspp] (if-not (empty? vars-pre)
                                 [pattern-pre filters-pre]
                                 (let [s (nth (first pattern-pre) 0)
                                       p (nth (first pattern-pre) 1)
                                       o (nth (first pattern-pre) 2)]
                                   [(cons [s ?p o] (rest pattern-pre))
                                    (cons (f :sameTerm  ?p p) filters-pre)]))
           [limit filtersp] (if (some #(= :limit (:expression %1)) filterspp)
                              [(:args (first (filter #(= :limit (:expression %1)) filterspp))) (filter #(not (= :limit (:expression %1))) filterspp)]
                              [nil filterspp])
           [offset filters] (if (some #(= :offset (:expression %1)) filtersp)
                              [(:args (first (filter #(= :offset (:expression %1)) filtersp))) (filter #(not (= :offset (:expression %1))) filtersp)]
                              [nil filtersp])
           filters (concat filters-pre (make-cw-filters pattern))
           query (if (empty? filters)
                   (defquery
                     (query-set-pattern pattern)
                     (query-set-type :select)
                     (query-set-vars vars)
                     (query-set-distinct)
                     (chain-limit limit)
                     (chain-offset offset))
                   (defquery
                     (query-set-pattern pattern)
                     (query-set-type :select)
                     (query-set-vars vars)
                     (query-set-distinct)
                     (chain-limit limit)
                     (chain-offset offset)
                     (query-set-filters filters)))]
       (distinct (model-query-triples model
                                      query)))))
