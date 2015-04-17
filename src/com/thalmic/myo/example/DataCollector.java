package com.thalmic.myo.example;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.enums.Arm;
import com.thalmic.myo.enums.PoseType;
import com.thalmic.myo.enums.VibrationType;
import com.thalmic.myo.enums.XDirection;

public class DataCollector extends AbstractDeviceListener {
    private static final int SCALE = 18;
    private double rollW;
    private double pitchW;
    private double yawW;
    private Pose currentPose;
    private Arm whichArm;

    public DataCollector() {
	rollW = 0;
	pitchW = 0;
	yawW = 0;
	currentPose = new Pose();
    }

    @Override
    public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
	Quaternion normalized = rotation.normalized();

	double roll = Math.atan2(2.0f * (normalized.getW() * normalized.getX() + normalized.getY() * normalized.getZ()), 1.0f - 2.0f * (normalized.getX() * normalized.getX() + normalized.getY() * normalized.getY()));
	double pitch = Math.asin(2.0f * (normalized.getW() * normalized.getY() - normalized.getZ() * normalized.getX()));
	double yaw = Math.atan2(2.0f * (normalized.getW() * normalized.getZ() + normalized.getX() * normalized.getY()), 1.0f - 2.0f * (normalized.getY() * normalized.getY() + normalized.getZ() * normalized.getZ()));

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
	    String poseTypeString = currentPose.getType()
		    .toString();
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

    private String repeatCharacter(char character, int numOfTimes) {
	StringBuilder builder = new StringBuilder();
	for (int i = 0; i < numOfTimes; i++) {
	    builder.append(character);
	}
	return builder.toString();
    }
}