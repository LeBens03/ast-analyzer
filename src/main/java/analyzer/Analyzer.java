package analyzer;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import analyzer.Utils.ClassInfo;
import analyzer.Utils.MethodInfo;

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
 * It also builds a call graph and analyzes class coupling.
 * <p>
 * Usage: <code>java Analyzer</code> - GUI will prompt for file/folder selection
 * <p>
 * Dependencies: Eclipse JDT Core (ASTParser), AnalyzerGUI for graphical interface
 */
public class Analyzer {
    
    /**
     * Entry point for the Analyzer tool.
     * Launches the graphical interface for interactive analysis.
     * @param args Not used; all input is interactive through GUI
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(AnalyzerGUI::new);
    }

    /**
     * Analyzes Java source files at the given path and returns a list of ClassInfo objects.
     * @param inputPath Path to a Java file or directory
     * @return List of ClassInfo representing all classes found
     * @throws IOException if file reading fails
     */
    public static List<ClassInfo> analyzeSource(Path inputPath) throws IOException {
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
                
                // First pass: collect class and method information
                cu.accept(new ASTVisitor() {
                    ClassInfo currentClass;
                    
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
                            MethodInfo currentMethod = new MethodInfo();
                            currentMethod.name = node.getName().getIdentifier();
                            currentMethod.nbParameters = node.parameters().size();
                            currentMethod.classOwner = currentClass.packageName + "." + currentClass.name;
                            int start = cu.getLineNumber(node.getStartPosition());
                            int end = cu.getLineNumber(node.getStartPosition()) + node.getLength();
                            currentMethod.nbLines = end - start + 1;
                            currentClass.methods.add(currentMethod);
                            currentClass.nbMethods++;
                        }
                        return super.visit(node);
                    }
                });
                
                // Second pass: collect method invocations
                cu.accept(new ASTVisitor() {
                    ClassInfo currentClass;
                    MethodInfo currentMethod;
                    int classIndex = 0;
                    
                    @Override
                    public boolean visit(TypeDeclaration node) {
                        if (!node.isInterface() && classIndex < classes.size()) {
                            currentClass = classes.get(classIndex++);
                        }
                        return super.visit(node);
                    }
                    
                    @Override
                    public boolean visit(MethodDeclaration node) {
                        if (currentClass != null) {
                            String methodName = node.getName().getIdentifier();
                            int paramCount = node.parameters().size();
                            
                            // Find matching method in current class
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
                            int paramCount = node.arguments().size();
                            
                            // Store call signature
                            String callSignature = calledMethodName + ":" + paramCount;
                            currentMethod.callSignatures.add(callSignature);
                            
                            // Also try to find and add to calls list for call graph
                            MethodInfo foundMethod = null;
                            
                            // First try current class
                            for (MethodInfo m : currentClass.methods) {
                                if (m.name.equals(calledMethodName) && m.nbParameters == paramCount) {
                                    foundMethod = m;
                                    break;
                                }
                            }
                            
                            // If not found, try other classes
                            if (foundMethod == null) {
                                for (ClassInfo otherClass : classes) {
                                    if (!otherClass.equals(currentClass)) {
                                        for (MethodInfo m : otherClass.methods) {
                                            if (m.name.equals(calledMethodName) && m.nbParameters == paramCount) {
                                                foundMethod = m;
                                                break;
                                            }
                                        }
                                        if (foundMethod != null) break;
                                    }
                                }
                            }
                            
                            if (foundMethod != null) {
                                currentMethod.calls.add(foundMethod);
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
}