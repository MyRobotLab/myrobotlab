package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface Listener extends NameProvider {

	/*
	 * important for callback information
	 * if we can optimize direct callback 
	 * or must use pub/sub queues
	 * 
	 */
	public boolean isLocal();
}
