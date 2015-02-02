package org.myrobotlab.framework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * Bootstrap's responsibilities include spawning all new processes. This
 * includes : - preparing the environment - environment variables - jvm
 * arguments - valid cmd line parameters - relayed stdout & stderr - saving
 * references to stdin
 *
 * References :
 *
 * http://www.excelsior-usa.com/articles/java-to-exe.html
 *
 * possible small wrappers mac / linux / windows http://mypomodoro.googlecode
 * .com/svn-history/r89/trunk/src/main/java/org /mypomodoro/util/Restart.java
 *
 * http://java.dzone.com/articles/programmatically-restart-java
 * http://stackoverflow
 * .com/questions/3468987/executing-another-application-from-java
 *
 *
 * TODO - ARMV 6 7 8 ??? - http://www.binarytides.com/linux-cpu-information/ -
 * lscpu
 *
 * Architecture: armv7l Byte Order: Little Endian CPU(s): 4 On-line CPU(s) list:
 * 0-3 Thread(s) per core: 1 Core(s) per socket: 1 Socket(s): 4
 *
 *
 * TODO - soft floating point vs hard floating point readelf -A /proc/self/exe |
 * grep Tag_ABI_VFP_args soft = nothing hard = Tag_ABI_VFP_args: VFP registers
 *
 * PACKAGING jsmooth - windows only javafx - 1.76u - more dependencies ?
 * http://stackoverflow.com/questions/1967549/java-packaging-tools-
 * alternatives-for-jsmooth-launch4j-onejar
 *
 * TODO classpath order - for quick bleeding edge updates? rsync exploded
 * classpath
 *
 * TODO - check for Java 1.7 or > addShutdownHook check for network connectivity
 * TODO - proxy -Dhttp.proxyHost=webproxy -Dhttp.proxyPort=80
 * -Dhttps.proxyHost=webproxy -Dhttps.proxyPort=80
 * -Dhttp.proxyUserName="myusername" -Dhttp.proxyPassword="mypassword"
 * 
 * TODO? how to get vm args http:*
 * stackoverflow.com/questions/1490869/how-to-get
 * -vm-arguments-from-inside-of-java-application http:*
 * java.dzone.com/articles/programmatically-restart-java http:*
 * stackoverflow.com
 * /questions/9911686/getresource-some-jar-returns-null-although
 * -some-jar-exists-in-geturls RuntimeMXBean runtimeMxBean =
 * ManagementFactory.getRuntimeMXBean(); List<String> arguments =
 * runtimeMxBean.getInputArguments();
 * 
 *
 */

public class Bootstrap {
	static int BUFFER_SIZE = 2048;
	public final static Logger log = LoggerFactory.getLogger(Bootstrap.class);

	static public List<String> getJVMArgs() {
		RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
		return runtimeMxBean.getInputArguments();
	}

	/**
	 * Responsibility - This method will always call Runtime. To start Runtime
	 * correctly environment must correctly be setup
	 */
	static public synchronized Process spawn(String[] in) throws IOException, URISyntaxException, InterruptedException {
		log.info("============== spawn begin ==============");
		// step 1 - get current env data
		String ps = File.pathSeparator;
		String fs = System.getProperty("file.separator");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd:HH:mm:ss");
		log.info(String.format("\n\nBootstrap starting spawn %s", formatter.format(new Date())));

		log.info("Bootstrap PID {}", Runtime.getPID());
		List<String> jvmArgs = getJVMArgs();
		log.info("jvmArgs {}", Arrays.toString(jvmArgs.toArray()));
		log.info("in args {}", Arrays.toString(in));

		// FIXME - details on space / %20 decoding in URI
		// http://stackoverflow.com/questions/320542/how-to-get-the-path-of-a-running-jar-file
		String protectedDomain = URLDecoder.decode(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath(), "UTF-8");
		log.info("protected domain {}", protectedDomain);

		// platform id
		Platform platform = Platform.getLocalInstance();
		String platformId = platform.getPlatformId();
		log.info("platform {}", platformId);

		ArrayList<String> outArgs = new ArrayList<String>();
		String classpath = String.format("./%s./myrobotlab.jar%s./libraries/jar/*", ps, ps);

		String javaExe = platform.isWindows() ? "javaw" : "java";

		String javaPath = System.getProperty("java.home") + fs + "bin" + fs + javaExe;
		// JNI
		String jniLibraryPath = String.format("-Djava.library.path=libraries/native%slibraries/native/%s", ps, platformId);

		// String jvmMemory = "-Xmx2048m -Xms256m";
		Integer totalMemory = getTotalPhysicalMemory();
		if (totalMemory == null) {
			log.info("could not get total physical memory");
		} else {
			log.info("total physical memory returned is {} Mb", totalMemory);
		}

		outArgs.add(javaPath);
		outArgs.add(jniLibraryPath);
		outArgs.add("-cp");
		outArgs.add(classpath);

		boolean hasService = false;
		for (int i = 0; i < in.length; ++i) {
			String arg = in[i];
			if (arg.startsWith("org.myrobotlab.service")) {
				hasService = true;
			}
		}

		if (!hasService) {
			outArgs.add("org.myrobotlab.service.Runtime");
		}

		// TODO preserve/serialize command line parameters
		if (in.length > 0) {
			for (int i = 0; i < in.length; ++i) {
				outArgs.add(in[i]);
			}
		} else {
			// (default) - no parameters supplied

			outArgs.add("-service");
			outArgs.add("gui");
			outArgs.add("GUIService");
			outArgs.add("python");
			outArgs.add("Python");
		}

		// ProcessBuilder builder = new ProcessBuilder(path, "-Xmx1024m", "-cp",
		// classpath, ReSpawner.class.getName());

		File update = new File("./update/myrobotlab.jar");

		if (update.exists()) {
			// attempt to process the update
			log.info("update exists archiving current");

			try {
				// if thrown - "file locked" then createBootstrapJar
				// IF THAT THROWS - GIVE UP !!!

				// update available - archive old file
				// Path source = Paths.get("./myrobotlab.jar");
				File archiveDir = new File("./archive");
				archiveDir.mkdirs();

				File source = new File("./myrobotlab.jar");
				File target = new File(String.format("./archive/myrobotlab.%s.jar", Runtime.getVersion()));
				copy(source, target);

				// copy update
				log.info("moving update");
				source = new File("./update/myrobotlab.jar");
				target = new File("./myrobotlab.jar");
				copy(source, target);
				log.info("deleting update");
				if (!source.delete()) {
					log.error("could not delete update");
				}
				log.info("completed update !");
			} catch (Exception e) {
				try {
					// FIXME FIXME - normalize the start !!!!
					log.info("file myrobotlab.jar is locked - ejecting bootstrap.jar - {}", e.getMessage());

					File source = new File("./myrobotlab.jar");
					File target = new File("./bootstrap.jar");
					copy(source, target);

					ArrayList<String> bootArgs = new ArrayList<String>();

					bootArgs.add(javaPath);
					bootArgs.add("-jar");
					bootArgs.add("./bootstrap.jar"); // -jar uses manifest
					// bootArgs.add("org.myrobotlab.framework.Bootstrap");
					for (int i = 0; i < in.length; ++i) {
						bootArgs.add(in[i]);
					}
					String cmd = formatList(bootArgs);
					log.info(String.format("bootstrap.jar spawning -> [%s]", cmd));
					ProcessBuilder builder = new ProcessBuilder(bootArgs);
					Process process = builder.start();

					log.info(String.format("terminating - good luck new bootstrap & update :)"));
					System.exit(0);
					return process;
				} catch (Exception ex) {
					log.error("PANIC - failed to create bootstrap - terminating - bye :(");
					log.error(ex.getMessage());
				}
			}
		}

		String cmd = formatList(outArgs);
		log.info(String.format("spawning -> [%s]", cmd));

		ProcessBuilder builder = new ProcessBuilder(outArgs);// .inheritIO();

		// environment variables setup
		Map<String, String> env = builder.environment();
		if (platform.isLinux()) {
			String ldPath = String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${LD_LIBRARY_PATH}", platformId);
			env.put("LD_LIBRARY_PATH", ldPath);
		} else if (platform.isMac()) {
			String dyPath = String.format("'pwd'/libraries/native:'pwd'/libraries/native/%s:${DYLD_LIBRARY_PATH}", platformId);
			env.put("DYLD_LIBRARY_PATH", dyPath);
		} else if (platform.isWindows()) {
			String path = String.format("PATH=%%CD%%\\libraries\\native;PATH=%%CD%%\\libraries\\native\\%s;%%PATH%%", platformId);
			env.put("PATH", path);
		} else {
			log.error("unkown operating system");
		}

		Process process = builder.start();

		/* Decision to exist or exit - relay streams or terminate */
		StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), "ERROR");
		errorGobbler.start();

		StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), "OUTPUT");
		outputGobbler.start();
		process.waitFor();
		
		log.info("Bootstrap finished spawn {}", formatter.format(new Date()));
		return process;
	}

	static public String formatList(ArrayList<String> args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); ++i) {
			log.info(args.get(i));
			sb.append(String.format("%s ", args.get(i)));
		}
		return sb.toString();
	}

	static public Process spawn(List<String> args) throws IOException, URISyntaxException, InterruptedException {
		return spawn(args.toArray(new String[args.size()]));
	}

	static public Integer getTotalPhysicalMemory() {
		try {

			com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
			Integer physicalMemorySize = (int) (os.getTotalPhysicalMemorySize() / 1048576);

			return physicalMemorySize;
		} catch (Exception e) {
			log.error("getTotalPhysicalMemory - threw");
		}
		return null;
	}

	static public void copy(File src, File dst) throws IOException {
		System.out.println(String.format("copy %s to %s", src, dst));
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	/**
	 * First method JVM executes when myrobotlab.jar is in jar form.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			System.out.println("Bootstrap.main starting bootstrap");
			Logging logging = LoggingFactory.getInstance();
			// TODO send file name of logger depending on context
			//logging.addAppender(Appender.FILE);
			logging.addAppender(Appender.CONSOLE);
			Bootstrap.spawn(args);
			System.out.println("leaving bootstrap");
		} catch (Exception e) {
			e.printStackTrace(System.out);
		} finally {
			// big hammer
			log.info("Bootstrap.main exiting");
			//System.exit(0);
		}
	}

}