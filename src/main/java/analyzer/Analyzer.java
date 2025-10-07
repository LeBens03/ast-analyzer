package analyzer;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import analyzer.Utils.ClassInfo;
import analyzer.Utils.MethodInfo;

import org.eclipse.jdt.core.dom.ASTVisitor;

/**
 * Analyzer is a tool for analyzing Java source files and directories.
 * <p>
 * It computes various statistics about the codebase, such as:
 * <ul>
 *   <li>Number of classes, methods, lines of code, packages</li>
 *   <li>Average methods/attributes per class, average lines per method</li>
 *   <li>Top 10% classes by methods/attributes, intersection of these sets</li>
 *   <li>Classes with more than X methods</li>
 *   <li>Top 10% methods by lines of code (per class)</li>
 *   <li>Maximum number of parameters in any method</li>
 * </ul>
 * It also builds a call graph and exports it in DOT and HTML formats.
 * <p>
 * Usage: <code>java Analyzer</code>
 * <p>
 * Interactive features:
 * <ul>
 *   <li>Command-line interface for path selection and statistics/call graph navigation</li>
 *   <li>Statistics submenu with dynamic parameter input (e.g., X for method count)</li>
 *   <li>Call graph display in console or browser</li>
 * </ul>
 * <p>
 * Dependencies: Eclipse JDT Core (ASTParser), AppStatsGUI for graphical statistics
 */
public class Analyzer {
    /**
     * Entry point for the Analyzer tool.
     * <p>
     * Interactive command-line application for analyzing Java source files and directories.
     * <ul>
     *   <li>Prompts user for a path to analyze</li>
     *   <li>Presents menu for statistics and call graph</li>
     *   <li>Statistics menu includes all codebase metrics and a graphical interface option</li>
     *   <li>Call graph can be displayed in console or browser</li>
     * </ul>
     * @param args Not used; all input is interactive
     * @throws IOException if file reading fails
     */
	public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bienvenue dans Analyzer!");
        Path inputPath = null;
        while (inputPath == null) {
            System.out.print("Veuillez entrer le chemin du fichier ou dossier Java à analyser: ");
            String pathStr = scanner.nextLine();
            Path p = Paths.get(pathStr);
            if ((Files.isDirectory(p)) || (Files.isRegularFile(p) && p.toString().endsWith(".java"))) {
                inputPath = p;
            } else {
                System.out.println("Chemin invalide ou pas un .java: " + p);
            }
        }
        List<ClassInfo> allClasses = analyzeSource(inputPath);
        while (true) {
            System.out.println("\nMenu principal:");
            System.out.println("1. Statistiques");
            System.out.println("2. Graphe d'appels");
            System.out.println("3. Quitter");
            System.out.print("Choisissez une option: ");
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    statisticsMenu(scanner, allClasses);
                    break;
                case "2":
                    while (true) {
                        System.out.println("1. Affichez le graphe d'appels dans la console");
                        System.out.println("2. Affichez le graphe d'appels sur navigateur web");
                        System.out.println("0. Retour au menu principal");
                        System.out.print("Choisissez une option: ");
                        String graphChoice = scanner.nextLine();
                        switch (graphChoice) {
                            case "1":
                                CallGraphBuilder.displayCallGraph(allClasses);
                                break;
                            case "2":
                                CallGraphBuilder.generateHtmlGraph(allClasses, "callgraph.html");
                                break;
                            case "0":
                                break;
                            default:
                                System.out.println("Option invalide.");
                                continue;
                        }
                        if (graphChoice.equals("0")) {
                            break;
                        }                        
                    }
                    break;
                    
                case "3":
                    System.out.println("Au revoir!");
                    scanner.close();
                    return;
                default:
                    System.out.println("Option invalide.");
            }
        }
    }

    /**
     * Analyzes Java source files at the given path and returns a list of ClassInfo objects.
     * @param inputPath Path to a Java file or directory
     * @return List of ClassInfo representing all classes found
     * @throws IOException if file reading fails
     */
    private static List<ClassInfo> analyzeSource(Path inputPath) throws IOException {
        Stream<Path> files;
        if (Files.isDirectory(inputPath)) {
            files = Files.walk(inputPath).filter(p -> p.toString().endsWith(".java"));
        } else {
            files = Stream.of(inputPath);
        }
        List<ClassInfo> allClasses = new ArrayList<>();
        files.forEach(p -> {
            try {
                String source = Files.readString(p);
                ASTParser parser = ASTParser.newParser(AST.JLS11);
                parser.setKind(ASTParser.K_COMPILATION_UNIT);
                parser.setSource(source.toCharArray());
                parser.setResolveBindings(false);
                CompilationUnit cu = (CompilationUnit) parser.createAST(null);
                List<ClassInfo> classes = new ArrayList<>();
                cu.accept(new ASTVisitor() {
                    ClassInfo currentClass;
                    MethodInfo currentMethod;
                    @Override
                    public boolean visit(TypeDeclaration node) {
                        if (!node.isInterface()) {
                            currentClass = new ClassInfo();
                            currentClass.name = node.getName().getIdentifier();
                            PackageDeclaration pd = cu.getPackage();
                            currentClass.packageName = pd != null ? pd.getName().getFullyQualifiedName() : "";
                            classes.add(currentClass);
                            currentClass.nbAttributes = node.getFields().length;
                        }
                        return super.visit(node);
                    }
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        if (currentClass != null) {
                            currentMethod = new MethodInfo();
                            currentMethod.name = node.getName().getIdentifier();
                            currentMethod.nbParameters = node.parameters().size();
                            int start = cu.getLineNumber(node.getStartPosition());
                            int end = cu.getLineNumber(node.getStartPosition()) + node.getLength();
                            currentMethod.nbLines = end - start + 1;
                            currentClass.methods.add(currentMethod);
                            currentClass.nbMethods++;
                        }
                        return super.visit(node);
                    }
                });
                cu.accept(new ASTVisitor() {
                    ClassInfo currentClass;
                    MethodInfo currentMethod;
                    int classIndex = 0;
                    @Override
                    public boolean visit(TypeDeclaration node) {
                        if (!node.isInterface()) {
                            currentClass = classes.get(classIndex++);
                        }
                        return super.visit(node);
                    }
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        if (currentClass != null) {
                            String methodName = node.getName().getIdentifier();
                            int paramCount = node.parameters().size();
                            for (MethodInfo m : currentClass.methods) {
                                if (m.name.equals(methodName) && m.nbParameters == paramCount) {
                                    currentMethod = m;
                                    break;
                                }
                            }
                        }
                        return super.visit(node);
                    }
                    @Override
                    public void endVisit(MethodDeclaration node) {
                        currentMethod = null;
                    }
                    @Override
                    public boolean visit(MethodInvocation node) {
                        if (currentMethod != null && currentClass != null) {
                            String calledMethodName = node.getName().getIdentifier();
                            for (MethodInfo m : currentClass.methods) {
                                if (m.name.equals(calledMethodName)) {
                                    currentMethod.calls.add(m);
                                    break;
                                }
                            }
                        }
                        return super.visit(node);
                    }
                });
                allClasses.addAll(classes);
            } catch(IOException e) {
                e.printStackTrace();
            }
        });
        return allClasses;
    }

    /**
     * Interactive statistics menu.
     * <p>
     * Presents all available statistics, including:
     * <ul>
     *   <li>Class, method, line, package, attribute counts</li>
     *   <li>Averages and top 10% lists</li>
     *   <li>Intersection of top classes</li>
     *   <li>Classes with more than X methods (user input)</li>
     *   <li>Top methods by lines per class</li>
     *   <li>Maximum parameters in any method</li>
     * </ul>
     * @param scanner Scanner for user input
     * @param classes List of ClassInfo to analyze
     */
    private static void statisticsMenu(Scanner scanner, List<ClassInfo> classes) {
        int totalClasses = classes.size();
        int totalMethods = classes.stream().mapToInt(c -> c.nbMethods).sum();
        int totalLines = classes.stream().flatMap(c -> c.methods.stream()).mapToInt(m -> m.nbLines).sum();
        int totalAttributes = classes.stream().mapToInt(c -> c.nbAttributes).sum();
        long totalPackages = classes.stream().map(c -> c.packageName).distinct().count();

        double avgMethodsPerClass = totalClasses > 0 ? (double) totalMethods / totalClasses : 0;
        double avgLinesPerMethod = totalMethods > 0 ? (double) totalLines / totalMethods : 0;
        double avgAttributesPerClass = totalClasses > 0 ? (double) totalAttributes / totalClasses : 0;

        int topN = Math.max(1, (int) Math.ceil(0.1 * totalClasses));

        List<ClassInfo> topClassesByMethods = classes.stream()
                .sorted((c1, c2) -> Integer.compare(c2.nbMethods, c1.nbMethods))
                .limit(topN)
                .collect(Collectors.toList());

        List<ClassInfo> topClassesByAttributes = classes.stream()
                .sorted((c1, c2) -> Integer.compare(c2.nbAttributes, c1.nbAttributes))
                .limit(topN)
                .collect(Collectors.toList());

        Set<ClassInfo> topBoth = topClassesByMethods.stream()
                .filter(topClassesByAttributes::contains)
                .collect(Collectors.toSet());

        Map<ClassInfo, List<MethodInfo>> topMethodsByLines = classes.stream()
                .collect(Collectors.toMap(
                        c -> c,
                        c -> c.methods.stream()
                                .sorted((m1, m2) -> Integer.compare(m2.nbLines, m1.nbLines))
                                .limit(topN)
                                .collect(Collectors.toList())
                ));

        int maxParams = classes.stream()
                .flatMap(c -> c.methods.stream())
                .mapToInt(m -> m.nbParameters)
                .max()
                .orElse(0);
        
        while (true) {
            System.out.println("1. Affichez les statistiques sur la console");
            System.out.println("2. Affichez les statistiques sur une interface graphique");
            System.out.println("0. Retour au menu principal");
            System.out.print("Choisissez une option: ");
            String formatChoice = scanner.nextLine();

            switch (formatChoice) {
                case "1":
                    while (true) {
                        System.out.println("\nStatistiques disponibles:");
                        System.out.println("1. Nombre de classes");
                        System.out.println("2. Nombre total de méthodes");
                        System.out.println("3. Nombre total de lignes de code");
                        System.out.println("4. Nombre total de packages");
                        System.out.println("5. Nombre moyen de méthodes par classe");
                        System.out.println("6. Nombre moyen de lignes par méthode");
                        System.out.println("7. Nombre moyen d'attributs par classe");
                        System.out.println("8. Top 10% des classes par nombre de méthodes");
                        System.out.println("9. Top 10% des classes par nombre d'attributs");
                        System.out.println("10. Intersection des deux catégories précédentes");
                        System.out.println("11. Classes avec plus de X méthodes");
                        System.out.println("12. Top 10% des méthodes par nombre de lignes (par classe)");
                        System.out.println("13. Nombre maximal de paramètres dans toutes les méthodes");
                        System.out.println("0. Retour");
                        System.out.print("Choisissez une option: ");
                        String statChoice = scanner.nextLine();

                        switch (statChoice) {
                            case "1":
                                System.out.println("Nombre de classes : " + totalClasses);
                                break;
                            case "2":
                                System.out.println("Nombre total de méthodes : " + totalMethods);
                                break;
                            case "3":
                                System.out.println("Nombre total de lignes de code : " + totalLines);
                                break;
                            case "4":
                                System.out.println("Nombre total de packages : " + totalPackages);
                                break;
                            case "5":
                                System.out.printf("Nombre moyen de méthodes par classe : %.2f\n", avgMethodsPerClass);
                                break;
                            case "6":
                                System.out.printf("Nombre moyen de lignes par méthode : %.2f\n", avgLinesPerMethod);
                                break;
                            case "7":
                                System.out.printf("Nombre moyen d'attributs par classe : %.2f\n", avgAttributesPerClass);
                                break;
                            case "8":
                                System.out.println("Top 10% des classes par nombre de méthodes : " +
                                        topClassesByMethods.stream().map(c -> c.name).collect(Collectors.toList()));
                                break;
                            case "9":
                                System.out.println("Top 10% des classes par nombre d'attributs : " +
                                        topClassesByAttributes.stream().map(c -> c.name).collect(Collectors.toList()));
                                break;
                            case "10":
                                System.out.println("Classes dans les deux catégories : " +
                                        topBoth.stream().map(c -> c.name).collect(Collectors.toList()));
                                break;
                            case "11":
                                System.out.print("Entrez la valeur de X: ");
                                try {
                                    int X = Integer.parseInt(scanner.nextLine());
                                    List<ClassInfo> moreThanX = classes.stream()
                                            .filter(c -> c.nbMethods > X)
                                            .collect(Collectors.toList());
                                    System.out.println("Classes avec plus de " + X + " méthodes : " +
                                            moreThanX.stream().map(c -> c.name).collect(Collectors.toList()));
                                } catch (NumberFormatException e) {
                                    System.out.println("Valeur invalide.");
                                }
                                break;
                            case "12":
                                for (ClassInfo c : topMethodsByLines.keySet()) {
                                    System.out.println("Top 10% méthodes de " + c.name + ": " +
                                            topMethodsByLines.get(c).stream().map(m -> m.name).collect(Collectors.toList()));
                                }
                                break;
                            case "13":
                                System.out.println("Nombre maximal de paramètres dans toutes les méthodes : " + maxParams);
                                break;
                            case "0":
                                break;
                            default:
                                System.out.println("Option invalide.");
                        }

                        if (statChoice.equals("0")) {
                            break;
                        }
                    }
                    break; 

                case "2":
                    AppStatsGUI.displayStats(
                            totalClasses,
                            totalMethods,
                            totalLines,
                            totalPackages,
                            avgMethodsPerClass,
                            avgLinesPerMethod,
                            avgAttributesPerClass,
                            topClassesByMethods,
                            topClassesByAttributes,
                            topBoth,
                            classes,
                            topMethodsByLines,
                            maxParams
                    );
                    break;

                case "0":
                    return; 

                default:
                    System.out.println("Option invalide.");
            }

            if (formatChoice.equals("0")) {
                break;
            }
        }
    }
}
