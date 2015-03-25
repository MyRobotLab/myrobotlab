package org.myrobotlab.framework;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * WARNING !!! - this class used to extend Exception or Throwable - but the gson
 * serializer would stack overflow with self reference issue
 * 
 * TODO - allow radix tree searches for "keys" ???
 */
public class Status implements Serializable {// extends Exception {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Status.class);

	public final static String DEBUG = "debug";

	public final static String INFO = "info";

	public final static String WARN = "warn";

	public final static String ERROR = "error";

	public String name; // service name ???

	public String level;

	public String key;
	public String detail;

	private ArrayList<Status> statuses = null;

	// private String allowLevel = null; pre filtering attempt .. aborted

	public static Status debug(String format, Object... args) {
		Status status = new Status(String.format(format, args));
		status.level = DEBUG;
		return status;
	}

	public static Status error(Exception e) {
		Status s = new Status(e);
		s.level = ERROR;
		return s;
	}

	public static Status error(String msg) {
		Status s = new Status(msg);
		s.level = ERROR;
		return s;
	}

	public static Status error(String format, Object... args) {
		Status status = new Status(String.format(format, args));
		status.level = ERROR;
		return status;
	}

	/*
	 * public void allowDebug(String b) { allowLevel = b; }
	 */

	public static Status info(String msg) {
		Status s = new Status(msg);
		s.level = INFO;
		return s;
	}

	public static Status info(String format, Object... args) {
		Status status = new Status(String.format(format, args));
		status.level = INFO;
		return status;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		Status test = new Status("i am pessimistic");
		Status subTest = new Status("i am sub pessimistic");

		test.add(subTest);

		String json = Encoder.toJson(test);
		Status z = Encoder.fromJson(json, Status.class);
		log.info(json);
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

	public Status(Exception e) {
		this.level = ERROR;
		this.key = e.getClass().getSimpleName();
		StringWriter sw;
		try {
			sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			detail = sw.toString();
		} catch (Exception e2) {
		}
		this.key = e.getMessage();
	}

	public Status(Status s) {
		if (s == null) {
			return;
		}
		this.name = s.name;
		this.level = s.level;
		this.key = s.key;
		this.detail = s.detail;
	}

	/**
	 * for minimal amount of information error is assumed, and info is detail of
	 * an ERROR
	 * 
	 * @param detail
	 */
	public Status(String detail) {
		this.level = ERROR;
		this.detail = detail;
	}

	public Status(String name, String level, String key, String detail) {
		this.name = name;
		this.level = level;
		this.key = key;
		this.detail = detail;
	}

	public void add(Status status) {
		if (status != null) {
			if (statuses == null) {
				statuses = new ArrayList<Status>();
			}
			statuses.add(status);
		}
	}

	public Status addDebug(String format, Object... args) {
		Status status = debug(format, args);
		log.debug(String.format(format, args));
		add(status);
		return status;
	}

	public Status addError(Exception e) {
		Logging.logError(e);
		Status status = error("%s %s", e.getMessage(), stackToString(e));
		add(status);
		return status;
	}

	public Status addError(String format, Object... args) {
		Status status = error(format, args);
		log.error(String.format(format, args));
		add(status);
		return status;
	}

	public Status addInfo(String format, Object... args) {
		Status status = info(format, args);
		log.info(String.format(format, args));
		add(status);
		return status;
	}

	public Status addNamedInfo(String name, String format, Object... args) {
		Status status = info(format, args);
		log.info(String.format(format, args));
		status.name = name;
		add(status);
		return status;
	}

	public ArrayList<Status> flatten() {
		ArrayList<Status> ret = new ArrayList<Status>();

		if (statuses != null) {
			for (int i = 0; i < statuses.size(); ++i) {
				Status status = statuses.get(i);
				ArrayList<Status> s = status.flatten();
				for (int j = 0; j < s.size(); ++j) {
					ret.add(s.get(j));
				}
			}
		}
		return ret;
	}

	public boolean hasError() {
		// if I am in error
		// then return error
		if (ERROR.equals(level)) {
			return true;
		}
		// if my children have an error
		// return the sum of that error
		boolean b = false;

		if (statuses == null) {
			return b;
		}

		for (int i = 0; i < statuses.size(); ++i) {
			b |= statuses.get(i).hasError();
		}
		return b;
	}

	public boolean isDebug() {
		return DEBUG.equals(level);
	}

	public boolean isError() {
		return ERROR.equals(level);
	}

	public boolean isInfo() {
		return INFO.equals(level);
	}

	public boolean isWarn() {
		return WARN.equals(level);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (name != null) {
			sb.append(name);
			sb.append(" ");
		}
		if (level != null) {
			sb.append(level);
			sb.append(" ");
		}
		if (key != null) {
			sb.append(key);
			sb.append(" ");
		}
		if (detail != null) {
			sb.append(detail);
		}

		sb.append(" ");

		if (statuses != null) {
			for (int i = 0; i < statuses.size(); ++i) {
				sb.append(statuses.get(i).toString());
			}
		}

		return sb.toString();
	}

}
