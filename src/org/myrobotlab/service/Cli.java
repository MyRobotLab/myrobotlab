package org.myrobotlab.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.codec.CodecUri;
import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.InvokerUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.framework.Status;
import org.myrobotlab.framework.StreamGobbler;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * Cli - This is a command line interface to MyRobotLab. It supports some shell
 * like commands such as "cd", and "ls" Use the command "help" to display help.
 *
 * should be a singleton in a process. This does not seem to work on
 * cygwin/windows, but it does work in a command prompt. Linux/Mac can use this
 * via a Terminal / Console window.
 * 
 * @author GroG
 *
 */
public class Cli extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(Cli.class);

	public final static String cd = "cd";
	public final static String pwd = "pwd";
	public final static String ls = "ls";
	public final static String help = "help";
	public final static String question = "?";

	transient private HashMap<String, Pipe> pipes = new HashMap<String, Pipe>();
	// my "real" std:in & std:out
	transient Decoder in;
	transient OutputStream os;

	ArrayList<String> history = new ArrayList<String>();

	// transient FileOutputStream fos;

	String cwd = "/";
	String prompt = "#";

	// active relay - could be list - but lets start simple
	String attached = null;
	// transient OutputStream attachedIn = null;
	transient OutputStream attachedIn = null;
	transient StreamGobbler attachedOut = null;

	// ================= Decoder Begin =================
	// FIXME - tab to autoComplete !
	// FIXME - needs refactor / merge with StreamGobbler
	// FIXME - THIS CONCEPT IS SOOOOOO IMPORTANT
	// - its a Central Point Controller - where input (any InputStream) can send
	// data to be decoded on a very common API e.g. (proto
	// scheme)(host)/api/inputEncoding/responseEncoding/instance/(method)/(params...)
	// Agent + (RemoteAdapter/WebGui/Netosphere) + Cli(command processor part
	// with InStream/OutStream) - is most Big-Fu !
	public class Decoder extends Thread {
		// public String cwd = "/"; CHANGED THIS - it now is GLOBAL - :P
		String name;
		transient Cli cli;
		transient InputStream is;
		// TODO ecoding defaults & methods to change
		// FIXME - need reference to OutputStream to return
		String inputEncoding = CodecUtils.TYPE_URI; // REST JSON
		String outputEncoding = CodecUtils.TYPE_JSON; // JSON / JSON MSG

		public Decoder(Cli cli, String name, InputStream is) {
			super(String.format("cli-decoder-%s", name));
			this.cli = cli;
			this.name = name;
			this.is = is;
		}

		StringBuffer sb = new StringBuffer();

		@Override
		public void run() {
			try {
				writePrompt();
				String line = null;
				int c = 0;

				while ((c = is.read()) != -1) {

					// handle up arrow - tab autoComplet - and regular \n
					// if not one of these - then append the next character
					if (c != '\n') {
						sb.append((char) c);
						continue;
					} else {
						line = sb.toString();
						sb.setLength(0);
					}

					line = line.trim();

					// order of precedence
					// 1. execute cli methods
					// 2. execute service methods
					// 3. execute Runtime methods

					if (line.length() == 0) {
						writePrompt();
						continue;
					}

					// FIXME - don't need a buffered writer here
					// everything should be OutputStream
					if (attachedIn != null) {
						if ("detach".equals(line)) {
							// multiple in future mabye
							detach();
							continue;
						}

						// relaying command to another process
						try {
							attachedIn.write(String.format("%s\n", line).getBytes());
							attachedIn.flush();
						} catch (Exception e) {
							log.error("std:in ---(agent)---X---> process ({})", name);
							log.info("detaching... ");
							detach();
						}
						// writePrompt();
						continue;
					}

					if (line.startsWith(cd)) {
						String path = "/";
						if (line.length() > 2) {
							// FIXME - cheesy - "look" for relative directories
							// !
							if (!line.contains("/")) {
								path = "/" + line.substring(3);
							} else {
								path = line.substring(3);
							}
						}
						cli.cd(path);
					} else if (line.startsWith(help)) {
						// TODO dump json command object
						// which has a map of commands
					} else if (line.startsWith(pwd)) {
						out(String.format("%s\n", cwd).getBytes());
					} else if (line.startsWith(ls)) {
						String path = cwd; // <-- path =
						if (line.length() > 3) {
							path = line.substring(3);
						}

						path = path.trim();
						// absolute path always
						cli.ls(path);
					} else if (line.startsWith("lp")) {

						// cli.lp(path??);
						// cli.lp();

					} else {

						String path = null;
						if (line.startsWith("/")) {
							path = String.format("/%s%s", CodecUtils.PREFIX_API, line);
						} else {
							path = String.format("/%s%s%s", CodecUtils.PREFIX_API, cwd, line);
						}

						log.info(path);
						try {

							Object ret = null;
							// Object ret = InvokerUtils.invoke(path);
							// InvokerUtils removed - need more access & control
							Message msg = CodecUri.decodePathInfo(path);
							ServiceInterface si = Runtime.getService(msg.name);
							if (si == null) {
								ret = Status.error("could not find service %s", msg.name);
							} else {
								ret = si.invoke(msg.method, msg.data);
							}

							if (ret != null && ret instanceof Serializable) {
								// configurable use log or system.out ?
								// FIXME - make getInstance configurable
								// Encoder
								// reference !!!
								out(CodecUtils.toJson(ret).getBytes());
							}

						} catch (Exception e) {
							Logging.logError(e);
						}

					}
					writePrompt();
				} // while read line

			} catch (IOException e) {
				log.error("leaving Decoder");
				Logging.logError(e);
			}

			/*
			 * DON'T CLOSE - WE MAY WANT TO RE-ATTACH finally {
			 * FileIO.closeStream(is); }
			 */
			log.info("LEAVING STDIN READING!");
		}

	}

	// ================= Decoder End =================

	public void writePrompt() throws IOException {
		out(getPrompt().getBytes());
	}

	public String getPrompt() {
		return String.format("%s:%s%s ", Runtime.getInstance().getName(), cwd, prompt);
	}

	public Object process(String line) throws IOException {
		// FIXME - must read char by char to process up-arrow history commands
		// in.read()
		line = line.trim();

		if (line.length() == 0) {
			writePrompt();
			return null;
		}

		if (attachedIn != null) {
			if ("detach".equals(line)) {
				// multiple in future mabye
				detach();
				return null;
			}

			// relaying command to another process
			attachedIn.write(String.format("%s\n", line).getBytes());
			attachedIn.flush();
			// writePrompt();
			return null;
		}

		if (line.startsWith(cd)) {
			String path = "/";
			if (line.length() > 2) {
				// FIXME - cheesy - "look" for relative directories
				// !
				if (!line.contains("/")) {
					path = "/" + line.substring(3);
				} else {
					path = line.substring(3);
				}
			}
			cd(path);
		} else if (line.startsWith(help)) {
			// TODO dump json command object
			// which has a map of commands
		} else if (line.startsWith(pwd)) {
			out(cwd.getBytes());
		} else if (line.startsWith(ls)) {
			String path = cwd; // <-- path =
			if (line.length() > 3) {
				path = line.substring(3);
			}

			path = path.trim();
			// absolute path always
			ls(path);
		} else if (line.startsWith("lp")) {

			// cli.lp(path??);
			// cli.lp();

		} else {

			String path = null;
			log.info("line length {}", line.length());
			if (line.startsWith("/")) {
				path = String.format("/%s%s", CodecUtils.PREFIX_API, line);
			} else {
				path = String.format("/%s%s%s", CodecUtils.PREFIX_API, cwd, line);
			}

			log.info(path);
			try {

				// if service is local - we can trasact
				Message msg = CodecUri.decodePathInfo(path);
				Object ret = null;
				if (Runtime.getService(msg.name).isLocal()) {
					ret = InvokerUtils.invoke(path);
				} else {
					// FIXME - sendBlocking is not getting a return
					ret = sendBlocking(msg.name, msg.method, msg.data);
				}

				if (ret != null && ret instanceof Serializable) {
					// configurable use log or system.out ?
					// FIXME - make getInstance configurable
					// Encoder
					// reference !!!
					out(CodecUtils.toJson(ret).getBytes());
				}
				/*
				 * Old Way Message msg = Encoder.decodePathInfo(path); if (msg
				 * != null) { info("incoming msg[%s]", msg);
				 * 
				 * // get service - is this a security breech ? ServiceInterface
				 * si = Runtime.getService(msg.name); Object ret =
				 * si.invoke(msg.method, msg.data);
				 * 
				 * // want message ? or just data ? // configurable ... // if
				 * you data with tags - you might as well do // message ! // -
				 * return only callbacks this way -> // si.in(msg); if (ret !=
				 * null && ret instanceof Serializable) { // configurable use
				 * log or system.out ? // FIXME - make getInstance configurable
				 * // Encoder // reference !!!
				 * out(Encoder.toJson(ret).getBytes()); } }
				 */
			} catch (Exception e) {
				Logging.logError(e);
			}

		}
		writePrompt();

		return null;
	}

	public class Pipe {
		public String name;
		public transient InputStream out;
		public transient OutputStream in;

		public Pipe(String name, InputStream out, OutputStream in) {
			this.name = name;
			this.out = out;
			this.in = in;
		}
	}

	/**
	 * Command Line Interpreter - used for processing encoded (default RESTful)
	 * commands from std in and returning results in (default JSON) encoded
	 * return messages.
	 * 
	 * Has the ability to pipe to another process - if attached to another
	 * process handle, and the ability to switch between many processes
	 * 
	 * @param n
	 */
	public Cli(String n) {
		super(n);
	}

	/**
	 * add an i/o pair to this cli for the possible purpose attaching
	 * 
	 * @param name
	 * @param process
	 * @return
	 */
	public void add(String name, InputStream out, OutputStream in) {
		pipes.put(name, new Pipe(name, out, in));
	}

	public boolean attach() {
		return attach(null);
	}

	/**
	 * attach to another processes' Cli
	 * 
	 * @param name
	 * @return
	 */
	public boolean attach(String name) {

		if (pipes.size() == 1) {
			// only 1 choice
			for (String key : pipes.keySet()) {
				name = key;
			}
		}

		if (!pipes.containsKey(name)) {
			error("%s not found", name);
			return false;
		}

		Pipe pipe = pipes.get(name);
		attached = name;
		// stdin will now be relayed and not interpreted
		attachedIn = pipe.in;
		// need to fire up StreamGobbler
		// (new Process) --- stdout --> (Agent Process) StreamGobbler --->
		// stdout
		ArrayList<OutputStream> outRelay = new ArrayList<OutputStream>();

		if (os != null) {
			outRelay.add(os);
		}

		/*
		 * if (fos != null) { outRelay.add(fos); }
		 */

		attachedOut = new StreamGobbler(pipe.out, outRelay, name);
		attachedOut.start();

		// grab input output from foreign process

		// introduce - hello - get response check with
		// timer - because if a Cli is not there
		// we cant attach to it

		return true;
	}

	public void attachStdIO() {
		if (in == null) {
			in = new Decoder(this, "stdin", System.in);
			in.start();
		} else {
			log.info("stdin already attached");
		}

		// if I'm not an agent then just writing to System.out is fine
		// because all of it will be relayed to an Agent if I'm spawned
		// from an Agent.. or
		// If I'm without an Agent I'll just do the logging I was directed
		// to on the command line
		if (os == null) {
			os = System.out;
		} else {
			log.info("stdout already attached");
		}

		try {
			// if I'm an agent I'll do dual logging
			/*
			 * if (fos == null && Runtime.isAgent()) { fos = new
			 * FileOutputStream("agent.log"); }
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public String cd(String path) {
		cwd = path;
		return path;
	}

	private void detach() throws IOException {
		out(String.format("detaching from %s", attached).getBytes());
		attached = null;
		attachedIn = null;
		if (attachedOut != null) {
			attachedOut.interrupt();
		}
		attachedOut = null;
	}

	public void detachStdIO() {
		if (in != null) {
			in.interrupt();
		}
	}

	public String echo(String msg) {
		return msg;
	}

	/*
	 * public ArrayList<ProcessData> lp(){ return
	 * Runtime.getAgent().getProcesses(); }
	 */

	/**
	 * FIXME !!! return Object[] and let Cli command processor handle encoding
	 * for return
	 * 
	 * path is always absolute never relative
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void ls(String path) throws IOException {
		String[] parts = path.split("/");

		if (path.equals("/")) {
			// FIXME don't do this here !!!
			out(String.format("%s\n", CodecUtils.toJson(Runtime.getServiceNames()).toString()).getBytes());
		} else if (parts.length == 2 && !path.endsWith("/")) {
			// FIXME don't do this here !!!
			out(String.format("%s\n", CodecUtils.toJson(Runtime.getService(parts[1])).toString()).getBytes());
		} else if (parts.length == 2 && path.endsWith("/")) {
			ServiceInterface si = Runtime.getService(parts[1]);
			// FIXME don't do this here !!!
			out(String.format("%s\n", CodecUtils.toJson(si.getDeclaredMethodNames()).toString()).getBytes());
		}

		// if path == /serviceName - json return ? Cool !
		// if path /serviceName/ - method return
	}

	public void out(byte[] data) throws IOException {

		// if (Runtime.isAgent()) {
		if (os != null) {
			os.write(data);
			os.flush();
		}

		/*
		 * if (fos != null) { fos.write(data); fos.flush(); }
		 */
		// }
		invoke("stdout", data);
	}

	public String stdout(byte[] data) {
		if (data != null)
			return new String(data);
		else {
			return "";
		}
	}

	public void out(String str) throws IOException {
		out(str.getBytes());
	}

	@Override
	public void startService() {
		super.startService();
		attachStdIO();
	}

	@Override
	public void stopService() {
		super.stopService();
		try {
			if (in != null) {
				in.interrupt();
			}

			in = null;
			if (os != null) {
				os.close();
			}
			os = null;

			/*
			 * if (fos != null) { fos.close(); } fos = null;
			 */
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {
		ServiceType meta = new ServiceType(Cli.class.getCanonicalName());
		meta.addDescription("command line interpreter interface");
		meta.addCategory("framework");
		return meta;
	}

	public static void main(String[] args) {
		LoggingFactory.init("ERROR");

		try {

			Runtime.start("gui", "SwingGui");
			Cli cli = (Cli) Runtime.start("cli", "Cli");

			cli.process("help");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
