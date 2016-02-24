package org.myrobotlab.opencv;

import org.bytedeco.javacpp.opencv_core.Rect;

public class DetectedFace {

	public Rect face;
	public Rect leftEye;
	public Rect rightEye;
	public Rect mouth;
	// TODO: create some better label id to string mapping
	public int detectedLabelId;
	
	public void dePicaso() {
		// the face might be slightly scrabled. make sure the left eye 
		// is in the left socket.. and the right eye in the right socket.
		if (leftEye.x() > rightEye.x()) {
			// swap eyes!
			Rect tmp = leftEye;
			leftEye = rightEye;
			rightEye = tmp;
		}
	}
	
	public Rect getFace() {
		return face;
	}
	public void setFace(Rect face) {
		this.face = face;
	}
	public Rect getLeftEye() {
		return leftEye;
	}
	public void setLeftEye(Rect leftEye) {
		this.leftEye = leftEye;
	}
	public Rect getRightEye() {
		return rightEye;
	}
	public void setRightEye(Rect rightEye) {
		this.rightEye = rightEye;
	}
	public Rect getMouth() {
		return mouth;
	}
	public void setMouth(Rect mouth) {
		this.mouth = mouth;
	}
	public boolean isComplete() {
		// helper method to tell us if everything is set.
		return !((face == null) ||
				 (leftEye == null) || 
				 (rightEye == null) ||
				 (mouth == null)); 
	}
	public int getDetectedLabelId() {
		return detectedLabelId;
	}
	public void setDetectedLabelId(int detectedLabelId) {
		this.detectedLabelId = detectedLabelId;
	}
}

