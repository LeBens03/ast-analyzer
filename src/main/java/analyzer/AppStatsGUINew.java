package analyzer;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import analyzer.Utils.ClassInfo;
import analyzer.Utils.MethodInfo;

/**
 * Modern statistics display panel for the Analyzer GUI.
 * Shows all application statistics in an organized, aesthetic layout.
 */
public class AppStatsGUINew {
    private List<ClassInfo> classes;
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color BG_COLOR = new Color(236, 240, 241);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT_COLOR = new Color(44, 62, 80);

    public AppStatsGUINew(List<ClassInfo> classes) {
        this.classes = classes;
    }

    /**
     * Create the main statistics panel
     */
    public JPanel createStatsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);

        // Scroll pane for all content
        JScrollPane scrollPane = new JScrollPane(createContentPanel());
        scrollPane.setBackground(BG_COLOR);
        scrollPane.getViewport().setBackground(BG_COLOR);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(BG_COLOR);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Calculate statistics
        int totalClasses = classes.size();
        int totalMethods = classes.stream().mapToInt(c -> c.nbMethods).sum();
        int totalLines = classes.stream().flatMap(c -> c.methods.stream()).mapToInt(m -> m.nbLines).sum();
        int totalAttributes = classes.stream().mapToInt(c -> c.nbAttributes).sum();
        long totalPackages = classes.stream().map(c -> c.packageName).distinct().count();

        double avgMethodsPerClass = totalClasses > 0 ? (double) totalMethods / totalClasses : 0;
        double avgLinesPerMethod = totalMethods > 0 ? (double) totalLines / totalMethods : 0;
        double avgAttributesPerClass = totalClasses > 0 ? (double) totalAttributes / totalClasses : 0;

        int topN = Math.max(1, (int) Math.ceil(0.1 * totalClasses));
        List<ClassInfo> topByMethods = classes.stream()
                .sorted((c1, c2) -> Integer.compare(c2.nbMethods, c1.nbMethods))
                .limit(topN).collect(Collectors.toList());

        List<ClassInfo> topByAttributes = classes.stream()
                .sorted((c1, c2) -> Integer.compare(c2.nbAttributes, c1.nbAttributes))
                .limit(topN).collect(Collectors.toList());

        Set<ClassInfo> topBoth = topByMethods.stream()
                .filter(topByAttributes::contains)
                .collect(Collectors.toSet());

        Map<ClassInfo, List<MethodInfo>> topMethodsByLines = classes.stream()
                .collect(Collectors.toMap(
                        c -> c,
                        c -> c.methods.stream()
                                .sorted((m1, m2) -> Integer.compare(m2.nbLines, m1.nbLines))
                                .limit(topN).collect(Collectors.toList())
                ));

        int maxParams = classes.stream()
                .flatMap(c -> c.methods.stream())
                .mapToInt(m -> m.nbParameters)
                .max().orElse(0);

        // Title
        JLabel titleLabel = new JLabel("Statistiques de l'Application");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(PRIMARY_COLOR);
        contentPanel.add(titleLabel);
        contentPanel.add(Box.createVerticalStrut(20));

        // General Statistics Cards
        contentPanel.add(createSectionLabel("Statistiques Générales"));
        JPanel generalPanel = createCardGrid(4, 2);
        generalPanel.add(createStatCard("Classes", String.valueOf(totalClasses)));
        generalPanel.add(createStatCard("Méthodes", String.valueOf(totalMethods)));
        generalPanel.add(createStatCard("Lignes de Code", String.valueOf(totalLines)));
        generalPanel.add(createStatCard("Packages", String.valueOf(totalPackages)));
        generalPanel.add(createStatCard("Attributs", String.valueOf(totalAttributes)));
        contentPanel.add(generalPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Averages
        contentPanel.add(createSectionLabel("Moyennes"));
        JPanel avgPanel = createCardGrid(3, 2);
        avgPanel.add(createStatCard("Méthodes/Classe", String.format("%.2f", avgMethodsPerClass)));
        avgPanel.add(createStatCard("Lignes/Méthode", String.format("%.2f", avgLinesPerMethod)));
        avgPanel.add(createStatCard("Attributs/Classe", String.format("%.2f", avgAttributesPerClass)));
        contentPanel.add(avgPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Top Classes
        contentPanel.add(createSectionLabel("Top 10% des Classes"));
        JPanel topPanel = createCardGrid(1, 1);
        StringBuilder topByMethodsStr = new StringBuilder("<html><b>Par méthodes:</b> ");
        topByMethods.forEach(c -> topByMethodsStr.append(c.name).append(", "));
        topPanel.add(createTextCard(topByMethodsStr.toString().replaceAll(", $", "") + "</html>"));

        StringBuilder topByAttributesStr = new StringBuilder("<html><b>Par attributs:</b> ");
        topByAttributes.forEach(c -> topByAttributesStr.append(c.name).append(", "));
        topPanel.add(createTextCard(topByAttributesStr.toString().replaceAll(", $", "") + "</html>"));

        StringBuilder topBothStr = new StringBuilder("<html><b>Dans les deux catégories:</b> ");
        if (topBoth.isEmpty()) {
            topBothStr.append("Aucune");
        } else {
            topBoth.forEach(c -> topBothStr.append(c.name).append(", "));
            topBothStr.deleteCharAt(topBothStr.length() - 2);
        }
        topPanel.add(createTextCard(topBothStr.toString() + "</html>"));
        contentPanel.add(topPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Classes with more than X methods
        contentPanel.add(createSectionLabel("Filtrer les Classes"));
        JPanel filterPanel = new JPanel(new BorderLayout(10, 10));
        filterPanel.setBackground(BG_COLOR);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        inputPanel.setBackground(CARD_BG);
        inputPanel.setBorder(new LineBorder(PRIMARY_COLOR, 1));

        JLabel filterLabel = new JLabel("Classes avec plus de X méthodes:");
        filterLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        JTextField xField = new JTextField(5);
        xField.setFont(new Font("Arial", Font.PLAIN, 12));

        JTextArea resultArea = new JTextArea(3, 80);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        resultArea.setFont(new Font("Arial", Font.PLAIN, 11));
        resultArea.setBorder(new LineBorder(PRIMARY_COLOR, 1));

        JButton applyButton = new JButton("Appliquer");
        applyButton.setBackground(PRIMARY_COLOR);
        applyButton.setForeground(Color.WHITE);
        applyButton.setFont(new Font("Arial", Font.BOLD, 11));
        applyButton.setFocusPainted(false);
        applyButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        applyButton.addActionListener(e -> {
            try {
                int X = Integer.parseInt(xField.getText());
                List<ClassInfo> moreThanX = classes.stream()
                        .filter(c -> c.nbMethods > X)
                        .collect(Collectors.toList());
                resultArea.setText(moreThanX.stream()
                        .map(c -> c.name + " (" + c.nbMethods + " méthodes)")
                        .collect(Collectors.joining(", ")));
                if (moreThanX.isEmpty()) {
                    resultArea.setText("Aucune classe trouvée");
                }
            } catch (NumberFormatException ex) {
                resultArea.setText("Valeur invalide");
            }
        });

        inputPanel.add(filterLabel);
        inputPanel.add(xField);
        inputPanel.add(applyButton);

        filterPanel.add(inputPanel, BorderLayout.NORTH);
        filterPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);
        contentPanel.add(filterPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Top methods by lines
        contentPanel.add(createSectionLabel("Top 10% Méthodes par Lignes"));
        JPanel methodsPanel = createCardGrid(1, 1);
        StringBuilder methodsStr = new StringBuilder("<html>");
        for (ClassInfo c : topMethodsByLines.keySet()) {
            methodsStr.append("<b>").append(c.name).append(":</b> ");
            methodsStr.append(topMethodsByLines.get(c).stream()
                    .map(m -> m.name + "(" + m.nbLines + "L)")
                    .collect(Collectors.joining(", ")));
            methodsStr.append("<br>");
        }
        methodsStr.append("</html>");
        methodsPanel.add(createTextCard(methodsStr.toString()));
        contentPanel.add(methodsPanel);
        contentPanel.add(Box.createVerticalStrut(20));

        // Max parameters
        contentPanel.add(createSectionLabel("Complexité"));
        JPanel complexPanel = createCardGrid(1, 1);
        complexPanel.add(createStatCard("Nombre Max de Paramètres", String.valueOf(maxParams)));
        contentPanel.add(complexPanel);

        return contentPanel;
    }

    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(PRIMARY_COLOR);
        return label;
    }

    private JPanel createCardGrid(int cols, int rows) {
        JPanel panel = new JPanel(new GridLayout(rows, cols, 15, 15));
        panel.setBackground(BG_COLOR);
        return panel;
    }

    private JPanel createStatCard(String label, String value) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(189, 195, 199), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel labelJLabel = new JLabel(label);
        labelJLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        labelJLabel.setForeground(new Color(127, 140, 141));

        JLabel valueJLabel = new JLabel(value);
        valueJLabel.setFont(new Font("Arial", Font.BOLD, 18));
        valueJLabel.setForeground(PRIMARY_COLOR);

        card.add(labelJLabel, BorderLayout.NORTH);
        card.add(valueJLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createTextCard(String text) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(189, 195, 199), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel textLabel = new JLabel(text);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        textLabel.setForeground(TEXT_COLOR);

        card.add(textLabel, BorderLayout.NORTH);
        return card;
    }
}