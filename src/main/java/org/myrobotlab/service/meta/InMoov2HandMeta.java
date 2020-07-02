package org.myrobotlab.service.meta;

import org.myrobotlab.framework.Platform;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.meta.abstracts.MetaData;
import org.slf4j.Logger;

public class InMoov2HandMeta extends MetaData {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(InMoov2HandMeta.class);

	/**
	 * This class is contains all the meta data details of a service. It's peers,
	 * dependencies, and all other meta data related to the service.
	 * 
	 */
	public InMoov2HandMeta() {

		Platform platform = Platform.getLocalInstance();
		addDescription("an easier way to create gestures for InMoov");
		addCategory("robot");

		addPeer("thumb", "Servo", "Thumb servo");
		addPeer("index", "Servo", "Index servo");
		addPeer("majeure", "Servo", "Majeure servo");
		addPeer("ringFinger", "Servo", "RingFinger servo");
		addPeer("pinky", "Servo", "Pinky servo");
		addPeer("wrist", "Servo", "Wrist servo");
		addPeer("arduino", "Arduino", "Arduino controller for this hand");
		// Currently if the LeapMotion service is loaded and the jni is not there - the
		// whole jvm crashes :(
		// this should use pub sub and be less destructive !
		// addPeer("leap", "LeapMotion", "Leap Motion Service");

	}

}
