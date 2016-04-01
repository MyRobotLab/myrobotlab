package org.myrobotlab.codec;

import java.io.OutputStream;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;

public class CodecCli implements Codec {
	
	// commands
	public final static String cd = "cd";
	public final static String pwd = "pwd";
	public final static String ls = "ls";
	public final static String help = "help";
	public final static String question = "?";

	
	String cwd = "/";
	//String prompt = "(:";
	String prompt = "#";

	@Override
	public void encode(OutputStream out, Object obj) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Object[] decodeArray(Object data) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object decode(Object data, Class<?> type) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getMimeType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return null;
	}

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {
		} catch (Exception e) {
			Logging.logError(e);
		}

	}

}
