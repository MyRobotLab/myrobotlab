package org.myrobotlab.java;

import com.strobel.decompiler.Decompiler;
import com.strobel.decompiler.PlainTextOutput;

public class DynaComp {

	// static JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
	// static ClassFileManager fileManager = new ClassFileManager(
	// compiler.getStandardFileManager(null, null, null));
//	static DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

	public static Decompiler decompiler = new Decompiler();

	// static{
	// decompiler.setClassPath(System.getProperty("java.class.path"));
	// }

	public static String decompile(Object o) {
		String className=o.getClass().getName();
//		className = className.replaceAll("\\\\", ".");
//		className = className.replaceAll("/", ".");
		className =className.replaceAll("\\.","/");
		className =className.replaceAll("\\\\","/");
//		System.out.println("---------------" + className);
		PlainTextOutput it = new PlainTextOutput();
		Decompiler.decompile(className,it);// (o.getClass().getCanonicalName(),
											// sw, null);
		return it.toString();
	}

	// public static byte[] compile(String fullName, String src) {
	// List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();
	// jfiles.add(new CharSequenceJavaFileObject(fullName, src));
	// boolean success=compiler.getTask(null, fileManager, diagnostics, null,
	// null, jfiles).call();
	// return success?fileManager.jclassObject.bos.toByteArray():null;
	// }

	public static void main(String[] args) {
		System.out.println(DynaComp.decompile("java/lang/String"));
	}

}
