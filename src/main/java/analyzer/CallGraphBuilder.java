package analyzer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import analyzer.Utils.ClassInfo;
import analyzer.Utils.MethodInfo;

/**
 * Generates beautiful, interactive HTML graphs using Vis.js library.
 */
public class CallGraphBuilder {
    
    private static final Map<String, String> CLASS_COLORS = new HashMap<>();
    private static int colorIndex = 0;
    private static final String[] COLORS = {
        "#3498db", "#e74c3c", "#2ecc71", "#f39c12", "#9b59b6",
        "#1abc9c", "#34495e", "#e67e22", "#95a5a6", "#16a085"
    };

    /**
     * Displays the call graph in the console in a readable format.
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
     * Generates an enhanced interactive HTML call graph visualization.
     * Features:
     * - Grouped nodes by class with color coding
     * - Physics simulation for optimal layout
     * - Interactive hover effects
     * - Directional arrows
     * - Search and filter capabilities
     * - Statistics panel
     */
    public static void generateHtmlGraph(List<ClassInfo> classes, String outputPath) {
        try (PrintWriter writer = new PrintWriter(outputPath)) {
            // Assign colors to classes
            for (ClassInfo cls : classes) {
                if (!CLASS_COLORS.containsKey(cls.name)) {
                    CLASS_COLORS.put(cls.name, COLORS[colorIndex % COLORS.length]);
                    colorIndex++;
                }
            }

            writer.println("<!DOCTYPE html>");
            writer.println("<html lang='fr'>");
            writer.println("<head>");
            writer.println("  <meta charset='UTF-8'>");
            writer.println("  <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
            writer.println("  <title>Graphe d'Appels Interactif</title>");
            writer.println("  <script src='https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.js'></script>");
            writer.println("  <link href='https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.css' rel='stylesheet' type='text/css' />");
            writer.println("  <style>");
            writer.println("    * { margin: 0; padding: 0; box-sizing: border-box; }");
            writer.println("    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; }");
            writer.println("    .container { display: flex; height: 100vh; }");
            writer.println("    .sidebar { width: 300px; background: #2c3e50; color: white; padding: 20px; overflow-y: auto; box-shadow: 2px 0 10px rgba(0,0,0,0.3); }");
            writer.println("    .sidebar h2 { margin-bottom: 20px; font-size: 18px; border-bottom: 2px solid #3498db; padding-bottom: 10px; }");
            writer.println("    .sidebar h3 { font-size: 14px; margin-top: 15px; margin-bottom: 10px; color: #3498db; }");
            writer.println("    .class-legend { background: rgba(255,255,255,0.1); padding: 10px; margin: 5px 0; border-radius: 5px; border-left: 4px solid; font-size: 12px; }");
            writer.println("    .stats { background: rgba(255,255,255,0.1); padding: 15px; border-radius: 8px; margin-top: 20px; font-size: 12px; }");
            writer.println("    .stat-item { margin: 8px 0; display: flex; justify-content: space-between; }");
            writer.println("    .stat-label { color: #bdc3c7; }");
            writer.println("    .stat-value { color: #3498db; font-weight: bold; }");
            writer.println("    .controls { background: rgba(255,255,255,0.1); padding: 15px; border-radius: 8px; margin-bottom: 20px; }");
            writer.println("    .controls input, .controls button { width: 100%; padding: 8px; margin: 5px 0; border: none; border-radius: 4px; }");
            writer.println("    .controls input { background: #34495e; color: white; }");
            writer.println("    .controls input::placeholder { color: #95a5a6; }");
            writer.println("    .controls button { background: #3498db; color: white; font-weight: bold; cursor: pointer; transition: 0.3s; }");
            writer.println("    .controls button:hover { background: #2980b9; }");
            writer.println("    #network { flex: 1; background: white; position: relative; box-shadow: inset 0 0 20px rgba(0,0,0,0.1); }");
            writer.println("    .info-box { position: absolute; bottom: 20px; right: 20px; background: white; padding: 15px; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.2); font-size: 12px; max-width: 250px; z-index: 10; }");
            writer.println("    .info-box h4 { color: #2c3e50; margin-bottom: 8px; }");
            writer.println("    .info-box p { color: #7f8c8d; line-height: 1.4; }");
            writer.println("  </style>");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("  <div class='container'>");
            writer.println("    <div class='sidebar'>");
            writer.println("      <h2>Graphe d'Appels</h2>");
            writer.println("      <div class='controls'>");
            writer.println("        <input type='text' id='searchInput' placeholder='Rechercher une méthode...'>");
            writer.println("        <button onclick='resetGraph()'>Réinitialiser</button>");
            writer.println("      </div>");
            writer.println("      <h3>Classes</h3>");
            
            // Class legend
            for (ClassInfo cls : classes) {
                String color = CLASS_COLORS.get(cls.name);
                writer.println("      <div class='class-legend' style='border-color: " + color + ";'>");
                writer.println("        <strong>" + cls.name + "</strong><br>");
                writer.println("        <span style='font-size: 11px; color: #bdc3c7;'>" + cls.methods.size() + " méthodes</span>");
                writer.println("      </div>");
            }
            
            // Statistics
            int totalMethods = classes.stream().mapToInt(c -> c.methods.size()).sum();
            int totalCalls = classes.stream()
                    .flatMap(c -> c.methods.stream())
                    .mapToInt(m -> m.calls.size())
                    .sum();
            
            writer.println("      <div class='stats'>");
            writer.println("        <h3 style='margin-top: 0; color: #3498db;'>Statistiques</h3>");
            writer.println("        <div class='stat-item'>");
            writer.println("          <span class='stat-label'>Classes:</span>");
            writer.println("          <span class='stat-value'>" + classes.size() + "</span>");
            writer.println("        </div>");
            writer.println("        <div class='stat-item'>");
            writer.println("          <span class='stat-label'>Méthodes:</span>");
            writer.println("          <span class='stat-value'>" + totalMethods + "</span>");
            writer.println("        </div>");
            writer.println("        <div class='stat-item'>");
            writer.println("          <span class='stat-label'>Appels:</span>");
            writer.println("          <span class='stat-value'>" + totalCalls + "</span>");
            writer.println("        </div>");
            writer.println("      </div>");
            writer.println("    </div>");
            writer.println("    <div id='network'>");
            writer.println("      <div class='info-box'>");
            writer.println("        <h4>💡 Conseils</h4>");
            writer.println("        <p><strong>Cliquer</strong> sur un nœud pour voir ses appels</p>");
            writer.println("        <p><strong>Glisser</strong> les nœuds pour réorganiser</p>");
            writer.println("        <p><strong>Scroll</strong> pour zoomer/dézoomer</p>");
            writer.println("      </div>");
            writer.println("    </div>");
            writer.println("  </div>");
            
            writer.println("  <script type='text/javascript'>");
            writer.println("    var allNodes = [];");
            writer.println("    var allEdges = [];");
            writer.println("    var network;");
            writer.println();
            
            // Create nodes
            writer.println("    var nodes = new vis.DataSet([");
            int nodeId = 0;
            Map<String, Integer> methodToId = new HashMap<>();
            
            for (ClassInfo classInfo : classes) {
                for (MethodInfo method : classInfo.methods) {
                    methodToId.put(classInfo.name + "." + method.name, nodeId);
                    String color = CLASS_COLORS.get(classInfo.name);
                    writer.println("      {");
                    writer.println("        id: " + nodeId + ",");
                    writer.println("        label: '" + method.name + "()',");
                    writer.println("        title: '" + classInfo.name + "." + method.name + "',");
                    writer.println("        group: '" + classInfo.name + "',");
                    writer.println("        color: { background: '" + color + "', border: '#2c3e50' },");
                    writer.println("        font: { color: '#ffffff', size: 13, bold: {mod: 'bold'} },");
                    writer.println("        shadow: true,");
                    writer.println("        borderWidth: 2");
                    writer.println("      },");
                    nodeId++;
                }
            }
            writer.println("    ]);");
            writer.println();
            
            // Create edges
            writer.println("    var edges = new vis.DataSet([");
            Set<String> edgeSet = new HashSet<>();
            for (ClassInfo classInfo : classes) {
                for (MethodInfo method : classInfo.methods) {
                    for (MethodInfo calledMethod : method.calls) {
                        String fromKey = classInfo.name + "." + method.name;
                        String toKey = null;
                        
                        // Find which class owns the called method
                        for (ClassInfo cls : classes) {
                            for (MethodInfo m : cls.methods) {
                                if (m.name.equals(calledMethod.name) && m.nbParameters == calledMethod.nbParameters) {
                                    toKey = cls.name + "." + calledMethod.name;
                                    break;
                                }
                            }
                            if (toKey != null) break;
                        }
                        
                        if (toKey != null) {
                            Integer fromId = methodToId.get(fromKey);
                            Integer toId = methodToId.get(toKey);
                            
                            if (fromId != null && toId != null) {
                                String edgeKey = fromId + "->" + toId;
                                if (!edgeSet.contains(edgeKey)) {
                                    edgeSet.add(edgeKey);
                                    writer.println("      { from: " + fromId + ", to: " + toId + ", arrows: 'to', smooth: { type: 'continuous' }, shadow: true },");
                                }
                            }
                        }
                    }
                }
            }
            writer.println("    ]);");
            writer.println();
            
            // Vis.js configuration
            writer.println("    var container = document.getElementById('network');");
            writer.println("    var data = { nodes: nodes, edges: edges };");
            writer.println("    var options = {");
            writer.println("      physics: {");
            writer.println("        enabled: true,");
            writer.println("        stabilization: { iterations: 300 },");
            writer.println("        barnesHut: { gravitationalConstant: -26000, centralGravity: 0.005, springLength: 200 }");
            writer.println("      },");
            writer.println("      interaction: { navigationButtons: true, keyboard: true, zoomView: true, dragView: true },");
            writer.println("      nodes: {");
            writer.println("        shape: 'dot',");
            writer.println("        scaling: { label: { enabled: true, min: 14, max: 30 } },");
            writer.println("        font: { size: 14, face: 'Segoe UI' }");
            writer.println("      },");
            writer.println("      edges: { color: { color: '#bdc3c7', highlight: '#e74c3c' }, width: 2 }");
            writer.println("    };");
            writer.println();
            
            writer.println("    network = new vis.Network(container, data, options);");
            writer.println();
            writer.println("    function resetGraph() {");
            writer.println("      network.fit();");
            writer.println("    }");
            writer.println();
            writer.println("    document.getElementById('searchInput').addEventListener('keyup', function(e) {");
            writer.println("      var search = e.target.value.toLowerCase();");
            writer.println("      var toHighlight = [];");
            writer.println("      nodes.forEach(function(node) {");
            writer.println("        if (node.label.toLowerCase().includes(search)) {");
            writer.println("          toHighlight.push(node.id);");
            writer.println("        }");
            writer.println("      });");
            writer.println("      network.selectNodes(toHighlight);");
            writer.println("    });");
            writer.println();
            writer.println("    network.on('click', function(params) {");
            writer.println("      if (params.nodes.length > 0) {");
            writer.println("        var nodeId = params.nodes[0];");
            writer.println("        network.selectNodes([nodeId]);");
            writer.println("      }");
            writer.println("    });");
            writer.println("  </script>");
            writer.println("</body>");
            writer.println("</html>");
            
            System.out.println("Graphe d'appels interactif généré: " + outputPath);
            
        } catch (IOException e) {
            System.err.println("Erreur lors de la génération du fichier HTML: " + e.getMessage());
        }
    }
}