package analyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import analyzer.Utils.ClassInfo;
import analyzer.Utils.MethodInfo;

/**
 * Computes class-level coupling based on method call signatures and generates
 * visualizations for further analysis.
 *
 * <p>This analyzer inspects MethodInfo.callSignatures for every method and
 * builds a coupling map between pairs of classes. Coupling values are
 * normalized by the total number of inter-class calls discovered.</p>
 *
 * <p>Typical usage:
 * <ol>
 *   <li>Create an instance passing a list of ClassInfo populated by a parser (JDT/Spoon).</li>
 *   <li>Use displayCouplings() / displayCouplingMatrix() for console output.</li>
 *   <li>Call generateHtmlGraph(filename) to get an interactive Vis.js visualization.</li>
 *   <li>Access normalized coupling values programmatically via getNormalizedCouplings().</li>
 * </ol>
 * </p>
 */
public class ClassCouplingAnalyzer {
    
    private List<ClassInfo> classes;
    private Map<String, Integer> couplingMap; // Key: "ClassA-ClassB", Value: call count
    private int totalCouplings; // Total number of method calls between all classes
    private Map<String, Double> normalizedCouplings; // Normalized coupling values
    private Map<String, ClassInfo> classMap; // Map class name to ClassInfo
    private Map<String, String> methodToClassMap; // Map "MethodName:ParamCount" to class name
    
    public ClassCouplingAnalyzer(List<ClassInfo> classes) {
        this.classes = classes;
        this.couplingMap = new HashMap<>();
        this.normalizedCouplings = new HashMap<>();
        this.totalCouplings = 0;
        this.classMap = new HashMap<>();
        this.methodToClassMap = new HashMap<>();
        
        // Build class map and method to class map
        for (ClassInfo cls : classes) {
            classMap.put(cls.name, cls);
            for (MethodInfo method : cls.methods) {
                String methodKey = cls.name + "." + method.name + ":" + method.nbParameters;
                methodToClassMap.put(methodKey, cls.name);
            }
        }
        
        analyzeCouplings();
    }
    
    /**
     * Analyzes all couplings between classes by examining method call signatures.
     * Uses callSignatures instead of object references to reliably identify called methods.
     */
    private void analyzeCouplings() {
        // Build a map of method signatures to their owning class
        Map<String, Set<String>> signatureToClasses = new HashMap<>();
        for (ClassInfo cls : classes) {
            for (MethodInfo method : cls.methods) {
                String signature = method.name + ":" + method.nbParameters;
                signatureToClasses.computeIfAbsent(signature, k -> new HashSet<>()).add(cls.name);
            }
        }
        
        // For each class and its methods, analyze what classes they call
        for (ClassInfo sourceClass : classes) {
            for (MethodInfo sourceMethod : sourceClass.methods) {
                // For each method call signature in this method
                for (String callSignature : sourceMethod.callSignatures) {
                    Set<String> targetClasses = signatureToClasses.get(callSignature);
                    
                    if (targetClasses != null) {
                        for (String targetClass : targetClasses) {
                            // Only count calls between different classes
                            if (!sourceClass.name.equals(targetClass)) {
                                String key = getCouplingKey(sourceClass.name, targetClass);
                                couplingMap.put(key, couplingMap.getOrDefault(key, 0) + 1);
                                totalCouplings++;
                            }
                        }
                    }
                }
            }
        }
        
        // Normalize couplings
        if (totalCouplings > 0) {
            for (String key : couplingMap.keySet()) {
                double normalized = (double) couplingMap.get(key) / totalCouplings;
                normalizedCouplings.put(key, normalized);
            }
        }
    }
    
    /**
     * Creates a canonical key for coupling pairs (A-B or B-A becomes A-B).
     */
    private String getCouplingKey(String classA, String classB) {
        if (classA.compareTo(classB) <= 0) {
            return classA + "-" + classB;
        } else {
            return classB + "-" + classA;
        }
    }
    
    /**
     * Displays the coupling between all class pairs in console.
     */
    public void displayCouplings() {
        System.out.println("\n=== Couplage entre les classes ===\n");
        if (totalCouplings == 0) {
            System.out.println("Aucun couplage détecté.");
            System.out.println("Total d'appels tracés: " + totalCouplings);
            System.out.println("Nombre de classes: " + classes.size());
            
            // Debug info
            System.out.println("\nInformations de debug:");
            for (ClassInfo cls : classes) {
                System.out.println("  Classe: " + cls.name + " (" + cls.methods.size() + " méthodes)");
                int callCount = 0;
                for (MethodInfo m : cls.methods) {
                    callCount += m.calls.size();
                    if (m.calls.size() > 0) {
                        System.out.println("    Méthode " + m.name + " appelle " + m.calls.size() + " méthode(s)");
                    }
                }
                System.out.println("    Total appels: " + callCount);
            }
            return;
        }
        
        normalizedCouplings.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .forEach(entry -> {
                String[] parts = entry.getKey().split("-");
                System.out.printf("%s -> %s: %.4f (%d appels)\n", 
                    parts[0], parts[1], entry.getValue(), couplingMap.get(entry.getKey()));
            });
        System.out.println("\nNombre total d'appels inter-classe: " + totalCouplings);
    }
    
    /**
     * Generates and displays a coupling matrix for all classes.
     */
    public void displayCouplingMatrix() {
        List<String> uniqueClasses = classes.stream()
            .map(c -> c.name)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        
        System.out.println("\n=== Matrice de Couplage ===\n");
        
        // Header
        System.out.print("Classe");
        for (String cls : uniqueClasses) {
            System.out.printf("%15s", cls);
        }
        System.out.println();
        
        // Separator
        System.out.print("-".repeat(10));
        for (int i = 0; i < uniqueClasses.size(); i++) {
            System.out.print("-".repeat(15));
        }
        System.out.println();
        
        // Matrix rows
        for (String classA : uniqueClasses) {
            System.out.printf("%-10s", classA);
            for (String classB : uniqueClasses) {
                if (classA.equals(classB)) {
                    System.out.printf("%15s", "-");
                } else {
                    String key = getCouplingKey(classA, classB);
                    Double coupling = normalizedCouplings.getOrDefault(key, 0.0);
                    System.out.printf("%15.4f", coupling);
                }
            }
            System.out.println();
        }
    }
    
    /**
     * Generates an HTML visualization of the coupling graph.
     */
    public void generateHtmlGraph(String filename) throws IOException {
        List<String> uniqueClasses = classes.stream()
            .map(c -> c.name)
            .distinct()
            .collect(Collectors.toList());
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='fr'>\n");
        html.append("<head>\n");
        html.append("  <meta charset='UTF-8'>\n");
        html.append("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("  <title>Graphe de Couplage des Classes</title>\n");
        html.append("  <script src='https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.js'></script>\n");
        html.append("  <link href='https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.css' rel='stylesheet' type='text/css' />\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 0; padding: 10px; background-color: #f5f5f5; }\n");
        html.append("    h1 { color: #333; }\n");
        html.append("    #network { width: 100%; height: 600px; border: 1px solid #ccc; background-color: white; }\n");
        html.append("    #stats { margin-top: 20px; background-color: white; padding: 15px; border-radius: 5px; }\n");
        html.append("    .stat-item { margin: 10px 0; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <h1>Graphe de Couplage des Classes</h1>\n");
        html.append("  <div id='network'></div>\n");
        html.append("  <div id='stats'>\n");
        html.append("    <div class='stat-item'><strong>Total des appels inter-classe:</strong> " + totalCouplings + "</div>\n");
        html.append("    <div class='stat-item'><strong>Nombre de classes:</strong> " + uniqueClasses.size() + "</div>\n");
        html.append("  </div>\n");
        html.append("  <script type='text/javascript'>\n");
        
        // Create nodes
        html.append("    var nodes = new vis.DataSet([\n");
        for (int i = 0; i < uniqueClasses.size(); i++) {
            html.append("      { id: ").append(i).append(", label: '").append(uniqueClasses.get(i)).append("', title: '").append(uniqueClasses.get(i)).append("' }");
            if (i < uniqueClasses.size() - 1) html.append(",");
            html.append("\n");
        }
        html.append("    ]);\n\n");
        
        // Create edges with coupling strength
        html.append("    var edges = new vis.DataSet([\n");
        List<String> edgeLines = new ArrayList<>();
        for (int i = 0; i < uniqueClasses.size(); i++) {
            for (int j = i + 1; j < uniqueClasses.size(); j++) {
                String classA = uniqueClasses.get(i);
                String classB = uniqueClasses.get(j);
                String key = getCouplingKey(classA, classB);
                Double coupling = normalizedCouplings.getOrDefault(key, 0.0);
                
                if (coupling > 0) {
                    int calls = couplingMap.get(key);
                    double width = 1 + (coupling * 10);
                    String color = getColorForCoupling(coupling);
                    String edgeLine = String.format(
                        "      { from: %d, to: %d, value: %.4f, title: '%s -> %s: %.4f (%d appels)', width: %.2f, color: '%s' }",
                        i, j, coupling, classA, classB, coupling, calls, width, color
                    );
                    edgeLines.add(edgeLine);
                }
            }
        }
        
        for (int k = 0; k < edgeLines.size(); k++) {
            html.append(edgeLines.get(k));
            if (k < edgeLines.size() - 1) html.append(",");
            html.append("\n");
        }
        html.append("    ]);\n\n");
        
        // Vis.js configuration
        html.append("    var container = document.getElementById('network');\n");
        html.append("    var data = { nodes: nodes, edges: edges };\n");
        html.append("    var options = {\n");
        html.append("      physics: { enabled: true, stabilization: { iterations: 200 } },\n");
        html.append("      interaction: { navigationButtons: true, keyboard: true },\n");
        html.append("      nodes: { font: { size: 16 }, color: { background: '#97C2FC', border: '#2B7CE5', highlight: { background: '#FF6B6B', border: '#FF0000' } } }\n");
        html.append("    };\n");
        html.append("    var network = new vis.Network(container, data, options);\n");
        html.append("  </script>\n");
        html.append("</body>\n");
        html.append("</html>\n");
        
        try (FileWriter fw = new FileWriter(filename)) {
            fw.write(html.toString());
            System.out.println("\nGraphe de couplage généré: " + filename);
        }
    }
    
    /**
     * Returns a color based on coupling strength.
     */
    private String getColorForCoupling(double coupling) {
        if (coupling > 0.1) return "#FF0000";
        if (coupling > 0.05) return "#FF6B6B";
        if (coupling > 0.02) return "#FFA500";
        if (coupling > 0.01) return "#FFD700";
        return "#87CEEB";
    }
    
    /**
     * Public getter for normalized couplings map (read-only view).
     * Keys are canonicalized pairs "A-B" and values are normalized in [0..1].
     *
     * @return unmodifiable map of normalized coupling scores between class pairs
     */
    public Map<String, Double> getNormalizedCouplings() {
        return Collections.unmodifiableMap(normalizedCouplings);
    }

    /**
     * Returns normalized coupling between two named classes (order-insensitive).
     *
     * @param classA simple name of first class
     * @param classB simple name of second class
     * @return normalized coupling value (0.0 if not present)
     */
    public double getCoupling(String classA, String classB) {
        String key = getCouplingKey(classA, classB);
        return normalizedCouplings.getOrDefault(key, 0.0);
    }
    
    /**
     * Returns raw (integer) number of inter-class calls observed between two classes.
     *
     * @param classA simple name of first class
     * @param classB simple name of second class
     * @return raw call count (0 if not present)
     */
    public int getRawCoupling(String classA, String classB) {
        String key = getCouplingKey(classA, classB);
        return couplingMap.getOrDefault(key, 0);
    }
    
    /**
     * Returns the total number of inter-class calls discovered while building the coupling map.
     */
    public int getTotalCouplings() {
        return totalCouplings;
    }
}