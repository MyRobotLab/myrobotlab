package org.myrobotlab.headtracking;

import java.io.Serializable;
import java.util.SimpleTimeZone;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OculusRift;
import org.myrobotlab.service.data.OculusData;
import org.slf4j.Logger;

import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.SensorState;

public class OculusHeadTracking implements Runnable, Serializable {

	public final static Logger log = LoggerFactory.getLogger(OculusHeadTracking.class);
	private static final long serialVersionUID = -4067064437788846187L;
	protected final Hmd hmd;
	boolean running = false;
	transient public OculusRift oculus;
	transient Thread trackerThread = null;
	
	public OculusHeadTracking(Hmd hmd) {
		// TODO Auto-generated constructor stub
		this.hmd = hmd;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
		running = true;
		while (running) {
	  		SensorState ss = hmd.getSensorState(0);
	  		double w = Math.toDegrees(ss.Recorded.Pose.Orientation.w);
	  		// rotations about x axis  (pitch)
	  		double pitch = Math.toDegrees(ss.Recorded.Pose.Orientation.x);
	  		// rotation about y axis (yaw)
	  		double yaw = Math.toDegrees(ss.Recorded.Pose.Orientation.y);
	  		// rotation about z axis (roll)
	  		double roll = Math.toDegrees(ss.Recorded.Pose.Orientation.z);
	  		
			// log.info("Roll: " + z*RAD_TO_DEGREES);
			// log.info("Pitch:"+ x*RAD_TO_DEGREES);
			// log.info("Yaw:"+ y*RAD_TO_DEGREES );
	  		
	  		OculusData headTrackingData = new OculusData(roll, pitch, yaw);
	  		oculus.invoke("publishOculusData", headTrackingData);
	  		
	  		try {
	  			// TODO: should we have a minor pause here?
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				// oops.. bomb out.
				break;
			}
	  		
	  		
	  		
		}
		
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public OculusRift getOculus() {
		return oculus;
	}

	public void setOculus(OculusRift oculus) {
		this.oculus = oculus;
	}

	public void start() {
		log.info("starting head tracking");
		if (trackerThread != null) {
			log.info("video processor already started");
			return;
		}
		trackerThread = new Thread(this, String.format("%s_oculusHeadTracking", oculus.getName()));
		trackerThread.start();
 	}

	public void stop() {
		// TODO Auto-generated method stub
		log.debug("stopping head tracking");
        running = false;
        trackerThread = null;
	}

}
