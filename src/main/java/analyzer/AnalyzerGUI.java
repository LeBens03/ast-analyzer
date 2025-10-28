package analyzer;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

import analyzer.Utils.ClassInfo;

/**
 * Swing-based graphical user interface for the Analyzer tool.
 *
 * <p>This class provides a lightweight desktop UI to:
 * <ul>
 *   <li>Select a source folder or Java file to analyze</li>
 *   <li>Display project statistics</li>
 *   <li>Generate interactive HTML visualizations: call graph and coupling graph</li>
 *   <li>Run hierarchical module identification (optionally using Spoon)</li>
 * </ul>
 * </p>
 *
 * <p>Long-running operations (source parsing, graph generation, Spoon analysis)
 * are executed in background threads using SwingWorker to avoid blocking the
 * Event Dispatch Thread (EDT). Generated reports are written to the project
 * root as HTML files and opened in the system browser when available.</p>
 */
public class AnalyzerGUI {
    private List<ClassInfo> allClasses;
    private Path currentPath;
    private JFrame mainFrame;
    private JPanel contentPanel;
    private static final Color PRIMARY_COLOR = new Color(41, 128, 185);
    private static final Color SECONDARY_COLOR = new Color(52, 73, 94);
    private static final Color ACCENT_COLOR = new Color(26, 188, 156);
    private static final Color BG_COLOR = new Color(236, 240, 241);

    /**
     * Creates an instance of AnalyzerGUI, initializing the folder selection process.
     */
    public AnalyzerGUI() {
        showFolderSelector();
    }

    /**
     * Shows the initial folder/file selection dialog and launches the analysis.
     * This method creates a modal-like selection window and uses a SwingWorker
     * to perform the source parsing off the EDT. On success it opens the main UI.
     */
    private void showFolderSelector() {
        JFrame selectorFrame = new JFrame("Analyzer - Sélection du dossier");
        selectorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        selectorFrame.setSize(600, 300);
        selectorFrame.setLocationRelativeTo(null);
        selectorFrame.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(20, 20));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel("Bienvenue dans Analyzer");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(PRIMARY_COLOR);
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 10, 10));
        centerPanel.setBackground(BG_COLOR);

        JLabel instructionLabel = new JLabel("<html>Sélectionnez un dossier ou un fichier Java à analyser</html>");
        instructionLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        centerPanel.add(instructionLabel);

        JTextField pathField = new JTextField();
        pathField.setEditable(false);
        pathField.setFont(new Font("Arial", Font.PLAIN, 12));
        pathField.setBackground(Color.WHITE);
        pathField.setBorder(BorderFactory.createLineBorder(PRIMARY_COLOR, 1));
        centerPanel.add(pathField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(BG_COLOR);

        JButton browseButton = createButton("Parcourir", PRIMARY_COLOR);
        JButton analyzeButton = createButton("Analyser", ACCENT_COLOR);
        analyzeButton.setEnabled(false);

        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setDialogTitle("Sélectionnez un dossier ou fichier Java");
            int result = chooser.showOpenDialog(selectorFrame);

            if (result == JFileChooser.APPROVE_OPTION) {
                currentPath = chooser.getSelectedFile().toPath();
                pathField.setText(currentPath.toString());
                analyzeButton.setEnabled(true);
            }
        });

        analyzeButton.addActionListener(e -> {
            browseButton.setEnabled(false);
            analyzeButton.setEnabled(false);
            browseButton.setText("Analyse en cours...");

            SwingWorker<List<ClassInfo>, Void> worker = new SwingWorker<List<ClassInfo>, Void>() {
                @Override
                protected List<ClassInfo> doInBackground() throws Exception {
                    return Analyzer.analyzeSource(currentPath);
                }

                @Override
                protected void done() {
                    try {
                        allClasses = get();
                        selectorFrame.dispose();
                        showMainWindow();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(selectorFrame,
                            "Erreur lors de l'analyse: " + ex.getMessage(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                        browseButton.setEnabled(true);
                        analyzeButton.setEnabled(true);
                        browseButton.setText("Parcourir");
                    }
                }
            };
            worker.execute();
        });

        buttonPanel.add(browseButton);
        buttonPanel.add(analyzeButton);
        centerPanel.add(buttonPanel);

        panel.add(centerPanel, BorderLayout.CENTER);
        selectorFrame.add(panel);
        selectorFrame.setVisible(true);
    }

    /**
     * Builds and shows the main application window. This method sets up the
     * menu bar, side navigation and default content panel. The UI components
     * are created on the EDT.
     */
    private void showMainWindow() {
        mainFrame = new JFrame("Analyzer - Analyse du code Java");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1200, 800);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(SECONDARY_COLOR);
        menuBar.setForeground(Color.WHITE);

        JMenu fileMenu = createMenu("Fichier");
        JMenuItem openItem = new JMenuItem("Ouvrir un autre dossier");
        openItem.addActionListener(e -> {
            mainFrame.dispose();
            showFolderSelector();
        });
        JMenuItem exitItem = new JMenuItem("Quitter");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        mainFrame.setJMenuBar(menuBar);

        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setBackground(SECONDARY_COLOR);
        sidePanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel sideTitle = new JLabel("Analyse");
        sideTitle.setFont(new Font("Arial", Font.BOLD, 16));
        sideTitle.setForeground(Color.WHITE);
        sidePanel.add(sideTitle);
        sidePanel.add(Box.createVerticalStrut(20));

        JButton statsBtn = createSideButton("📊 Statistiques");
        JButton callGraphBtn = createSideButton("📈 Graphe d'appels");
        JButton couplingBtn = createSideButton("🔗 Couplage");

        statsBtn.addActionListener(e -> showStatistics());
        callGraphBtn.addActionListener(e -> showCallGraph());
        couplingBtn.addActionListener(e -> showCoupling());

        sidePanel.add(statsBtn);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(callGraphBtn);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(couplingBtn);
        sidePanel.add(Box.createVerticalGlue());

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BG_COLOR);

        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(sidePanel, BorderLayout.WEST);
        mainContainer.add(contentPanel, BorderLayout.CENTER);

        mainFrame.add(mainContainer);
        mainFrame.setVisible(true);

        showStatistics();
    }

    /**
     * Replaces the content panel with the statistics view.
     * This delegates to AppStatsGUINew which constructs the visual elements.
     */
    private void showStatistics() {
        contentPanel.removeAll();
        JPanel statsPanel = new AppStatsGUINew(allClasses).createStatsPanel();
        contentPanel.add(statsPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Builds the call-graph generation panel. When the user triggers the
     * generation, the HTML report is produced (callgraph.html) and opened
     * in the system default browser.
     */
    private void showCallGraph() {
        contentPanel.removeAll();
        JPanel graphPanel = createGraphPanel("Graphe d'Appels",
            () -> CallGraphBuilder.generateHtmlGraph(allClasses, "callgraph.html"));
        contentPanel.add(graphPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Builds the coupling analysis panel. Supports two modes:
     * <ul>
     *   <li>Local coupling analysis using the in-memory ClassCouplingAnalyzer</li>
     *   <li>Spoon-based analysis (when the Spoon checkbox is checked)</li>
     * </ul>
     * The heavy work is executed in a SwingWorker; results are written to
     * coupling_graph.html and modules.html and then opened in the browser.
     */
    private void showCoupling() {
        contentPanel.removeAll();

        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Analyse du Couplage");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(PRIMARY_COLOR);
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BG_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel infoLabel = new JLabel(
            "<html><div style='text-align: center; width: 420px;'>" +
            "Générez le graphe de couplage et identifiez les modules via un clustering hiérarchique.<br><br>" +
            "Ajustez le seuil de couplage (0.0 - 1.0) pour filtrer les modules.</div></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        infoLabel.setForeground(SECONDARY_COLOR);
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        centerPanel.add(infoLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel thresholdLabel = new JLabel("Seuil de couplage (cp):");
        thresholdLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        centerPanel.add(thresholdLabel, gbc);

        gbc.gridx = 1;
        JTextField thresholdField = new JTextField("0.02", 6);
        thresholdField.setFont(new Font("Arial", Font.PLAIN, 13));
        centerPanel.add(thresholdField, gbc);

        // Spoon checkbox
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JCheckBox useSpoonCheck = new JCheckBox("Utiliser Spoon pour cette analyse");
        useSpoonCheck.setBackground(BG_COLOR);
        useSpoonCheck.setFont(new Font("Arial", Font.PLAIN, 13));
        useSpoonCheck.setSelected(false);
        centerPanel.add(useSpoonCheck, gbc);

        gbc.gridy = 3;
        JButton generateBtn = createButton("Générer Graphe & Modules", ACCENT_COLOR);
        centerPanel.add(generateBtn, gbc);

        generateBtn.addActionListener(e -> {
            generateBtn.setEnabled(false);
            double threshold = 0.02;
            try {
                threshold = Double.parseDouble(thresholdField.getText());
                if (threshold < 0 || threshold > 1) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(mainFrame,
                    "Seuil invalide. Entrez un nombre entre 0 et 1.",
                    "Valeur invalide", JOptionPane.ERROR_MESSAGE);
                generateBtn.setEnabled(true);
                return;
            }

            final double finalThreshold = threshold;
            final boolean useSpoon = useSpoonCheck.isSelected();

            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    if (useSpoon) {
                        SpoonRunner.runSpoonAnalysis(currentPath, finalThreshold);
                    } else {
                        ClassCouplingAnalyzer analyzer = new ClassCouplingAnalyzer(allClasses);
                        analyzer.generateHtmlGraph("coupling_graph.html");

                        HierarchicalClusteringAnalyzer hc =
                            new HierarchicalClusteringAnalyzer(allClasses, analyzer.getNormalizedCouplings());
                        hc.runClusteringAndIdentifyModules(finalThreshold);
                        hc.generateHtmlModules("modules.html");
                    }
                    return null;
                }

                @Override
                protected void done() {
                    generateBtn.setEnabled(true);
                    JOptionPane.showMessageDialog(mainFrame,
                        "Analyses générées: coupling_graph.html et modules.html",
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                    openInBrowser("coupling_graph.html");
                    openInBrowser("modules.html");
                }
            };
            worker.execute();
        });

        panel.add(centerPanel, BorderLayout.CENTER);
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Create a reusable panel for graph-generation actions. The provided
     * generateAction Runnable is executed synchronously by the button handler
     * but should itself perform work off the EDT if long-running.
     *
     * @param title display title for the panel
     * @param generateAction action to execute when the user clicks the button
     * @return configured JPanel that can be added to the main content area
     */
    private JPanel createGraphPanel(String title, Runnable generateAction) {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(BG_COLOR);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(PRIMARY_COLOR);
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BG_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel infoLabel = new JLabel(
            "<html><div style='text-align: center; width: 400px;'>" +
            "Cliquez sur le bouton ci-dessous pour générer le " + title.toLowerCase() +
            " interactif en HTML.<br><br>" +
            "Un navigateur s'ouvrira automatiquement avec la visualisation." +
            "</div></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        infoLabel.setForeground(SECONDARY_COLOR);
        centerPanel.add(infoLabel, gbc);

        JButton generateBtn = createButton("Générer " + title, ACCENT_COLOR);
        gbc.gridy = 1;
        centerPanel.add(generateBtn, gbc);

        generateBtn.addActionListener(e -> {
            try {
                generateAction.run();
                JOptionPane.showMessageDialog(mainFrame,
                    "Graphe généré avec succès!\nLe fichier HTML s'ouvre dans votre navigateur...",
                    "Succès", JOptionPane.INFORMATION_MESSAGE);

                String filename = title.equals("Graphe d'Appels") ? "callgraph.html" : "coupling_graph.html";
                openInBrowser(filename);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(mainFrame,
                    "Erreur: " + ex.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Attempts to open the supplied filename in the user's default browser.
     * The method is tolerant: if the desktop/browse action is not supported
     * it simply logs the error.
     *
     * @param filename relative or absolute path to the HTML file to open
     */
    private void openInBrowser(String filename) {
        try {
            Path filePath = Paths.get(filename).toAbsolutePath();
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(filePath.toUri());
            }
        } catch (IOException e) {
            System.err.println("Impossible d'ouvrir le navigateur: " + e.getMessage());
        }
    }

    /**
     * Create a styled JButton used across the UI. This helper centralizes the
     * look-and-feel used by the panels in the application.
     */
    private JButton createButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(darkenColor(bgColor, 0.8f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    /**
     * Create a side navigation button (used in the left sidebar). Buttons
     * created here have a consistent style and mouse hover behavior.
     */
    private JButton createSideButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 13));
        button.setBackground(PRIMARY_COLOR);
        button.setForeground(Color.WHITE);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setBorder(new EmptyBorder(10, 15, 10, 15));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setContentAreaFilled(true);
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_COLOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(PRIMARY_COLOR);
            }
        });

        return button;
    }

    /**
     * Helper to create a JMenu with consistent styling.
     */
    private JMenu createMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setFont(new Font("Arial", Font.PLAIN, 12));
        menu.setForeground(Color.WHITE);
        return menu;
    }

    /**
     * Darken a color by the specified factor (0..1). Used to create hover
     * states for buttons in the UI.
     */
    private Color darkenColor(Color color, float factor) {
        return new Color(
            (int) (color.getRed() * factor),
            (int) (color.getGreen() * factor),
            (int) (color.getBlue() * factor)
        );
    }

    /**
     * Application entry point — constructs the AnalyzerGUI on the EDT.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(AnalyzerGUI::new);
    }
}