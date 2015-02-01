package org.myrobotlab.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class CLI extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(CLI.class);
	
	HashMap<String, InputStream> in;
	HashMap<String, OutputStream> out;

	public CLI(String n) {
		super(n);
	}
	
	public void start(InputStream in){
		
	}

	@Override
	public String getDescription() {
		return "used as a general cli";
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			CLI cli = (CLI)Runtime.start("cli", "CLI");
			cli.test();	

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

}
