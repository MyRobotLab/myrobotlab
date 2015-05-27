package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.kinematics.Point;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.FirmwareVersion;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.Vector3;
import com.thalmic.myo.enums.Arm;
import com.thalmic.myo.enums.PoseType;
import com.thalmic.myo.enums.UnlockType;
import com.thalmic.myo.enums.VibrationType;
import com.thalmic.myo.enums.XDirection;
import com.thalmic.myo.example.DataCollector;

public class MyoThalmic extends Service implements DeviceListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(MyoThalmic.class);

	static final int SCALE = 18;
	double rollW;
	double pitchW;
	double yawW;
	transient Pose currentPose;
	transient Arm whichArm;
	
	transient Myo myo = null; 
	transient Hub hub = null;
	transient HubThread hubThread = null;
	
	class HubThread extends Thread {
		public boolean running = false;
		MyoThalmic myService = null;
		
		public HubThread(MyoThalmic myService){
			this.myService = myService;
		}
		
		public void run(){
			running = true;
			while (running) {
				hub.run(1000 / 20);
				log.info(myService.toString());
			}
		}
	}
	
	public void disconnect(){
		if (hubThread != null){
			hubThread.running = false;
			hubThread = null;
		}
	}

	public void connect() {

		hub = new Hub("com.example.hello-myo");

		System.out.println("Attempting to find a Myo...");
		log.info("Attempting to find a Myo");
		myo = hub.waitForMyo(10000);

		if (myo == null) {
			// throw new RuntimeException("Unable to find a Myo!");
			log.info("Unable to find a Myo");
		}

		System.out.println("Connected to a Myo armband!");
		log.info("Connected to a Myo armband");
		hub.addListener(this);
		
		if (hubThread == null){
			hubThread = new HubThread(this);
			hubThread.start();
		}

	}

	public MyoThalmic(String n) {
		super(n);
 
		pitchW = 0;
		yawW = 0;
		currentPose = new Pose();
	}

	@Override
	public String[] getCategories() {
		return new String[] { "general" };
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	@Override
	public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {

		Quaternion normalized = rotation.normalized();

		double roll = Math.atan2(2.0f * (normalized.getW() * normalized.getX() + normalized.getY() * normalized.getZ()),
				1.0f - 2.0f * (normalized.getX() * normalized.getX() + normalized.getY() * normalized.getY()));
		double pitch = Math.asin(2.0f * (normalized.getW() * normalized.getY() - normalized.getZ() * normalized.getX()));
		double yaw = Math.atan2(2.0f * (normalized.getW() * normalized.getZ() + normalized.getX() * normalized.getY()),
				1.0f - 2.0f * (normalized.getY() * normalized.getY() + normalized.getZ() * normalized.getZ()));

		rollW = ((roll + Math.PI) / (Math.PI * 2.0) * SCALE);
		pitchW = ((pitch + Math.PI / 2.0) / Math.PI * SCALE);
		yawW = ((yaw + Math.PI) / (Math.PI * 2.0) * SCALE);
	}

	@Override
	public void onPose(Myo myo, long timestamp, Pose pose) {
		currentPose = pose;
		if (currentPose.getType() == PoseType.FIST) {
			myo.vibrate(VibrationType.VIBRATION_MEDIUM);
		}		
		invoke("publishPose", pose);
	}
	
	public void addPoseListener(Service service){
		addListener("publishPose", service.getName(), "onPose", Pose.class);
	}
	
	public Pose publishPose(Pose pose){
		return pose;
	}

	@Override
	public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
		whichArm = arm;
	}

	@Override
	public void onArmUnsync(Myo myo, long timestamp) {
		whichArm = null;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("\r");

		String xDisplay = String.format("[%s%s]", repeatCharacter('*', (int) rollW), repeatCharacter(' ', (int) (SCALE - rollW)));
		String yDisplay = String.format("[%s%s]", repeatCharacter('*', (int) pitchW), repeatCharacter(' ', (int) (SCALE - pitchW)));
		String zDisplay = String.format("[%s%s]", repeatCharacter('*', (int) yawW), repeatCharacter(' ', (int) (SCALE - yawW)));

		String armString = null;
		if (whichArm != null) {
			armString = String.format("[%s]", whichArm == Arm.ARM_LEFT ? "L" : "R");
		} else {
			armString = String.format("[?]");
		}
		String poseString = null;
		if (currentPose != null) {
			String poseTypeString = currentPose.getType().toString();
			poseString = String.format("[%s%" + (SCALE - poseTypeString.length()) + "s]", poseTypeString, " ");
		} else {
			poseString = String.format("[%14s]", " ");
		}
		builder.append(xDisplay);
		builder.append(yDisplay);
		builder.append(zDisplay);
		builder.append(armString);
		builder.append(poseString);
		return builder.toString();
	}

	public String repeatCharacter(char character, int numOfTimes) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < numOfTimes; i++) {
			builder.append(character);
		}
		return builder.toString();
	}

	@Override
	public void onPair(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnpair(Myo myo, long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnect(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDisconnect(Myo myo, long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onUnlock(Myo myo, long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onLock(Myo myo, long timestamp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAccelerometerData(Myo myo, long timestamp, Vector3 accel) {
		// TODO Auto-generated method stub
		
	}
	
	

	@Override
	public void onGyroscopeData(Myo myo, long timestamp, Vector3 gyro) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRssi(Myo myo, long timestamp, int rssi) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEmgData(Myo myo, long timestamp, byte[] emg) {
		// TODO Auto-generated method stub
		
	}
	
	public void lock() {
		myo.lock();
	}
	
	public void unlock(){
		myo.unlock(UnlockType.UNLOCK_TIMED);
	}
	
	/*
	public void setLockingPolicy(String policy){
		myo.setL
		myo.setLockingPolicy("none") ;
	}
	*/
	
	////
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		try {

			MyoThalmic myo = (MyoThalmic) Runtime.start("myo", "MyoThalmic");
			myo.test();

			Hub hub = new Hub("com.example.hello-myo");

			System.out.println("Attempting to find a Myo...");
			log.info("Attempting to find a Myo");

			Myo myodevice = hub.waitForMyo(10000);

			if (myodevice == null) {
				throw new RuntimeException("Unable to find a Myo!");
			}

			System.out.println("Connected to a Myo armband!");
			log.info("Connected to a Myo armband");
			DeviceListener dataCollector = new DataCollector();
			hub.addListener(dataCollector);

			while (true) {
				hub.run(1000 / 20);
				System.out.print(dataCollector);

				Runtime.start("gui", "GUIService");

			}
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
