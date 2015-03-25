package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.Status;

public interface RepoUpdateListener {

	/**
	 * Status events from Repo when dependency resolution is attempted
	 * 
	 * @param status
	 * @return
	 */
	public Status updateProgress(final Status status);

}
