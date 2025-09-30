package analyzer;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

public class Analyzer {

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			System.out.println("Usage: java Analyzer <chemin-fichier-ou-dossier>");
			return;
		}
		
		Path inputPath = Paths.get(args[0]);
		
		Stream<Path> files;
		if (Files.isDirectory(inputPath)) {
            files = Files.walk(inputPath).filter(p -> p.toString().endsWith(".java"));
        } else if (Files.isRegularFile(inputPath) && inputPath.toString().endsWith(".java")) {
            files = Stream.of(inputPath);
        } else {
            System.out.println("Chemin invalide ou pas un .java: " + inputPath);
            return;
        }
		
		List<ClassInfo> allClasses = new ArrayList<>();
		
		files.forEach(p -> {
			try {
				System.out.println("---- Analyser fichier: " + p + " ----");
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
                		
            			System.out.println("Classe: " + node.getName().getIdentifier());
            			allClasses.addAll(classes);
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
                		
                		System.out.println("   Méthode: " + node.getName().getIdentifier() + " (params=" + node.parameters().size() + ")");
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
                            // Find the corresponding method in our collected methods
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
                                    System.out.println("  Appel: " + currentMethod.name + " -> " + calledMethodName);
                                    break;
                                }
                            }
                        }
                        return super.visit(node);
                    }
                });
                            
                // 1) Nombre de classes de l’application
                int totalClasses = classes.size();
                System.out.println("1) Nombre de classes : " + totalClasses);
                
                // 3) Nombre de méthodes de l’application
                int totalMethods = classes.stream().mapToInt(c -> c.nbMethods).sum();
                System.out.println("3) Nombre total de méthodes : " + totalMethods);
                
                // 2) Nombre de lignes de codes de l'application
                int totalLines = totalMethods > 0
                        ? classes.stream()
	                		.flatMap(c -> c.methods.stream())
	                		.mapToInt(m -> m.nbLines).sum()
                        : 0; 
                System.out.println("2) Nombre total de lignes de code : " + totalLines);
                
                // 4) Nombre total de packages de l’application
                long totalPackages = classes.stream().map(c -> c.packageName).distinct().count();
                System.out.println("4) Nombre total de packages : " + totalPackages);
                
                // 5) Nombre moyen de méthodes par classe
                double avgMethodsPerClass = totalClasses > 0 ? (double) totalMethods / totalClasses : 0;
                System.out.printf("5) Nombre moyen de méthodes par classe : %.2f%n", avgMethodsPerClass);
                
                // 6) Nombre moyen de lignes de code par méthodes
                double avgLinesPerMethod = totalMethods > 0
                        ? totalLines / totalMethods
                        : 0;
                System.out.printf("6) Nombre moyen de lignes par méthode : %.2f%n", avgLinesPerMethod);
                
                // 7) Nombre moyen d’attributs par classe
                int totalAttributes = classes.stream().mapToInt(c -> c.nbAttributes).sum();
                double avgAttributesPerClass = totalClasses > 0 ? (double) totalAttributes / totalClasses : 0;
                System.out.printf("7) Nombre moyen d’attributs par classe : %.2f%n", avgAttributesPerClass);
                
                // 8) Les 10% des classes qui possèdent le plus grand nombre de méthodes
                int topN = Math.max(1, (int)Math.ceil(0.1 * totalClasses));
                List<ClassInfo> topByMethods = classes.stream()
                        .sorted((c1, c2) -> Integer.compare(c2.nbMethods, c1.nbMethods))
                        .limit(topN)
                        .collect(Collectors.toList());
                System.out.println("8) Top 10% des classes par nombre de méthodes : " +
                        topByMethods.stream().map(c -> c.name).collect(Collectors.toList()));
                
                // 9) Les 10% des classes qui possèdent le plus grand nombre d'attributs
                List<ClassInfo> topByAttributes = classes.stream()
                        .sorted((c1, c2) -> Integer.compare(c2.nbAttributes, c1.nbAttributes))
                        .limit(topN)
                        .collect(Collectors.toList());
                System.out.println("9) Top 10% des classes par nombre d'attributs : " +
                        topByAttributes.stream().map(c -> c.name).collect(Collectors.toList()));
                
                // 10) Les classes qui font partie en même temps des deux catégories précédentes
                Set<String> topBoth = new HashSet<>();
                for (ClassInfo c : topByMethods) {
                    if (topByAttributes.contains(c)) topBoth.add(c.name);
                }
                System.out.println("10) Classes dans les deux catégories : " + topBoth);
                
                // 11) Les classes qui possèdent plus de X méthodes (la valeur de X est donnée)
                int X = 5; 
                List<ClassInfo> moreThanX = classes.stream().filter(c -> c.nbMethods > X).collect(Collectors.toList());
                System.out.println("11) Classes avec plus de " + X + " méthodes : " +
                        moreThanX.stream().map(c -> c.name).collect(Collectors.toList()));

                // 12) Les 10% des méthodes qui possèdent le plus grand nombre de lignes de code (par classe)
                for (ClassInfo c : classes) {
                    int topM = Math.max(1, (int)Math.ceil(0.1 * c.methods.size()));
                    List<MethodInfo> topMethods = c.methods.stream()
                            .sorted((m1, m2) -> Integer.compare(m2.nbLines, m1.nbLines))
                            .limit(topM)
                            .collect(Collectors.toList());
                    System.out.println("Top 10% méthodes de " + c.name + ": " + topMethods.stream().map(m -> m.name).collect(Collectors.toList()));
                }

                
                // 13) Le nombre maximal de paramètres par rapport à toutes les méthodes de l’application
                int maxParams = classes.stream()
                        .flatMap(c -> c.methods.stream())
                        .mapToInt(m -> m.nbParameters)
                        .max()
                        .orElse(0);
                System.out.println("13) Nombre maximal de paramètres dans toutes les méthodes : " + maxParams);
                
                AppStatsGUI.displayStats(
                        totalClasses,
                        totalMethods,
                        totalLines,
                        totalPackages,
                        avgMethodsPerClass,
                        avgLinesPerMethod,
                        avgAttributesPerClass,
                        topByMethods.stream().map(c -> c.name).collect(Collectors.toList()),
                        topByAttributes.stream().map(c -> c.name).collect(Collectors.toList()),
                        topBoth,
                        moreThanX.stream().map(c -> c.name).collect(Collectors.toList()),
                        maxParams
                );
                             
				
			} catch(IOException e) {
				e.printStackTrace();
			}
		});
		
		CallGraphBuilder.displayCallGraph(allClasses);
        CallGraphBuilder.generateDotGraph(allClasses, "callgraph.dot");
        CallGraphBuilder.generateHtmlGraph(allClasses, "callgraph.html");
		
		;
	}
}