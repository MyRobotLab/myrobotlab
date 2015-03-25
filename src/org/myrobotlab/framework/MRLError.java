package org.myrobotlab.framework;

public class MRLError extends Exception {
	private static final long serialVersionUID = 1L;

	final public static String LEVEL_ERROR = "error";
	final public static String LEVEL_WARNING = "warning";
	final public static String LEVEL_INFO = "info";

	String level = LEVEL_ERROR;

	public MRLError() {
		super();
	}

	public MRLError(String msg) {
		super(msg);
	}

	public MRLError(String format, Object... params) {
		super(String.format(format, params));
	}

	public MRLError(Throwable throwable) {
		super(throwable);
	}

	@Override
	public String toString() {
		return String.format("%s %s", level, getMessage());
	}

}
