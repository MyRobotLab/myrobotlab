package org.myrobotlab.framework;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

public class StreamGobbler extends Thread {
	public final static Logger log = LoggerFactory.getLogger(StreamGobbler.class);
	
	InputStream is;
	String type;

	public StreamGobbler(InputStream is, String type) {
		super(String.format("StreamGobbler_%s", type));
		this.is = is;
		this.type = type;
	}

	@Override
	public void run() {
		try {
			InputStreamReader in = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(in);
			String line = null;
			while ((line = br.readLine()) != null) {
				// System.out.println(type + "> " + line);
				log.info(type + ">> " + line);
			}
		} catch (IOException e) {
			log.error("leaving StreamGobbler");
			Logging.logException(e);
		} finally {
			try{
				if (is != null){
					is.close();
				}
			} catch(Exception ex){
			}
		}
	}
}