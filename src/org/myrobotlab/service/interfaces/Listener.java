package org.myrobotlab.service.interfaces;

public interface Listener extends NameProvider {

	/**
	 * important for callback information
	 * if we can optimize direct callback 
	 * or must use pub/sub queues
	 * 
	 * @return
	 */
	public boolean isLocal();
}
