package org.myrobotlab.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.myrobotlab.framework.Encoder;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.StreamGobbler;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.slf4j.Logger;

/**
 * CLI should be a singleton in a process !
 *
 * 
 * @author GroG
 *
 */
public class CLI extends Service {
	
	// commands
	public final static String cd = "cd";
	public final static String pwd = "pwd";
	public final static String ls = "ls";
	public final static String help = "help";
	public final static String question = "?";
	
	public final static HashSet<String> cmdSet = new HashSet<String>();

	// FIXME - needs refactor / merge with StreamGobbler
	// FIXME - THIS CONCEPT IS SOOOOOO IMPORTANT
	// - its a Central Point Controller - where input (any InputStream) can send
	// data to be decoded on a very common API e.g. (proto
	// scheme)(host)/api/inputEncoding/responseEncoding/instance/(method)/(params...)
	// Agent + (RemoteAdapter/WebGUI/Netosphere) + CLI(command processor part
	// with InStream/OutStream) - is most Big-Fu !
	public class Decoder extends Thread {
		public String cwd = "/";
		public String prompt = "(:";
		String name;
		transient CLI cli;
		transient InputStream is;
		// TODO ecoding defaults & methods to change
		// FIXME - need reference to OutputStream to return
		String inputEncoding = Encoder.TYPE_REST; // REST JSON
		String outputEncoding = Encoder.TYPE_JSON; // REST JSON

		public Decoder(CLI cli, String name, InputStream is) {
			super(String.format("Decoder_%s", name));
			this.cli = cli;
			this.name = name;
			this.is = is;
		}

		@Override
		public void run() {
			try {
				InputStreamReader in = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(in); // < FIXME ? is
															// Buffered
															// Necessary?
				out(String.format("\n[%s %s]%s", Runtime.getInstance().getName(), cwd, prompt).getBytes());

				String line = null;

				// FIXME FIXME FIXME - line.split(" ") - invoke(line[0], line)
				// "One Handler to Rule them All !"
				while ((line = br.readLine()) != null) {

					line = line.trim();

					if (line.length() == 0) {
						writePrompt();
						continue;
					}

					if (attachedIn != null) {
						if ("detach".equals(line)) {
							// multiple in future mabye
							detach();
							continue;
						}

						// relaying command to another process
						attachedIn.write(line);
						attachedIn.newLine();
						attachedIn.flush();
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
						out(cwd.getBytes());
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
							path = String.format("/%s%s", Encoder.PREFIX_API, line);
						} else {
							path = String.format("/%s%s%s", Encoder.PREFIX_API, cwd, line);
						}

						log.info(path);
						try {
							
							// New Way
							Object ret = Encoder.invoke(path);
							if (ret != null && ret instanceof Serializable) {
								// configurable use log or system.out ?
								// FIXME - make getInstance configurable
								// Encoder
								// reference !!!
								out(Encoder.toJson(ret).getBytes());
							}
							/* Old Way
							Message msg = Encoder.decodePathInfo(path);
							if (msg != null) {
								info("incoming msg[%s]", msg);

								// get service - is this a security breech ?
								ServiceInterface si = Runtime.getService(msg.name);
								Object ret = si.invoke(msg.method, msg.data);

								// want message ? or just data ?
								// configurable ...
								// if you data with tags - you might as well do
								// message !
								// - return only callbacks this way ->
								// si.in(msg);
								if (ret != null && ret instanceof Serializable) {
									// configurable use log or system.out ?
									// FIXME - make getInstance configurable
									// Encoder
									// reference !!!
									out(Encoder.toJson(ret).getBytes());
								}
							}
							*/
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
		}

		public void writePrompt() throws IOException {
			out(String.format("\n[%s %s]%s", Runtime.getInstance().getName(), cwd, prompt).getBytes());
		}

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

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(CLI.class);

	private HashMap<String, Pipe> pipes = new HashMap<String, Pipe>();
	// my "real" in & out
	transient Decoder in;
	transient OutputStream os;

	transient FileOutputStream fos;

	// active relay - could be list - but lets start simple
	String attached = null;
	// transient OutputStream attachedIn = null;
	transient BufferedWriter attachedIn = null;

	transient StreamGobbler attachedOut = null;

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
	public CLI(String n) {
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

	/**
	 * attach to another processes' CLI
	 * 
	 * @param name
	 * @return
	 */
	public boolean attach(String name) {
		if (!pipes.containsKey(name)) {
			error("%s not found", name);
			return false;
		}

		Pipe pipe = pipes.get(name);
		attached = name;
		// stdin will now be relayed and not interpreted
		attachedIn = new BufferedWriter(new OutputStreamWriter(pipe.in));
		// need to fire up StreamGobbler
		// (new Process) --- stdout --> (Agent Process) StreamGobbler --->
		// stdout
		ArrayList<OutputStream> outRelay = new ArrayList<OutputStream>();

		if (os != null) {
			outRelay.add(os);
		}

		if (fos != null) {
			outRelay.add(fos);
		}

		attachedOut = new StreamGobbler(pipe.out, outRelay, name);
		attachedOut.start();

		// grab input output from foreign process

		// introduce - hello - get response check with
		// timer - because if a CLI is not there
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
			if (fos == null && Runtime.isAgent()) {
				fos = new FileOutputStream("agent.log");
			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

	public String cd(String path) {
		in.cwd = path;
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

	@Override
	public String[] getCategories() {
		return new String[] { "framework" };
	}

	@Override
	public String getDescription() {
		return "used as a general cli";
	}

	/*
	 * public ArrayList<ProcessData> lp(){ return
	 * Runtime.getAgent().getProcesses(); }
	 */

	/**
	 * FIXME !!! return Object[] and let CLI command processor handle encoding
	 * for return
	 * 
	 * path is always absolute never relative
	 * 
	 * @param path
	 * @throws IOException
	 */
	public void ls(String path) throws IOException {
		StringBuffer sb = new StringBuffer();
		String[] parts = path.split("/");

		if (path.equals("/")) {
			// FIXME don't do this here !!!
			out(Encoder.toJson(Runtime.getServiceNames()).toString().getBytes());
		} else if (parts.length == 2 && !path.endsWith("/")) {
			// FIXME don't do this here !!!
			out(Encoder.toJson(Runtime.getService(parts[1])).toString().getBytes());
		} else if (parts.length == 2 && path.endsWith("/")) {
			ServiceInterface si = Runtime.getService(parts[1]);
			// FIXME don't do this here !!!
			out(Encoder.toJson(si.getDeclaredMethodNames()).toString().getBytes());
		}

		// if path == /serviceName - json return ? Cool !
		// if path /serviceName/ - method return
	}

	public void out(byte[] data) throws IOException {

		// if (Runtime.isAgent()) {
		if (os != null)
			os.write(data);

		if (fos != null)
			fos.write(data);
		// }
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

			if (fos != null) {
				fos.close();
			}
			fos = null;
		} catch (Exception e) {
			Logging.logError(e);
		}
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("ERROR");

		try {

			CLI cli = (CLI) Runtime.start("cli", "CLI");
			/*
			 * cli.ls("/"); cli.ls("/cli"); cli.ls("/cli/");
			 */
			// cli.test();

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
