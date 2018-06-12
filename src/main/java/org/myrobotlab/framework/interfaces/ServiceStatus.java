package org.myrobotlab.framework.interfaces;

import org.myrobotlab.framework.Status;

public interface ServiceStatus {

	/**
	 * is a service "ready" for whatever service it provides
	 * @return
	 */
	public boolean isReady();

	public boolean isRunning();

	public String clearLastError();

	public boolean hasError();

	public Status getLastError();

}
