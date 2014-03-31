/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.myrobotlab.framework;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.cache.ResolutionCacheManager;
import org.apache.ivy.core.deliver.DeliverOptions;
import org.apache.ivy.core.module.descriptor.DefaultDependencyDescriptor;
import org.apache.ivy.core.module.descriptor.DefaultModuleDescriptor;
import org.apache.ivy.core.module.descriptor.ModuleDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.publish.PublishOptions;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.ResolveReport;
import org.apache.ivy.core.resolve.ResolveOptions;
import org.apache.ivy.core.retrieve.RetrieveOptions;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.parser.xml.XmlModuleDescriptorWriter;
import org.apache.ivy.plugins.report.XmlReportParser;
import org.apache.ivy.util.DefaultMessageLogger;
import org.apache.ivy.util.Message;
import org.apache.ivy.util.cli.CommandLine;
import org.apache.ivy.util.cli.CommandLineParser;
import org.apache.ivy.util.cli.OptionBuilder;
import org.apache.ivy.util.cli.ParseException;
import org.apache.ivy.util.filter.FilterHelper;
import org.apache.ivy.util.url.CredentialsStore;
import org.apache.ivy.util.url.URLHandler;
import org.apache.ivy.util.url.URLHandlerDispatcher;
import org.apache.ivy.util.url.URLHandlerRegistry;
import org.slf4j.Logger;
import org.myrobotlab.logging.LoggerFactory;


/**
 * Copied from Ivy's "Main" class - with the idea of being able to hook into the
 * error & caching details at a tighter level
 * 
 * Class used to launch ivy as a standalone tool.
 * <p>
 * Valid arguments can be obtained with the -? argument.
 */
public class Ivy2 {

	public final static Logger log = LoggerFactory.getLogger(Ivy2.class.getCanonicalName());

	private static final int HELP_WIDTH = 80;
	private static ResolveReport report = null;

	public static CommandLineParser getParser() {
		return new CommandLineParser()
				.addCategory("settings options")
				.addOption(new OptionBuilder("settings").arg("settingsfile").description("use given file for settings").create())
				.addOption(new OptionBuilder("cache").arg("cachedir").description("use given directory for cache").create())
				.addOption(new OptionBuilder("novalidate").description("do not validate ivy files against xsd").create())
				.addOption(new OptionBuilder("m2compatible").description("use maven2 compatibility").create())
				.addOption(new OptionBuilder("conf").arg("settingsfile").deprecated().description("use given file for settings").create())
				.addOption(
						new OptionBuilder("useOrigin").deprecated().description("use original artifact location " + "with local resolvers instead of copying to the cache")
								.create())

				.addCategory("resolve options")
				.addOption(new OptionBuilder("ivy").arg("ivyfile").description("use given file as ivy file").create())
				.addOption(new OptionBuilder("refresh").description("refresh dynamic resolved revisions").create())
				.addOption(
						new OptionBuilder("dependency").arg("organisation").arg("module").arg("revision")
								.description("use this instead of ivy file to do the rest " + "of the work with this as a dependency.").create())
				.addOption(new OptionBuilder("confs").arg("configurations").countArgs(false).description("resolve given configurations").create())
				.addOption(new OptionBuilder("types").arg("types").countArgs(false).description("comma separated list of accepted artifact types").create())
				.addOption(new OptionBuilder("mode").arg("resolvemode").description("the resolve mode to use").create())
				.addOption(new OptionBuilder("notransitive").description("do not resolve dependencies transitively").create())

				.addCategory("retrieve options")
				.addOption(new OptionBuilder("retrieve").arg("retrievepattern").description("use given pattern as retrieve pattern").create())
				.addOption(new OptionBuilder("ivypattern").arg("pattern").description("use given pattern to copy the ivy files").create())
				.addOption(new OptionBuilder("sync").description("use sync mode for retrieve").create())
				.addOption(new OptionBuilder("symlink").description("create symbolic links").create())

				.addCategory("cache path options")
				.addOption(
						new OptionBuilder("cachepath")
								.arg("cachepathfile")
								.description(
										"outputs a classpath consisting of all dependencies in cache " + "(including transitive ones) "
												+ "of the given ivy file to the given cachepathfile").create())

				.addCategory("deliver options").addOption(new OptionBuilder("deliverto").arg("ivypattern").description("use given pattern as resolved ivy file pattern").create())

				.addCategory("publish options").addOption(new OptionBuilder("publish").arg("resolvername").description("use given resolver to publish to").create())
				.addOption(new OptionBuilder("publishpattern").arg("artpattern").description("use given pattern to find artifacts to publish").create())
				.addOption(new OptionBuilder("revision").arg("revision").description("use given revision to publish the module").create())
				.addOption(new OptionBuilder("status").arg("status").description("use given status to publish the module").create())
				.addOption(new OptionBuilder("overwrite").description("overwrite files in the repository if they exist").create())

				.addCategory("http auth options").addOption(new OptionBuilder("realm").arg("realm").description("use given realm for HTTP AUTH").create())
				.addOption(new OptionBuilder("host").arg("host").description("use given host for HTTP AUTH").create())
				.addOption(new OptionBuilder("username").arg("username").description("use given username for HTTP AUTH").create())
				.addOption(new OptionBuilder("passwd").arg("passwd").description("use given password for HTTP AUTH").create())

				.addCategory("launcher options").addOption(new OptionBuilder("main").arg("main").description("the FQCN of the main class to launch").create())
				.addOption(new OptionBuilder("args").arg("args").countArgs(false).description("the arguments to give to the launched process").create())
				.addOption(new OptionBuilder("cp").arg("cp").description("extra classpath to use when launching process").create())

				.addCategory("message options").addOption(new OptionBuilder("debug").description("set message level to debug").create())
				.addOption(new OptionBuilder("verbose").description("set message level to verbose").create())
				.addOption(new OptionBuilder("warn").description("set message level to warn").create())
				.addOption(new OptionBuilder("error").description("set message level to error").create())

				.addCategory("help options").addOption(new OptionBuilder("?").description("display this help").create())
				.addOption(new OptionBuilder("deprecated").description("show deprecated options").create())
				.addOption(new OptionBuilder("version").description("displays version information").create());
	}

	public static void main(String[] args) throws Exception {
		CommandLineParser parser = getParser();
		try {
			run(parser, args);
			// System.exit(0);
			return;
		} catch (ParseException ex) {
			System.err.println(ex.getMessage());
			usage(parser, false);
			// System.exit(1);
			log.error("Ivy error");
			return;
		}
	}

	public static int run(CommandLineParser parser, String[] args) throws Exception {
		// parse the command line arguments
		CommandLine line = parser.parse(args);
		// Message.setDefaultLogger(new Ivy2MessageLogger(Message.MSG_DEBUG));

		if (line.hasOption("?")) {
			usage(parser, line.hasOption("deprecated"));
			return 0;
		}

		if (line.hasOption("version")) {
			System.out.println("Ivy " + Ivy.getIvyVersion() + " - " + Ivy.getIvyDate() + " :: " + Ivy.getIvyHomeURL());
			return 0;
		}

		boolean validate = line.hasOption("novalidate") ? false : true;

		Ivy ivy = Ivy.newInstance();
		initMessage(line, ivy);
		IvySettings settings = initSettings(line, ivy);
		ivy.pushContext();

		File cache = new File(settings.substitute(line.getOptionValue("cache", settings.getDefaultCache().getAbsolutePath())));

		if (line.hasOption("cache")) {
			// override default cache path with user supplied cache path
			settings.setDefaultCache(cache);
		}

		if (!cache.exists()) {
			cache.mkdirs();
		} else if (!cache.isDirectory()) {
			error(cache + " is not a directory");
		}

		String[] confs;
		if (line.hasOption("confs")) {
			confs = line.getOptionValues("confs");
		} else {
			confs = new String[] { "*" };
		}

		File ivyfile;
		if (line.hasOption("dependency")) {
			String[] dep = line.getOptionValues("dependency");
			ivyfile = File.createTempFile("ivy", ".xml");
			ivyfile.deleteOnExit();
			DefaultModuleDescriptor md = DefaultModuleDescriptor.newDefaultInstance(ModuleRevisionId.newInstance(dep[0], dep[1] + "-caller", "working"));
			DefaultDependencyDescriptor dd = new DefaultDependencyDescriptor(md, ModuleRevisionId.newInstance(dep[0], dep[1], dep[2]), false, false, true);
			for (int i = 0; i < confs.length; i++) {
				dd.addDependencyConfiguration("default", confs[i]);
			}
			md.addDependency(dd);
			XmlModuleDescriptorWriter.write(md, ivyfile);
			confs = new String[] { "default" };
		} else {
			ivyfile = new File(settings.substitute(line.getOptionValue("ivy", "ivy.xml")));
			if (!ivyfile.exists()) {
				error("ivy file not found: " + ivyfile);
			} else if (ivyfile.isDirectory()) {
				error("ivy file is not a file: " + ivyfile);
			}
		}

		if (line.hasOption("useOrigin")) {
			ivy.getSettings().useDeprecatedUseOrigin();
		}
		ResolveOptions resolveOptions = new ResolveOptions().setConfs(confs).setValidate(validate).setResolveMode(line.getOptionValue("mode"))
				.setArtifactFilter(FilterHelper.getArtifactTypeFilter(line.getOptionValues("types")));
		if (line.hasOption("notransitive")) {
			resolveOptions.setTransitive(false);
		}
		if (line.hasOption("refresh")) {
			resolveOptions.setRefresh(true);
		}
		report = ivy.resolve(ivyfile.toURI().toURL(), resolveOptions);
		if (report.hasError()) {
			// System.exit(1);
			log.error("Ivy resolve error");
			List<String> l = report.getAllProblemMessages();
			for (int i = 0; i < l.size(); ++i) {
				log.error(l.get(i));
			}
		}

		ModuleDescriptor md = report.getModuleDescriptor();

		if (confs.length == 1 && "*".equals(confs[0])) {
			confs = md.getConfigurationsNames();
		}

		int ret = 0;

		if (line.hasOption("retrieve")) {
			String retrievePattern = settings.substitute(line.getOptionValue("retrieve"));
			if (retrievePattern.indexOf("[") == -1) {
				retrievePattern = retrievePattern + "/lib/[conf]/[artifact].[ext]";
			}
			String ivyPattern = settings.substitute(line.getOptionValue("ivypattern"));
			ret = ivy.retrieve(md.getModuleRevisionId(), retrievePattern,
					new RetrieveOptions().setConfs(confs).setSync(line.hasOption("sync")).setUseOrigin(line.hasOption("useOrigin")).setDestIvyPattern(ivyPattern)
							.setArtifactFilter(FilterHelper.getArtifactTypeFilter(line.getOptionValues("types"))).setMakeSymlinks(line.hasOption("symlink")));
			log.info("ivy.retrieve returned " + ret);
		}
		if (line.hasOption("cachepath")) {
			outputCachePath(ivy, cache, md, confs, line.getOptionValue("cachepath", "ivycachepath.txt"));
		}

		if (line.hasOption("revision")) {
			ivy.deliver(md.getResolvedModuleRevisionId(), settings.substitute(line.getOptionValue("revision")),
					settings.substitute(line.getOptionValue("deliverto", "ivy-[revision].xml")),
					DeliverOptions.newInstance(settings).setStatus(settings.substitute(line.getOptionValue("status", "release"))).setValidate(validate));
			if (line.hasOption("publish")) {
				ivy.publish(md.getResolvedModuleRevisionId(),
						Collections.singleton(settings.substitute(line.getOptionValue("publishpattern", "distrib/[type]s/[artifact]-[revision].[ext]"))),
						line.getOptionValue("publish"), new PublishOptions().setPubrevision(settings.substitute(line.getOptionValue("revision"))).setValidate(validate)
								.setSrcIvyPattern(settings.substitute(line.getOptionValue("deliverto", "ivy-[revision].xml"))).setOverwrite(line.hasOption("overwrite")));
			}
		}
		if (line.hasOption("main")) {
			// check if the option cp has been set
			List fileList = getExtraClasspathFileList(line);

			// merge -args and left over args
			String[] fargs = line.getOptionValues("args");
			if (fargs == null) {
				fargs = new String[0];
			}
			String[] extra = line.getLeftOverArgs();
			if (extra == null) {
				extra = new String[0];
			}
			String[] params = new String[fargs.length + extra.length];
			System.arraycopy(fargs, 0, params, 0, fargs.length);
			System.arraycopy(extra, 0, params, fargs.length, extra.length);
			// invoke with given main class and merged params
			invoke(ivy, cache, md, confs, fileList, line.getOptionValue("main"), params);
		}
		ivy.getLoggerEngine().popLogger();
		ivy.popContext();
		return 0;
	}

	/**
	 * Parses the <code>cp</code> option from the command line, and returns a
	 * list of {@link File}.
	 * <p>
	 * All the files contained in the returned List exist, non existing files
	 * are simply skipped with a warning.
	 * </p>
	 * 
	 * @param line
	 *            the command line in which the cp option shold be parsed
	 * @return a List of files to include as extra classpath entries, or
	 *         <code>null</code> if no cp option was provided.
	 */
	private static List/* <File> */getExtraClasspathFileList(CommandLine line) {
		List fileList = null;
		if (line.hasOption("cp")) {
			fileList = new ArrayList/* <File> */();
			String[] cpArray = line.getOptionValues("cp");
			for (int index = 0; index < cpArray.length; index++) {
				StringTokenizer tokenizer = new StringTokenizer(cpArray[index], System.getProperty("path.separator"));
				while (tokenizer.hasMoreTokens()) {
					String token = tokenizer.nextToken();
					File file = new File(token);
					if (file.exists()) {
						fileList.add(file);
					} else {
						Message.warn("Skipping extra classpath '" + file + "' as it does not exist.");
					}
				}
			}
		}
		return fileList;
	}

	private static IvySettings initSettings(CommandLine line, Ivy ivy) throws java.text.ParseException, IOException, ParseException {
		IvySettings settings = ivy.getSettings();
		settings.addAllVariables(System.getProperties());
		if (line.hasOption("m2compatible")) {
			settings.setVariable("ivy.default.configuration.m2compatible", "true");
		}

		configureURLHandler(line.getOptionValue("realm", null), line.getOptionValue("host", null), line.getOptionValue("username", null), line.getOptionValue("passwd", null));

		String settingsPath = line.getOptionValue("settings", "");
		if ("".equals(settingsPath)) {
			settingsPath = line.getOptionValue("conf", "");
			if (!"".equals(settingsPath)) {
				Message.deprecated("-conf is deprecated, use -settings instead");
			}
		}
		if ("".equals(settingsPath)) {
			ivy.configureDefault();
		} else {
			File conffile = new File(settingsPath);
			if (!conffile.exists()) {
				error("ivy configuration file not found: " + conffile);
			} else if (conffile.isDirectory()) {
				error("ivy configuration file is not a file: " + conffile);
			}
			ivy.configure(conffile);
		}
		return settings;
	}

	private static void initMessage(CommandLine line, Ivy ivy) {
		if (line.hasOption("debug")) {
			ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_DEBUG));
		} else if (line.hasOption("verbose")) {
			ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_VERBOSE));
		} else if (line.hasOption("warn")) {
			ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_WARN));
		} else if (line.hasOption("error")) {
			ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_ERR));
		} else {
			ivy.getLoggerEngine().pushLogger(new DefaultMessageLogger(Message.MSG_INFO));
		}
	}

	private static void outputCachePath(Ivy ivy, File cache, ModuleDescriptor md, String[] confs, String outFile) {
		try {
			String pathSeparator = System.getProperty("path.separator");
			StringBuffer buf = new StringBuffer();
			Collection all = new LinkedHashSet();
			ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();
			XmlReportParser parser = new XmlReportParser();
			for (int i = 0; i < confs.length; i++) {
				String resolveId = ResolveOptions.getDefaultResolveId(md);
				File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
				parser.parse(report);

				all.addAll(Arrays.asList(parser.getArtifactReports()));
			}
			for (Iterator iter = all.iterator(); iter.hasNext();) {
				ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter.next();
				if (artifact.getLocalFile() != null) {
					buf.append(artifact.getLocalFile().getCanonicalPath());
					buf.append(pathSeparator);
				}
			}

			PrintWriter writer = new PrintWriter(new FileOutputStream(outFile));
			if (buf.length() > 0) {
				writer.println(buf.substring(0, buf.length() - pathSeparator.length()));
			}
			writer.close();
			System.out.println("cachepath output to " + outFile);

		} catch (Exception ex) {
			throw new RuntimeException("impossible to build ivy cache path: " + ex.getMessage(), ex);
		}
	}

	private static void invoke(Ivy ivy, File cache, ModuleDescriptor md, String[] confs, List fileList, String mainclass, String[] args) {
		List urls = new ArrayList();

		// Add option cp (extra classpath) urls
		if (fileList != null && fileList.size() > 0) {
			for (Iterator iter = fileList.iterator(); iter.hasNext();) {
				File file = (File) iter.next();
				try {
					urls.add(file.toURI().toURL());
				} catch (MalformedURLException e) {
					// Should not happen, just ignore.
				}
			}
		}

		try {
			Collection all = new LinkedHashSet();
			ResolutionCacheManager cacheMgr = ivy.getResolutionCacheManager();
			XmlReportParser parser = new XmlReportParser();
			for (int i = 0; i < confs.length; i++) {
				String resolveId = ResolveOptions.getDefaultResolveId(md);
				File report = cacheMgr.getConfigurationResolveReportInCache(resolveId, confs[i]);
				parser.parse(report);

				all.addAll(Arrays.asList(parser.getArtifactReports()));
			}
			for (Iterator iter = all.iterator(); iter.hasNext();) {
				ArtifactDownloadReport artifact = (ArtifactDownloadReport) iter.next();

				if (artifact.getLocalFile() != null) {
					urls.add(artifact.getLocalFile().toURI().toURL());
				}
			}
		} catch (Exception ex) {
			throw new RuntimeException("impossible to build ivy cache path: " + ex.getMessage(), ex);
		}

		URLClassLoader classLoader = new URLClassLoader((URL[]) urls.toArray(new URL[urls.size()]), Ivy2.class.getClassLoader());

		try {
			Class c = classLoader.loadClass(mainclass);

			Method mainMethod = c.getMethod("main", new Class[] { String[].class });

			Thread.currentThread().setContextClassLoader(classLoader);
			mainMethod.invoke(null, new Object[] { (args == null ? new String[0] : args) });
		} catch (ClassNotFoundException cnfe) {
			throw new RuntimeException("Could not find class: " + mainclass, cnfe);
		} catch (SecurityException e) {
			throw new RuntimeException("Could not find main method: " + mainclass, e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Could not find main method: " + mainclass, e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("No permissions to invoke main method: " + mainclass, e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Unexpected exception invoking main method: " + mainclass, e);
		}
	}

	private static void configureURLHandler(String realm, String host, String username, String passwd) {
		CredentialsStore.INSTANCE.addCredentials(realm, host, username, passwd);

		URLHandlerDispatcher dispatcher = new URLHandlerDispatcher();
		URLHandler httpHandler = URLHandlerRegistry.getHttp();
		dispatcher.setDownloader("http", httpHandler);
		dispatcher.setDownloader("https", httpHandler);
		URLHandlerRegistry.setDefault(dispatcher);
	}

	private static void error(String msg) throws ParseException {
		throw new ParseException(msg);
	}

	private static void usage(CommandLineParser parser, boolean showDeprecated) {
		// automatically generate the help statement
		PrintWriter pw = new PrintWriter(System.out);
		parser.printHelp(pw, HELP_WIDTH, "ivy", showDeprecated);
		pw.flush();
	}

	private Ivy2() {
	}

	public static ResolveReport getReport() {
		return report;
	}

}
