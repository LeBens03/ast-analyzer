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
     *   <li><b>name</b>: Class name</li>
     *   <li><b>packageName</b>: Package name</li>
     *   <li><b>nbAttributes</b>: Number of attributes (fields)</li>
     *   <li><b>nbMethods</b>: Number of methods</li>
     *   <li><b>methods</b>: List of methods in the class</li>
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
    }

    /**
     * Represents a Java method with its analysis data.
     * <ul>
     *   <li><b>name</b>: Method name</li>
     *   <li><b>nbParameters</b>: Number of parameters</li>
     *   <li><b>nbLines</b>: Number of lines of code</li>
     *   <li><b>calls</b>: List of methods called by this method</li>
     *   <li><b>graphId</b>: Unique ID for graph representation</li>
     * </ul>
     */
    static class MethodInfo {
        /** Method name */
        String name;
        /** Number of parameters */
        int nbParameters = 0;
        /** Number of lines of code */
        int nbLines = 0;
        /** List of methods called by this method */
        List<MethodInfo> calls = new ArrayList<>();
        /** Unique ID for graph representation */
        int graphId;
    }
}