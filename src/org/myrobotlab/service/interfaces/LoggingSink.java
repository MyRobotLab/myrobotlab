package org.myrobotlab.service.interfaces;

public interface LoggingSink {
	// FIXME - normalize
	public String error(String format, Object... args);
	public void info(String format, Object... args);
	public void warn(String format, Object... args);
}
