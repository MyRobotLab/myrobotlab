package org.myrobotlab.openni;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.GestureRecognition;
import org.slf4j.Logger;

import SimpleOpenNI.SimpleOpenNI;

public class PApplet {
	// FIXME - EXTRACT INTERFACE ----
	private GestureRecognition gr = null;

	public Object g;
	public final static Logger log = LoggerFactory.getLogger(PApplet.class);
	
	public PApplet(GestureRecognition gr) {
		this.gr = gr;
	}

	public String dataPath(String recordPath) {
		log.info("dataPath");
		return null;
	}

	public void registerDispose(SimpleOpenNI simpleOpenNI) {
		log.info("registerDispose");
	}

	public void createPath(String path) {
		log.info("createPath");
	}

	public void line(Object x, Object y, Object x2, Object y2) {
		log.info(String.format("line %f %f %f %f", x, y, x2, y2));
		gr.line((Float)x, (Float)y, (Float)x2, (Float)y2);
	}

	static public final float sqrt(float a) {
		return (float) Math.sqrt(a);
	}
	
	// FIXME - EXTRACT USER INTERFACE BEGIN ----
	public void onNewUser(SimpleOpenNI openni, int userId){
		gr.onNewUser(openni, userId);
	}
	
	public void onLostUser(SimpleOpenNI openni, int userId){
		gr.onLostUser(openni, userId);
	}
	
	public void onOutOfSceneUser(SimpleOpenNI openni, int userId){
		
	}

	public void onNewHand(SimpleOpenNI openni, int userId, PVector v){
		log.info("here");
		//gr.onNewHand(openni, userId);
		//gr.on
	}
	public void onTrackedHand(SimpleOpenNI openni, int userId){
		log.info("here");
		gr.onNewUser(openni, userId);
		//gr.on
	}

}
