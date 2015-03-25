package org.myrobotlab.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

public class StreamGobbler extends Thread {
	public final static Logger log = LoggerFactory.getLogger("");

	InputStream is;
	ArrayList<OutputStream> os;

	String type;
	String tag;

	public StreamGobbler(InputStream is, ArrayList<OutputStream> os, String type) {
		super(String.format("%s_%s", type, Runtime.getPID()));
		// this.tag = String.format("%s_%s<<", type, Runtime.getPID());
		this.tag = "";
		this.is = is;
		this.os = os;
		this.type = type;
	}

	@Override
	public void run() {
		try {
			InputStreamReader in = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(in);
			String line = null;
			while ((line = br.readLine()) != null) {
				// FIXME OutputStream Versus Log !!! based on - IS_AGENT ||
				// FROM_AGENT ||
				// log.info(String.format("%s%s", tag, line));
				// log.info(String.format("<<%s", line));
				for (int i = 0; i < os.size(); ++i) {
					os.get(i).write(String.format("%s\n", line).getBytes());
				}
			}
		} catch (IOException e) {
			log.error(tag + "leaving StreamGobbler");
			Logging.logError(e);
		}
		/*
		 * NO CLOSING !?!?!?!?
		 * 
		 * finally { try{ if (is != null){ is.close(); } } catch(Exception ex){
		 * } }
		 */
	}
}