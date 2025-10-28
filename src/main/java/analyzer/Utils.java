package analyzer;

import java.util.*;

/**
 * Utility container with lightweight data structures used across the analyzer.
 *
 * <p>This class only declares plain data-holder types used to represent
 * source-level artifacts discovered by the parsers (JDT/Spoon) and consumed
 * by the analysis components (call graph builder, coupling analyzer,
 * hierarchical clustering).</p>
 *
 * <p>The nested types are intentionally simple POJOs (no business logic) so
 * they can be serialized, printed or converted to/from other representations
 * easily during analysis and for generating reports.</p>
 *
 * @since 1.0
 */
public class Utils {
    /**
    * Represents a Java class and collected analysis information.
    *
    * <p>Fields are populated by the source analyzers and then read by
    * the analysis/visualization modules. Typical usage:
    * <ol>
    *   <li>parser builds a list of ClassInfo objects</li>
    *   <li>call graph / coupling analyzers walk methods to fill call relations</li>
    *   <li>visualizers generate reports based on these objects</li>
    * </ol>
    *</p>
    */
    static class ClassInfo {
        /** Simple class name (without package). */
        String name;
        /** Fully-qualified package name, may be empty for default package. */
        String packageName;
        /** Number of declared attributes/fields in this class (best-effort). */
        int nbAttributes = 0;
        /** Number of declared methods in this class (best-effort). */
        int nbMethods = 0;
        /** Methods found in the class. Each MethodInfo contains call signatures and other metrics. */
        List<MethodInfo> methods = new ArrayList<>();
        /** Set of other classes this class depends on (by simple or qualified name). */
        Set<String> dependencies = new HashSet<>();
        /** Set of classes that depend on this class (inverse relation). */
        Set<String> dependents = new HashSet<>();
    }

    /**
    * Represents a Java method and lightweight metrics collected for analysis.
    *
    * <p>callSignatures contains method invocation signatures in the form
    * "methodName:paramCount" which are used by the coupling analyzer to map
    * calls to candidate target classes when full type binding is not available.
    *</p>
    */
    static class MethodInfo {
        /** Method simple name. */
        String name;
        /** Number of parameters declared on the method. */
        int nbParameters = 0;
        /** Number of source code lines for the method (best-effort). */
        int nbLines = 0;
        /** Direct callees discovered in the same analysis run (for call graph). */
        List<MethodInfo> calls = new ArrayList<>();
        /**
         * Call signatures used for class-level coupling analysis. Stored as
         * "methodName:paramCount". Using a Set avoids duplicates.
         */
        Set<String> callSignatures = new HashSet<>();
        /** Unique identifier used for graph node generation. */
        int graphId;
        /** Fully qualified owner class name (package + class) when available. */
        String classOwner;
    }
    
    /**
     * Cluster node used by the hierarchical clustering implementation.
     *
     * <p>A Cluster may represent a leaf (one or more ClassInfo objects) or
     * an internal merge node with left/right children and the merge score
     * (mergeCouplingValue).</p>
     */
    static class Cluster {
        /** Classes contained in this cluster subtree. */
        List<ClassInfo> classes;
        /** Left child in the dendrogram (null for leaves). */
        Cluster leftChild;
        /** Right child in the dendrogram (null for leaves). */
        Cluster rightChild;
        /** Coupling value at the time of merging (similarity between children). */
        double mergeCouplingValue;
        
        public Cluster() {
            classes = new ArrayList<>();
            leftChild = null;
            rightChild = null;
        }
        
        public Cluster(List<ClassInfo> classes, Cluster leftChild, Cluster rightChild, double mergeCouplingValue) {
            this.classes = classes;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.mergeCouplingValue = mergeCouplingValue;
        }
    }
    
   /**
    * Simple container representing a dendrogram: a list of clustering levels.
    * Each element in <code>clusters</code> is a list of Cluster nodes representing
    * the state of the partition at a given agglomeration step.
    */
   static class Dendro {
       List<ArrayList<Cluster>> clusters;
       
       public Dendro() {
           clusters = new ArrayList<ArrayList<Cluster>>();
       }
   }
}