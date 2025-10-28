package analyzer;

import java.util.*;

/**
* Utility classes for code analysis.
* <p>
* Contains data structures used by Analyzer to represent classes and methods.
*/
public class Utils {
    /**
    * Represents a Java class with its analysis data.
    * <ul>
    * <li><b>name</b>: Class name</li>
    * <li><b>packageName</b>: Package name</li>
    * <li><b>nbAttributes</b>: Number of attributes (fields)</li>
    * <li><b>nbMethods</b>: Number of methods</li>
    * <li><b>methods</b>: List of methods in the class</li>
    * </ul>
    */
    static class ClassInfo {
        /** Class name */
        String name;
        /** Package name */
        String packageName;
        /** Number of attributes (fields) */
        int nbAttributes = 0;
        /** Number of methods */
        int nbMethods = 0;
        /** List of methods in the class */
        List<MethodInfo> methods = new ArrayList<>();
        /** Classes this class depends on */
        Set<String> dependencies = new HashSet<>();
        /** Classes that depend on this class */
        Set<String> dependents = new HashSet<>();
    }

    /**
    * Represents a Java method with its analysis data.
    * <ul>
    * <li><b>name</b>: Method name</li>
    * <li><b>nbParameters</b>: Number of parameters</li>
    * <li><b>nbLines</b>: Number of lines of code</li>
    * <li><b>calls</b>: List of methods called by this method</li>
    * <li><b>callSignatures</b>: Method call signatures (name:paramCount) for coupling analysis</li>
    * <li><b>graphId</b>: Unique ID for graph representation</li>
    * </ul>
    */
    static class MethodInfo {
        /** Method name */
        String name;
        /** Number of parameters */
        int nbParameters = 0;
        /** Number of lines of code */
        int nbLines = 0;
        /** List of methods called by this method (for call graph) */
        List<MethodInfo> calls = new ArrayList<>();
        /** Method call signatures for coupling analysis: "methodName:paramCount" */
        Set<String> callSignatures = new HashSet<>();
        /** Unique ID for graph representation */
        int graphId;
        /** Owner class of the method */
        String classOwner;
    }
    
    static class Cluster {
    	List<ClassInfo> classes;
    	Cluster leftChild;
    	Cluster rightChild;
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
    
   static class Dendro {
	   List<ArrayList<Cluster>> clusters;
	   
	   public Dendro() {
		   clusters = new ArrayList<ArrayList<Cluster>>();
	   }
   }
}