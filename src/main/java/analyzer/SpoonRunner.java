package analyzer;

import analyzer.Utils.ClassInfo;
import analyzer.Utils.MethodInfo;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtType;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.visitor.filter.TypeFilter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utility to run analysis using Spoon instead of the JDT-based parser.
 */
public class SpoonRunner {

    /**
     * Build ClassInfo structures from source using Spoon.
     */
    public static List<ClassInfo> buildClassesFromSpoon(Path inputPath) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);
        launcher.addInputResource(inputPath.toString());
        launcher.buildModel();
        CtModel model = launcher.getModel();

        List<ClassInfo> classes = new ArrayList<>();

        for (CtType<?> ctType : model.getAllTypes()) {
            // Only consider classes (exclude interfaces, enums, annotations)
            if (!ctType.isClass()) continue;

            ClassInfo classInfo = new ClassInfo();
            classInfo.name = ctType.getSimpleName();
            try {
                classInfo.packageName = ctType.getPackage().getQualifiedName();
            } catch (Exception e) {
                classInfo.packageName = "";
            }

            Set<CtMethod<?>> methods = ctType.getMethods();
            classInfo.nbAttributes = 0; // attribute counting can be extended if needed
            classInfo.nbMethods = methods.size();

            for (CtMethod<?> ctMethod : methods) {
                MethodInfo mi = new MethodInfo();
                mi.name = ctMethod.getSimpleName();
                mi.nbParameters = ctMethod.getParameters().size();
                mi.classOwner = classInfo.packageName + "." + classInfo.name;

                // Best-effort number of lines
                try {
                    int start = ctMethod.getPosition().getLine();
                    int end = ctMethod.getPosition().getEndLine();
                    if (start > 0 && end >= start) mi.nbLines = end - start + 1;
                } catch (Exception e) {
                    mi.nbLines = 0;
                }

                // Collect method invocations as call signatures
                List<CtInvocation<?>> invocations = ctMethod.getElements(new TypeFilter<>(CtInvocation.class));
                for (CtInvocation<?> inv : invocations) {
                    String calledName = inv.getExecutable().getSimpleName();
                    int args = inv.getArguments() == null ? 0 : inv.getArguments().size();
                    String callSig = calledName + ":" + args;
                    mi.callSignatures.add(callSig);
                }

                classInfo.methods.add(mi);
            }

            classes.add(classInfo);
        }

        return classes;
    }

    /**
     * Run the full coupling + hierarchical module identification pipeline using Spoon.
     */
    public static void runSpoonAnalysis(Path inputPath, double cp) {
        try {
            List<ClassInfo> classes = buildClassesFromSpoon(inputPath);
            ClassCouplingAnalyzer cca = new ClassCouplingAnalyzer(classes);
            cca.generateHtmlGraph("coupling_graph.html");

            HierarchicalClusteringAnalyzer hc = new HierarchicalClusteringAnalyzer(classes, cca.getNormalizedCouplings());
            hc.runClusteringAndIdentifyModules(cp);
            hc.generateHtmlModules("modules.html");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}