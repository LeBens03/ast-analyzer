package analyzer;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class AppStatsGUI {

    // Méthode pour afficher les statistiques
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