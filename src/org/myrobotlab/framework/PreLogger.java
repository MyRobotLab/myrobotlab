package org.myrobotlab.framework;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

public class PreLogger {

	private static PreLogger instance = null;
	private static FileWriter fileWriter = null;
	private static BufferedWriter bufferedWriter = null;
	private static long startTS = 0;
	public final static String PRELOG_FILENAME = "prelog.log";
	//public static String PRELOG_FILENAME;

	public static PreLogger getInstance() {
		if (instance == null) {
			try {
				// FIXME - APPEND IF EXISTS - file will be transfered to mrl.log and removed by Runtime
				// if successful
				instance = new PreLogger();
				//SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
				//PRELOG_FILENAME = String.format("prelog.%s.log", formatter.format(new Date()));
				// TODO - make pre-logging logger with no dependencies
				fileWriter = new FileWriter(PRELOG_FILENAME, true);
				bufferedWriter = new BufferedWriter(fileWriter);
				startTS = System.currentTimeMillis();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return instance;
	}

	public void info(String msg) {
		info(msg, (Object[]) null);
	}

	public void info(String msg, Object... params) {
		write("INFO", msg, params);
	}

	public void warn(String msg) {
		info(msg, (Object[]) null);
	}

	public void warn(String msg, Object... params) {
		write("WARN", msg, params);
	}

	public void error(String msg) {
		info(msg, (Object[]) null);
	}

	public void error(String msg, Object... params) {
		write("ERROR", msg, params);
	}

	private void write(String level, String msg, Object... params) {
		try {
			String msgToWrite;
			if (params == null) {
				msgToWrite = msg;
			} else {
				msgToWrite = String.format(msg, params);
			}

			String data = String.format("%d %s %s\n", System.currentTimeMillis() - startTS, level, msgToWrite);
			System.out.println(data);

			bufferedWriter.write(data);
			bufferedWriter.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public final static String stackToString(final Throwable e) {
		StringWriter sw;
		try {
			sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
		} catch (Exception e2) {
			return "bad stackToString";
		}
		return "------\r\n" + sw.toString() + "------\r\n";
	}

	public static void main(String[] args) {
		PreLogger log = PreLogger.getInstance();
		log.info("hello");
		log.info("%s is this %d", "new string", 10);
	}

	static public void close() {
		if (instance != null) {
			try {
				bufferedWriter.write("closing file");
				bufferedWriter.flush();
				bufferedWriter.close();
				fileWriter.close();				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void error(Exception e) {
		error(stackToString(e));
	}
}
