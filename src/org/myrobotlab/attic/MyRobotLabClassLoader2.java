package org.myrobotlab.attic;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Hashtable;

import org.myrobotlab.cmdline.CMDLine;
import org.myrobotlab.logging.LoggingFactory;

// http://nucleussystems.com/blog/tag/urlclassloader

/**
 * 
 * Complete PITA ! either http://classworlds.codehaus.org/launchusage.html or
 * http://nucleussystems.com/blog/tag/urlclassloader
 * 
 * allows dynamic loading and re-loading from class path.
 * 
 * scan full directory create (absoulte) URL array add myrobotlab.jar
 * 
 * I have pulled much hair out do to the chasing down at why this was not
 * working The default behavior of the ClassLoader and most derivatives are in
 * this order : 1. get parent to attempt to resolve class 2. resolve it your
 * self In trying the "simplest" solution of constructing a new
 * URLClassLoader(new URL[]{url}, Runtime.class.getClassLoader()); the "parent"
 * classloader WILL RESOLVE the service request of classForName() HOWEVER there
 * are implicit dependencies like gnu.io.IOPortListener which can only be
 * resolved in the URLClassLoader I "think" I can get around this by building up
 * all the URLs for a new URLClassLoader
 * 
 * if the Service is downloaded we have to dynamically load the classes - if we
 * are not going to restart
 * http:tutorials.jenkov.com/java-reflection/dynamic-class
 * -loading-reloading.html
 * 
 * ("file:./libraries/jar/RXTXcomm.jar") or ("file:libraries/jar/RXTXcomm.jar")
 * are appropriate ClassLoader parent = Runtime.class.getClassLoader();
 * URLClassLoader loader = (URLClassLoader)ClassLoader.getSystemClassLoader();
 * 
 * Check out network classloaders
 * 
 * System.out.println("classloader urls count is " +
 * classLoader.getURLs().length); for (int z = 0; z <
 * classLoader.getURLs().length; ++z) {
 * System.out.println(classLoader.getURLs()[z]); }
 * 
 * 
 * @author greg (at) myrobotlab.org
 * 
 *         The purpose of this custom ClassLoader is to support the checking and
 *         dynamic loading of new Services. A modified Ivy.jar will be used to
 *         check dependencies (and future updates?) If the dependencies have not
 *         been loaded it will download them from the code.google.com repo After
 *         the appropriate dependencies have been loaded, this custom
 *         ClassLoader will scan the libraries/jar for any new files. It will
 *         use addURL or addFile to load references and change the classpath
 *         during runtime. This should allow a user to select a new Service,
 *         download it and run it without having to restart.
 * 
 *         Not interested in supporting -Djava.system.class.loader because I'm
 *         supporting only one custom ClassLoader. So you don't have a choice :)
 *         and I would like to make the command line as simple as possible.
 * 
 *         If you see dependencies being blown out (third party jars not
 *         resolving) a parent ClassLoader probably loaded the class and
 *         explicit references are "NOT" being loaded by the parents
 * 
 */

public class MyRobotLabClassLoader2 extends URLClassLoader {

	// public final static Logger log =
	// LoggerFactory.getLogger(MyRobotLabClassLoader.class.toString());
	ClassLoader parent;
	private static MyRobotLabClassLoader2 instance;

	/**
	 * because it is a List of URLs in URLClassLoader, was not sure if they were
	 * to be unique, or the consequences if they were not. references maintains
	 * the uniqueness of the urls
	 */
	private static Hashtable<String, URL> references;
	/**
	 * cache of Classes loaded
	 */
	private static Hashtable<String, URL> cache;

	/**
	 * private constructor to guarantee singleton
	 * 
	 * @param urls
	 *            - list of urls to append to the classpath
	 * @param parent
	 *            - parent classloader
	 */
	private MyRobotLabClassLoader2(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public static synchronized MyRobotLabClassLoader2 getInstance(ClassLoader parent) {
		if (null == instance) {
			System.out.println("constructing singleton MyRobotLabClassLoader");
			references = new Hashtable<String, URL>();
			URL[] urls = getFileSystemURLs();
			System.out.println("getFileSystemURLs found " + urls.length + " resources");

			for (int i = 0; i < urls.length; ++i) {
				references.put(urls[i].toString(), urls[i]);
				System.out.println("adding resource " + urls[i].toString());
			}
			instance = new MyRobotLabClassLoader2(urls, parent);
		}
		return instance;
	}

	public static URL[] getFileSystemURLs() {
		System.out.println("getFileSystemURLs - scanning libraries/jar directory");
		URL[] jars = null;
		try {
			// String path =
			// "C:/Documents and Settings/grperry/mrl2/myrobotlab/dist/218.232M.20111124.0700/";
			String path = "";
			// scan directory
			File librariesDir = new File(path + "libraries/jar");
			File[] libraries = librariesDir.listFiles();
			jars = new URL[(int) ((libraries.length) + 1)];
			for (int x = 0; x < libraries.length; ++x) {
				jars[x] = libraries[x].toURI().toURL();
			}

			// don't forget myrobotlab.jar !
			jars[libraries.length] = (new File(path + "myrobotlab.jar")).toURI().toURL();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return jars;
	}

	public void addURL(URL url) {
		// expose protected URLClassLoader method
		// bootload now with
		// -Djava.system.class.loader=org.myrobotlab.bootloader.MyRobotLabClassLoader
		if (references.contains(url.toString())) {
			System.out.println(url.toString() + " already references");
			return;
		}
		references.put(url.toString(), url);
		System.out.println("added reference " + url.toString());
		super.addURL(url);
	}

	public static String classLoaderTreeString(Object o) {

		StringBuffer sb = new StringBuffer("\nclass " + o.getClass().getCanonicalName());

		ClassLoader cl = o.getClass().getClassLoader();
		while (cl != null) {
			sb.append("has loader of type " + cl.getClass().getCanonicalName() + "\n");
			cl = cl.getParent();
		}

		sb.append("\n");

		cl = ClassLoader.getSystemClassLoader();
		sb.append("system class loader ");
		while (cl != null) {
			sb.append("has loader of type " + cl.getClass().getCanonicalName() + "\n");
			cl = cl.getParent();
		}

		sb.append("\n");
		cl = Thread.currentThread().getContextClassLoader();
		sb.append("current thread ");
		while (cl != null) {
			sb.append("has loader of type " + cl.getClass().getCanonicalName() + "\n");
			cl = cl.getParent();
		}

		return sb.toString();
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();

			// setting the context to a new custom class loader
			System.out.println("setting classloader to singleton instance of MyRobotLabClassLoader");
			// Thread.currentThread().setContextClassLoader(MyRobotLabClassLoader.getInstance());

			System.out.println(classLoaderTreeString(new Object()));

			CMDLine cmdline = new CMDLine();
			cmdline.splitLine(args);

			if (cmdline.containsKey("-start")) {
				String startClass = cmdline.getSafeArgument("-start", 0, "");
				System.out.println("invoking " + startClass + ".main(String[] args)");
				MyRobotLabClassLoader2 loader = MyRobotLabClassLoader2.getInstance(ClassLoader.getSystemClassLoader());
				Thread.currentThread().setContextClassLoader(loader);
				// Class<?> cl = Class.forName(startClass);
				Class<?> cl = loader.loadClass(startClass);
				Method main = cl.getDeclaredMethod("main", String[].class);
				Object[] p = { args };
				main.invoke(cl, p);
			} else {
				System.out.println("incorrect parameters");
			}

			/*
			 * l.addURL(new URL("file:C:/Temp/xyz.jar")); Class c =
			 * l.loadClass("package.classname");
			 * System.out.println(c.getName());
			 */

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
