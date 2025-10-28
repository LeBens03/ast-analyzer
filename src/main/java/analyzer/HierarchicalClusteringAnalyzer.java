package analyzer;

import analyzer.Utils.Dendro;
import analyzer.Utils.Cluster;
import analyzer.Utils.ClassInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.FileWriter;
import java.io.IOException;


/**
 * Performs hierarchical (agglomerative) clustering over classes using a normalized
 * class-coupling map as similarity. The analyzer builds a dendrogram by
 * repeatedly merging the pair of clusters with highest average inter-cluster
 * coupling. After clustering, subtrees can be identified as candidate modules
 * using a coupling threshold.
 *
 * <p>Key behavior:
 * <ul>
 *   <li>The constructor takes a list of ClassInfo and a map of normalized
 *       coupling scores between class pairs (keys are canonicalized as "A-B").</li>
 *   <li>runClustering() produces the dendrogram (dendro.clusters holds levels).</li>
 *   <li>identifyModules(cp) traverses the dendrogram, extracts subtree candidates
 *       and filters them by their internal average coupling >= cp.</li>
 *   <li>generateHtmlModules(filename) writes a simple HTML report listing found modules.</li>
 * </ul></p>
 *
 * <p>Notes and limitations:
 * <ul>
 *   <li>Coupling values are expected to be normalized (0..1). If absolute call
 *       counts are provided the identification heuristic may need tuning.</li>
 *   <li>The clustering strategy is average-link (mean pairwise coupling)
 *       approximated by computeInterClusterCoupling.</li>
 *   <li>The implementation is intentionally small and readable: for large codebases
 *       consider optimizations (priority queues, sparse data structures).</li>
 * </ul>
 * </p>
 */
public class HierarchicalClusteringAnalyzer {
	
	private Dendro dendro;
	private List<ClassInfo> classes;
	private Map<String, Double> normalizedCouplings;
	private ArrayList<Dendro> modules;
	
	/**
	 * Constructs an analyzer instance with the given class information and coupling map.
	 * Initializes the dendrogram with each class as a separate cluster.
	 * 
	 * @param classes List of ClassInfo objects representing the classes to be analyzed.
	 * @param couplings Map where keys are class pair identifiers (e.g., "A-B") and values
	 *                  are the normalized coupling scores between the classes.
	 */
	public HierarchicalClusteringAnalyzer(List<ClassInfo> classes, Map<String, Double> couplings) {
		dendro = new Dendro();
		this.classes = classes;
		normalizedCouplings = couplings;
		
		ArrayList<Cluster> leafClusters = new ArrayList<>();
		
		for (ClassInfo cls: classes) {
			Cluster leafCluster = new Cluster();
			leafCluster.classes.add(cls);
			leafClusters.add(leafCluster);
		}
		
		dendro.clusters.add(leafClusters);
	}
	
	/**
	 * Gets coupling value between two classes
	 */
	private double getCoupling(ClassInfo class1, ClassInfo class2) {
	    String key1 = class1.name + "-" + class2.name;
	    String key2 = class2.name + "-" + class1.name;
	    
	    // Try both key combinations since we don't know the order
	    if (normalizedCouplings.containsKey(key1)) {
	        return normalizedCouplings.get(key1);
	    } else if (normalizedCouplings.containsKey(key2)) {
	        return normalizedCouplings.get(key2);
	    }
	    return 0.0; // No coupling found
	}
	
	private double computeInterClusterCoupling(Cluster c1, Cluster c2) {
		double totalCoupling = 0.0;
		
		for (ClassInfo class1: c1.classes) {
			for (ClassInfo class2: c2.classes) {
				totalCoupling += getCoupling(class1, class2);
			}
		}
		
		return totalCoupling/(c1.classes.size() * c2.classes.size());
	}
	
	private Cluster[] findBestPair(ArrayList<Cluster> currentClusters) {
	    Cluster bestC1 = null;
	    Cluster bestC2 = null;
	    double maxCoupling = -1.0;
	    
	    for (int i=0; i < currentClusters.size(); i++) {
	    	for (int j=i+1; j < currentClusters.size(); j++) {
				double coupling = computeInterClusterCoupling(currentClusters.get(i), currentClusters.get(j));
				if (coupling > maxCoupling) {
					bestC1 = currentClusters.get(i);
					bestC2 = currentClusters.get(j);
					maxCoupling = coupling;
				}
			}
	    }
	    return new Cluster[]{bestC1, bestC2};
	}
	
	private Cluster mergeClusters(Cluster c1, Cluster c2, double couplingValue) {
		List<ClassInfo> mergedClusterClasses = new ArrayList<>();
		mergedClusterClasses.addAll(c1.classes);
		mergedClusterClasses.addAll(c2.classes);
		Cluster mergedCluster = new Cluster(mergedClusterClasses, c1, c2, couplingValue);
		return mergedCluster;
	}
	
	private void performClustering() {
	    int level = 0;
	    
	    while (dendro.clusters.get(level).size() > 1) {
	        ArrayList<Cluster> currentClusters = dendro.clusters.get(level);
	        
	        // Find best pair
	        Cluster[] bestPair = findBestPair(currentClusters);
	        double coupling = computeInterClusterCoupling(bestPair[0], bestPair[1]);
	        
	        // Merge them
	        Cluster mergedCluster = mergeClusters(bestPair[0], bestPair[1], coupling);
	        
	        // Create next level with merged cluster + remaining clusters
	        ArrayList<Cluster> nextLevel = new ArrayList<>();
	        for (Cluster c : currentClusters) {
	            if (c != bestPair[0] && c != bestPair[1]) {
	                nextLevel.add(c);
	            }
	        }
	        nextLevel.add(mergedCluster);
	        
	        dendro.clusters.add(nextLevel);
	        level++;
	    }
	}

	private void traverseDendrogram(Cluster cluster) {
	    if (cluster == null) return;

	    // Build a module (subtree) rooted at this cluster
	    Dendro module = new Dendro();
	    ArrayList<Cluster> clustersInSubtree = new ArrayList<>();
	    collectClusters(cluster, clustersInSubtree);
	    module.clusters.add(clustersInSubtree);
	    modules.add(module);

	    // Recurse to children
	    traverseDendrogram(cluster.leftChild);
	    traverseDendrogram(cluster.rightChild);
	}
	
	private double computeInternModuleCoupling(Dendro module) {
	    double totalCoupling = 0.0;
	    ArrayList<Cluster> clustersInModule = module.clusters.get(0);

	    for (int i = 0; i < clustersInModule.size(); i++) {
	        for (int j = i + 1; j < clustersInModule.size(); j++) {
	            totalCoupling += computeInterClusterCoupling(clustersInModule.get(i), clustersInModule.get(j));
	        }
	    }

	    int n = clustersInModule.size();
	    if (n <= 1) return 0.0;

	    // Normalize by number of cluster pairs
	    return totalCoupling / (n * (n - 1) / 2);
    }
	
	private void performModuleIdentification(double cp) {
	    modules = new ArrayList<>();
	    // get root cluster (last level, first element)
	    Cluster root = dendro.clusters.get(dendro.clusters.size() - 1).get(0);

	    // Step 1 — gather all subtrees (modules)
	    traverseDendrogram(root);

	    // Step 2 — filter by coupling threshold
	    modules.removeIf(module -> computeInternModuleCoupling(module) < cp);

	    // Step 3 — refine if too many modules remain
	    if (modules.size() > classes.size() / 2) {
	        modules = modules.stream()
	                .sorted((m1, m2) -> Double.compare(
	                        computeInternModuleCoupling(m2),
	                        computeInternModuleCoupling(m1)
	                ))
	                .limit(classes.size() / 2)
	                .collect(Collectors.toCollection(ArrayList::new));
	    }
	}

	private void collectClusters(Cluster cluster, ArrayList<Cluster> collector) {
	    if (cluster == null) return;
	    collector.add(cluster);
	    collectClusters(cluster.leftChild, collector);
	    collectClusters(cluster.rightChild, collector);
	}

	// Public API -------------------------------------------------------------
	
	/**
	 * Runs the hierarchical clustering process (agglomerative) based on the coupling map.
	 */
	public void runClustering() {
		performClustering();
	}

	/**
	 * After clustering, identify modules using the provided coupling threshold (cp).
	 */
	public void identifyModules(double cp) {
		performModuleIdentification(cp);
	}

	/**
	 * Convenience method: run clustering then identify modules
	 */
	public void runClusteringAndIdentifyModules(double cp) {
		runClustering();
		identifyModules(cp);
	}

	/**
	 * Returns a simple representation of modules: list of modules, each module is a list of class names.
	 */
	public List<List<String>> getModulesAsClassNames() {
		List<List<String>> result = new ArrayList<>();
		if (modules == null) return result;
		for (Dendro d : modules) {
			ArrayList<Cluster> clustersInModule = d.clusters.get(0);
			List<String> moduleClasses = new ArrayList<>();
			for (Cluster c : clustersInModule) {
				for (ClassInfo ci : c.classes) {
					moduleClasses.add(ci.name);
				}
			}
			result.add(moduleClasses);
		}
		return result;
	}

	/**
	 * Generate a simple HTML report listing identified modules and their internal coupling score.
	 */
	public void generateHtmlModules(String filename) throws IOException {
		StringBuilder html = new StringBuilder();
		html.append("<!DOCTYPE html>\n<html lang='fr'>\n<head>\n<meta charset='utf-8'>\n<meta name='viewport' content='width=device-width,initial-scale=1.0'>\n<title>Modules identifiés</title>\n<style>body{font-family:Arial,Helvetica,sans-serif;padding:20px;background:#f5f5f5} .module{background:#fff;padding:12px;margin:10px 0;border-radius:6px;box-shadow:0 2px 6px rgba(0,0,0,0.08)} .title{color:#2c3e50}</style>\n</head>\n<body>\n");
		html.append("<h1>Modules identifiés</h1>\n");
		if (modules == null || modules.isEmpty()) {
			html.append("<p>Aucun module identifié.</p>\n");
		} else {
			int idx = 1;
			for (Dendro d : modules) {
				double score = computeInternModuleCoupling(d);
				html.append("<div class='module'>\n");
				html.append("<h2 class='title'>Module " + idx++ + "</h2>\n");
				ArrayList<Cluster> clustersInModule = d.clusters.get(0);
				html.append("<p><strong>Interne (normalisée):</strong> " + String.format("%.4f", score) + "</p>\n");
				html.append("<p><strong>Classes:</strong> ");
				List<String> names = new ArrayList<>();
				for (Cluster c : clustersInModule) {
					for (ClassInfo ci : c.classes) names.add(ci.name);
				}
				html.append(String.join(", ", names));
				html.append("</p>\n</div>\n");
			}
		}
		html.append("</body>\n</html>\n");
		try (FileWriter fw = new FileWriter(filename)) {
			fw.write(html.toString());
		}
	}
}