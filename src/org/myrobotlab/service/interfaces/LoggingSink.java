package org.myrobotlab.service.interfaces;

public interface LoggingSink {

	public String getName();
	public String error(String format, Object... args);
	public String info(String format, Object... args);
	public String warn(String format, Object... args);
}
