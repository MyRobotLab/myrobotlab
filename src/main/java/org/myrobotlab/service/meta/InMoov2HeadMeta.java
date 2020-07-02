package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2HeadMeta extends MetaData {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(InMoov2HeadMeta.class);

	/**
	 * This class is contains all the meta data details of a service. It's peers,
	 * dependencies, and all other meta data related to the service.
	 * 
	 */
	public InMoov2HeadMeta() {

		Platform platform = Platform.getLocalInstance();
		addDescription("The inmoov2 head");
		addPeer("jaw", "Servo", "Jaw servo");
		addPeer("eyeX", "Servo", "Eyes pan servo");
		addPeer("eyeY", "Servo", "Eyes tilt servo");
		addPeer("rothead", "Servo", "Head pan servo");
		addPeer("neck", "Servo", "Head tilt servo");
		addPeer("rollNeck", "Servo", "rollNeck Mod servo");
		// addPeer("arduino", "Arduino", "Arduino controller for this arm");

		addPeer("eyelidLeft", "Servo", "eyelidLeft or both servo");
		addPeer("eyelidRight", "Servo", "Eyelid right servo");

	}

}
