package analyzer;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

/**
 * GUI for displaying application statistics in a window.
 * <p>
 * Uses Swing to show statistics about classes, methods, lines, packages, and more.
 */
public class AppStatsGUI {

    // Méthode pour afficher les statistiques
    /**
     * Displays the statistics in a graphical window.
     *
     * @param totalClasses Total number of classes
     * @param totalMethods Total number of methods
     * @param totalLines Total number of lines of code
     * @param totalPackages Total number of packages
     * @param avgMethodsPerClass Average number of methods per class
     * @param avgLinesPerMethod Average number of lines per method
     * @param avgAttributesPerClass Average number of attributes per class
     * @param topByMethods List of class names in the top 10% by number of methods
     * @param topByAttributes List of class names in the top 10% by number of attributes
     * @param topBoth Set of class names in both top categories
     * @param moreThanX List of class names with more than X methods
     * @param maxParams Maximum number of parameters in any method
     */
    public static void displayStats(int totalClasses, int totalMethods, int totalLines,
                                    long totalPackages, double avgMethodsPerClass,
                                    double avgLinesPerMethod, double avgAttributesPerClass,
                                    List<String> topByMethods, List<String> topByAttributes,
                                    Set<String> topBoth, List<String> moreThanX, int maxParams) {

        JFrame frame = new JFrame("Statistiques de l'application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Section 1 : Statistiques générales
        JPanel generalPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        generalPanel.setBorder(BorderFactory.createTitledBorder("Statistiques générales"));
        generalPanel.add(new JLabel("Nombre de classes: " + totalClasses));
        generalPanel.add(new JLabel("Nombre de méthodes: " + totalMethods));
        generalPanel.add(new JLabel("Nombre de lignes: " + totalLines));
        generalPanel.add(new JLabel("Nombre de packages: " + totalPackages));
        mainPanel.add(generalPanel);

        // Section 2 : Moyennes
        JPanel avgPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        avgPanel.setBorder(BorderFactory.createTitledBorder("Moyennes"));
        avgPanel.add(new JLabel(String.format("Moyennes de méthodes par classe: %.2f", avgMethodsPerClass)));
        avgPanel.add(new JLabel(String.format("Moyennes de lignes par méthode: %.2f", avgLinesPerMethod)));
        avgPanel.add(new JLabel(String.format("Moyennes d’attributs par classe: %.2f", avgAttributesPerClass)));
        mainPanel.add(avgPanel);

        // Section 3 : Top 10% des classes
        JPanel topPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        topPanel.setBorder(BorderFactory.createTitledBorder("Top 10% des classes"));
        topPanel.add(new JLabel("Par méthodes: " + topByMethods));
        topPanel.add(new JLabel("Par attributs: " + topByAttributes));
        topPanel.add(new JLabel("Dans les deux catégories: " + topBoth));
        mainPanel.add(topPanel);

        // Section 4 : Classes avec > X méthodes
        JPanel morePanel = new JPanel();
        morePanel.setBorder(BorderFactory.createTitledBorder("Classes avec plus de X méthodes"));
        morePanel.add(new JLabel(moreThanX.toString()));
        mainPanel.add(morePanel);

        // Section 5 : Nombre maximal de paramètres
        JPanel maxParamsPanel = new JPanel();
        maxParamsPanel.setBorder(BorderFactory.createTitledBorder("Nombre maximal de paramètres"));
        maxParamsPanel.add(new JLabel(String.valueOf(maxParams)));
        mainPanel.add(maxParamsPanel);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        frame.add(scrollPane);

        frame.setVisible(true);
    }
}