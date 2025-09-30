package analyzer;

import java.util.*;

public class Utils {
	
	static class ClassInfo {
	    String name;
	    String packageName;
	    int nbAttributes = 0;
	    int nbMethods = 0;
	    List<MethodInfo> methods = new ArrayList<>();
	}

	static class MethodInfo {
	    String name;
	    int nbParameters = 0;
	    int nbLines = 0;
	    List<MethodInfo> calls = new ArrayList<>();
		int graphId;
	}
	
}