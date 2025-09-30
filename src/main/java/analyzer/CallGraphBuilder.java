package analyzer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import analyzer.Utils.ClassInfo;
import analyzer.Utils.MethodInfo;

public class CallGraphBuilder {
    
    /**
     * Affiche le graphe d'appel sous forme textuelle
     */
    public static void displayCallGraph(List<ClassInfo> classes) {
        System.out.println("\n==== GRAPHE D'APPEL ====\n");
        
        for (ClassInfo classInfo : classes) {
            System.out.println("Classe: " + classInfo.packageName + "." + classInfo.name);
            
            for (MethodInfo method : classInfo.methods) {
                if (!method.calls.isEmpty()) {
                    System.out.println("  " + method.name + "() appelle:");
                    for (MethodInfo calledMethod : method.calls) {
                        System.out.println("    -> " + calledMethod.name + "()");
                    }
                } else {
                    System.out.println("  " + method.name + "() [pas d'appels]");
                }
            }
            System.out.println();
        }
    }
    
    /**
     * Génère un fichier DOT (GraphViz) du graphe d'appel
     */
    public static void generateDotGraph(List<ClassInfo> classes, String outputPath) {
        try (PrintWriter writer = new PrintWriter(outputPath)) {
            writer.println("digraph CallGraph {");
            writer.println("  rankdir=LR;");
            writer.println("  node [shape=box, style=rounded];");
            writer.println();
            
            for (ClassInfo classInfo : classes) {
                String className = classInfo.name;
                
                // Créer un sous-graphe pour chaque classe
                writer.println("  subgraph cluster_" + className + " {");
                writer.println("    label=\"" + className + "\";");
                writer.println("    style=filled;");
                writer.println("    color=lightgrey;");
                writer.println();
                
                // Déclarer tous les nœuds (méthodes) de cette classe
                for (MethodInfo method : classInfo.methods) {
                    String nodeId = className + "_" + method.name;
                    writer.println("    \"" + nodeId + "\" [label=\"" + method.name + "()\"];");
                }
                
                writer.println("  }");
                writer.println();
            }
            
            // Ajouter les arêtes (appels de méthodes)
            for (ClassInfo classInfo : classes) {
                String className = classInfo.name;
                
                for (MethodInfo method : classInfo.methods) {
                    String fromNode = className + "_" + method.name;
                    
                    for (MethodInfo calledMethod : method.calls) {
                        String toNode = className + "_" + calledMethod.name;
                        writer.println("  \"" + fromNode + "\" -> \"" + toNode + "\";");
                    }
                }
            }
            
            writer.println("}");
            System.out.println("\nGraphe DOT généré: " + outputPath);
            System.out.println("Utilisez: dot -Tpng " + outputPath + " -o callgraph.png");
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la génération du fichier DOT: " + e.getMessage());
        }
    }
    
    /**
     * Génère un graphe au format HTML interactif (avec vis.js via CDN)
     */
    public static void generateHtmlGraph(List<ClassInfo> classes, String outputPath) {
        try (PrintWriter writer = new PrintWriter(outputPath)) {
            writer.println("<!DOCTYPE html>");
            writer.println("<html>");
            writer.println("<head>");
            writer.println("  <title>Call Graph</title>");
            writer.println("  <script src=\"https://cdnjs.cloudflare.com/ajax/libs/vis-network/9.1.2/dist/vis-network.min.js\"></script>");
            writer.println("  <style>");
            writer.println("    #mynetwork { width: 100%; height: 800px; border: 1px solid lightgray; }");
            writer.println("    body { font-family: Arial, sans-serif; margin: 20px; }");
            writer.println("  </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("  <h1>Graphe d'Appel</h1>");
            writer.println("  <div id=\"mynetwork\"></div>");
            writer.println("  <script>");
            
            // Créer les nœuds
            writer.println("    var nodes = new vis.DataSet([");
            int nodeId = 0;
            for (ClassInfo classInfo : classes) {
                for (MethodInfo method : classInfo.methods) {
                    method.graphId = nodeId++;
                    writer.println("      { id: " + method.graphId + ", " +
                                 "label: '" + classInfo.name + "." + method.name + "()', " +
                                 "group: '" + classInfo.name + "' },");
                }
            }
            writer.println("    ]);");
            
            // Créer les arêtes
            writer.println("    var edges = new vis.DataSet([");
            for (ClassInfo classInfo : classes) {
                for (MethodInfo method : classInfo.methods) {
                    for (MethodInfo calledMethod : method.calls) {
                        writer.println("      { from: " + method.graphId + ", to: " + calledMethod.graphId + ", arrows: 'to' },");
                    }
                }
            }
            writer.println("    ]);");
            
            writer.println("    var container = document.getElementById('mynetwork');");
            writer.println("    var data = { nodes: nodes, edges: edges };");
            writer.println("    var options = {");
            writer.println("      layout: { hierarchical: { direction: 'LR', sortMethod: 'directed' } },");
            writer.println("      physics: { enabled: false }");
            writer.println("    };");
            writer.println("    var network = new vis.Network(container, data, options);");
            writer.println("  </script>");
            writer.println("</body>");
            writer.println("</html>");
            
            System.out.println("Graphe HTML généré: " + outputPath);
            System.out.println("Ouvrez le fichier dans un navigateur pour voir le graphe interactif");
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la génération du fichier HTML: " + e.getMessage());
        }
    }
}