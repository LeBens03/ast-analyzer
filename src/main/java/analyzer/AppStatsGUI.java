package analyzer;

import javax.swing.*;

import analyzer.Utils.ClassInfo;
import analyzer.Utils.MethodInfo;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AppStatsGUI provides a graphical interface for displaying application statistics.
 * <p>
 * This class uses Swing to present:
 * <ul>
 *   <li>General statistics: number of classes, methods, lines, packages</li>
 *   <li>Averages: methods per class, lines per method, attributes per class</li>
 *   <li>Top 10% classes by methods and attributes, and their intersection</li>
 *   <li>Interactive input for X to show classes with more than X methods</li>
 *   <li>Top 10% methods by lines per class</li>
 *   <li>Maximum number of parameters in any method</li>
 * </ul>
 * <p>
 * The interface is scrollable and interactive, allowing users to explore statistics visually.
 * <p>
 * Usage: Call {@code AppStatsGUI.displayStats(...)} with the required statistics and lists.
 */
public class AppStatsGUI {

	/**
	 * Displays the application statistics in a graphical window.
	 * <p>
	 * All statistics are shown in organized panels. The user can input X to filter classes by method count.
	 * @param totalClasses Total number of classes
	 * @param totalMethods Total number of methods
	 * @param totalLines Total number of lines
	 * @param totalPackages Total number of packages
	 * @param avgMethodsPerClass Average methods per class
	 * @param avgLinesPerMethod Average lines per method
	 * @param avgAttributesPerClass Average attributes per class
	 * @param topByMethods Top 10% classes by methods
	 * @param topByAttributes Top 10% classes by attributes
	 * @param topBoth Classes in both top categories
	 * @param allClasses All classes in the project
	 * @param topMethodsByLines Top 10% methods by lines per class
	 * @param maxParams Maximum number of parameters in any method
	 */
	public static void displayStats(
	        int totalClasses,
	        int totalMethods,
	        int totalLines,
	        long totalPackages,
	        double avgMethodsPerClass,
	        double avgLinesPerMethod,
	        double avgAttributesPerClass,
	        List<ClassInfo> topByMethods,
	        List<ClassInfo> topByAttributes,
	        Set<ClassInfo> topBoth,
	        List<ClassInfo> allClasses, 
	        Map<ClassInfo, List<MethodInfo>> topMethodsByLines,
	        int maxParams
	) {
	    JFrame frame = new JFrame("Statistiques de l'application");
	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    frame.setSize(900, 700);

	    JPanel mainPanel = new JPanel();
	    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
	    mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

	    // General stats
	    JPanel generalPanel = new JPanel(new GridLayout(2, 2, 10, 10));
	    generalPanel.setBorder(BorderFactory.createTitledBorder("Statistiques générales"));
	    generalPanel.add(new JLabel("Nombre de classes: " + totalClasses));
	    generalPanel.add(new JLabel("Nombre de méthodes: " + totalMethods));
	    generalPanel.add(new JLabel("Nombre de lignes: " + totalLines));
	    generalPanel.add(new JLabel("Nombre de packages: " + totalPackages));
	    mainPanel.add(generalPanel);

	    // Averages
	    JPanel avgPanel = new JPanel(new GridLayout(3, 1, 10, 10));
	    avgPanel.setBorder(BorderFactory.createTitledBorder("Moyennes"));
	    avgPanel.add(new JLabel(String.format("Moyenne de méthodes par classe: %.2f", avgMethodsPerClass)));
	    avgPanel.add(new JLabel(String.format("Moyenne de lignes par méthode: %.2f", avgLinesPerMethod)));
	    avgPanel.add(new JLabel(String.format("Moyenne d’attributs par classe: %.2f", avgAttributesPerClass)));
	    mainPanel.add(avgPanel);

	    // Top classes
	    JPanel topPanel = new JPanel(new GridLayout(3, 1, 10, 10));
	    topPanel.setBorder(BorderFactory.createTitledBorder("Top 10% des classes"));
	    topPanel.add(new JLabel("Par méthodes: " + topByMethods.stream().map(c -> c.name).toList()));
	    topPanel.add(new JLabel("Par attributs: " + topByAttributes.stream().map(c -> c.name).toList()));
	    topPanel.add(new JLabel("Dans les deux catégories: " + topBoth.stream().map(c -> c.name).toList()));
	    mainPanel.add(topPanel);

	    // Input X directly in GUI
	    JPanel morePanel = new JPanel();
	    morePanel.setBorder(BorderFactory.createTitledBorder("Classes avec plus de X méthodes"));
	    JTextField xField = new JTextField(5);
	    JButton applyButton = new JButton("Appliquer");
	    JTextArea resultArea = new JTextArea(5, 40);
	    resultArea.setEditable(false);
	    morePanel.add(new JLabel("Valeur de X: "));
	    morePanel.add(xField);
	    morePanel.add(applyButton);
	    morePanel.add(new JScrollPane(resultArea));
	    mainPanel.add(morePanel);

	    applyButton.addActionListener(e -> {
	        try {
	            int X = Integer.parseInt(xField.getText());
	            List<ClassInfo> moreThanX = allClasses.stream()
	                    .filter(c -> c.nbMethods > X)
	                    .toList();
	            resultArea.setText(moreThanX.stream().map(c -> c.name).toList().toString());
	        } catch (NumberFormatException ex) {
	            JOptionPane.showMessageDialog(frame, "Valeur invalide.", "Erreur", JOptionPane.ERROR_MESSAGE);
	        }
	    });

	    // Top methods by lines per class
	    JPanel topMethodsPanel = new JPanel();
	    topMethodsPanel.setBorder(BorderFactory.createTitledBorder("Top 10% méthodes par lignes (par classe)"));
	    JTextArea methodsArea = new JTextArea(10, 60);
	    methodsArea.setEditable(false);
	    for (ClassInfo c : topMethodsByLines.keySet()) {
	        String line = c.name + ": " + topMethodsByLines.get(c).stream().map(m -> m.name).toList();
	        methodsArea.append(line + "\n");
	    }
	    topMethodsPanel.add(new JScrollPane(methodsArea));
	    mainPanel.add(topMethodsPanel);

	    // Max parameters
	    JPanel maxParamsPanel = new JPanel();
	    maxParamsPanel.setBorder(BorderFactory.createTitledBorder("Nombre maximal de paramètres"));
	    maxParamsPanel.add(new JLabel(String.valueOf(maxParams)));
	    mainPanel.add(maxParamsPanel);

	    JScrollPane scrollPane = new JScrollPane(mainPanel);
	    frame.add(scrollPane);
	    frame.setVisible(true);
	}
}