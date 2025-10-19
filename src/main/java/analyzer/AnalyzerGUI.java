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
 * Modern unified GUI for the Analyzer tool.
 * Provides folder selection, statistics display, call graph generation, and coupling analysis.
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

    public AnalyzerGUI() {
        showFolderSelector();
    }

    /**
     * Displays initial folder selection dialog
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
     * Displays the main analysis window
     */
    private void showMainWindow() {
        mainFrame = new JFrame("Analyzer - Analyse du code Java");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1200, 800);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Create menu bar
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

        // Create sidebar with buttons
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

        // Content panel
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BG_COLOR);

        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.add(sidePanel, BorderLayout.WEST);
        mainContainer.add(contentPanel, BorderLayout.CENTER);

        mainFrame.add(mainContainer);
        mainFrame.setVisible(true);

        showStatistics();
    }

    /**
     * Display statistics view
     */
    private void showStatistics() {
        contentPanel.removeAll();
        JPanel statsPanel = new AppStatsGUINew(allClasses).createStatsPanel();
        contentPanel.add(statsPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Display call graph view
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
     * Display coupling analysis view
     */
    private void showCoupling() {
        contentPanel.removeAll();
        JPanel couplingPanel = createGraphPanel("Analyse du Couplage",
            () -> {
                ClassCouplingAnalyzer analyzer = new ClassCouplingAnalyzer(allClasses);
                try {
					analyzer.generateHtmlGraph("coupling_graph.html");
				} catch (IOException e) {
					e.printStackTrace();
				}
            });
        contentPanel.add(couplingPanel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    /**
     * Create a panel for graph operations
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
                
                // Open in browser
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
     * Open HTML file in default browser
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

    // UI Helper methods
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

    private JMenu createMenu(String text) {
        JMenu menu = new JMenu(text);
        menu.setFont(new Font("Arial", Font.PLAIN, 12));
        menu.setForeground(Color.WHITE);
        return menu;
    }

    private Color darkenColor(Color color, float factor) {
        return new Color(
            (int) (color.getRed() * factor),
            (int) (color.getGreen() * factor),
            (int) (color.getBlue() * factor)
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AnalyzerGUI::new);
    }
}