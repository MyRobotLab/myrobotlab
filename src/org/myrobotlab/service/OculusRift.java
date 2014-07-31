package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.HmdDesc;
import com.oculusvr.capi.OvrLibrary;
import com.oculusvr.capi.SensorState;

public class OculusRift extends Service {

	private static final long serialVersionUID = 1L;
	private static final float RAD_TO_DEGREES = 57.2957795F;
	public final static Logger log = LoggerFactory.getLogger(OculusRift.class);
	protected Hmd hmd;
	private boolean initialized = false;
	
	public OculusRift(String reservedKey) {
		super(reservedKey);
	}
	
	@Override
	public void startService() {
		super.startService();
		initContext();
	}
	
	private void initContext() {

		if (!initialized) {
			
			log.info("Init the rift.");

			OvrLibrary.INSTANCE.ovr_Initialize();
			hmd = Hmd.create(0); 
			
		  	int requiredSensorCaps = 0;
		  	int supportedSensorCaps = OvrLibrary.ovrSensorCaps.ovrSensorCap_Orientation;
		  	
		  	// TODO: what errors/exceptions might be thrown here?  not sure how JNA exposes that info.
		  	hmd.startSensor(supportedSensorCaps, requiredSensorCaps);
		  	log.info("Created HMD Oculus Rift Sensor");
			initialized = true;
		} else {
			log.info("Rift interface already initialized.");
		}

	}

	
	@Override
	public void stopService() {
		super.stopService();
		// TODO: validate proper life cycle.
		hmd.stopSensor();
		hmd.destroy();
	}
	
	
	public void resetSensor() {
		//hmd.
		if (initialized) {
			hmd.resetSensor();
		} else {
			log.info("Sensor not initalized.");
		}
	}
	
	public void logOrientation() {
  		SensorState ss = hmd.getSensorState(0);
  		float w = ss.Recorded.Pose.Orientation.w;
  		float x = ss.Recorded.Pose.Orientation.x;
  		float y = ss.Recorded.Pose.Orientation.y;
  		float z = ss.Recorded.Pose.Orientation.z;
  		log.info("Roll: " + z*RAD_TO_DEGREES);
  		log.info("Pitch:"+ x*RAD_TO_DEGREES);
  		log.info("Yaw:"+ y*RAD_TO_DEGREES );
	}
	
	public float getYaw() {
  		SensorState ss = hmd.getSensorState(0);
  		float y = ss.Recorded.Pose.Orientation.y * RAD_TO_DEGREES;
  		return y;
	}

	public float getRoll() {
  		SensorState ss = hmd.getSensorState(0);
  		float z = ss.Recorded.Pose.Orientation.z * RAD_TO_DEGREES;
  		return z;
	}
	
	public float getPitch() {
  		SensorState ss = hmd.getSensorState(0);
  		float x = ss.Recorded.Pose.Orientation.x * RAD_TO_DEGREES;
  		return x;
	}
	
	@Override
	public String getDescription() {
		// 
		return "The Oculus Rift Head Tracking Service";
	}
	
	public static void main(String s[]) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel("INFO");
		Runtime.createAndStart("gui", "GUIService");
		Runtime.createAndStart("python", "Python");
		OculusRift rift = (OculusRift) Runtime.createAndStart("oculus", "OculusRift");
		rift.logOrientation();
	}

}
