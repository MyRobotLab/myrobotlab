package org.myrobotlab.service;

import org.myrobotlab.service.data.Rectangle;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.opencv.OpenCVFilterLKOpticalTrack;
import org.slf4j.Logger;

public class FindHuman extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(FindHuman.class
			.getCanonicalName());
	public Arduino arduino;
	public Speech speech;
	public Servo pan;
	public Servo tilt;
	public Twitter twitter;
	public OpenCV opencv;
	public PID xpid;
	public PID ypid;
	public Java java;
	public boolean tweetOn = true;
	public boolean raverLights = true;

	private int actservox = 90;
	private int actservoy = 90;
	private int frameSkip;
	private int frameSkipHuman;
	private boolean spokeSearch;
	private int x;
	private int y;
	private double dx = 90d;
	private double dy = 90d;
	private double rad = 0;
	private double dist = 2;
	private double raddir = .2d;
	private double distdir = .3;
	private double speed = 3d;
	private boolean speakOn = false;

	public FindHuman(String n) {
		super(n);

		// create services ==============================================
		Runtime.createAndStart("runtime", "Runtime");
		arduino = (Arduino) Runtime.createAndStart("arduino", "Arduino");
		speech = (Speech) Runtime.createAndStart("speech", "Speech");
		pan = (Servo) Runtime.createAndStart("pan", "Servo");
		tilt = (Servo) Runtime.createAndStart("tilt", "Servo");
		twitter = (Twitter) Runtime.createAndStart("twitter", "Twitter");
		opencv = (OpenCV) Runtime.create("opencv", "OpenCV");
		xpid = (PID) Runtime.createAndStart("xpid", "PID");
		ypid = (PID) Runtime.createAndStart("ypid", "PID");
		java = (Java) Runtime.createAndStart("java", "Java");
		Sphinx ear = (Sphinx) Runtime.createAndStart("sphinx", "Sphinx");

		// xpid ==============================================
		xpid.setMode(1);
		xpid.setOutputRange(-1, 1);
		xpid.setPID(7.0, 0.2, 0.5);
		xpid.setControllerDirection(0);
		xpid.setSetpoint(80);// #setpoint now is 80 instead of 160 because of 2
								// PD filters

		// ypid ==============================================
		ypid.setMode(1);
		ypid.setOutputRange(-1, 1);
		ypid.setPID(7.0, 0.2, 0.5);
		ypid.setControllerDirection(0);
		ypid.setSetpoint(60); // set point is now 60 instead of 120 because of 2
								// PD filters
		xpid.invert();

		// twitter ==============================================
//		 twitter.setSecurity("","","","");
		twitter.configure();
		// twitter.tweet("#myrobotlab is Awesome!")

		// arduino ==============================================
		//arduino.connect("/dev/ttyACM0", 57600, 8, 1, 0);
		arduino.connect("COM15");

		// opencv ==============================================
		opencv.startService();
		opencv.addFilter("Gray", "Gray");
		// add enough pyramid down filters to get 160x120 resolution
		// opencv.addFilter("PyramidDown1", "PyramidDown");
		// opencv.addFilter("PyramidDown2", "PyramidDown");
		opencv.addFilter("lk", "LKOpticalTrack");
		opencv.addListener("publishOpenCVData", this.getName(), "input",
				OpenCVData.class);
		opencv.setCameraIndex(1);
		 opencv.capture();

		// speech ======
		// speech.setFrontendType("MULTI");

		// ear ============
		ear.addCommand("find human", opencv.getName(), "capture");
		ear.addCommand("kill all humans", this.getName(), "stopEverything");
		ear.addCommand("obey me", this.getName(), "tweetOn");
		ear.addCommand("two two four", this.getName(), "tweetOff");
		ear.addCommand("arm weapons", this.getName(), "speechOn");
		ear.addCommand("stupid robot", this.getName(), "speechOff");
		ear.addComfirmations("yes");
		ear.addNegations("no");
		ear.attach(speech);
		ear.startListening();

		// now get the servos working ==========================================
		arduino.attach(pan.getName(), 14);
		arduino.attach(tilt.getName(), 15);
		pan.moveTo(90);
		tilt.moveTo(90);
		lights(0,1,0);

		// extra stuff to get 640x480 camera resolution - awesome code grabs a
		// reference to a private variable!
		// sleep(4000);
		// java.interpret("import com.googlecode.javacv.OpenCVFrameGrabber;import com.googlecode.javacv.cpp.opencv_highgui;import com.googlecode.javacv.cpp.opencv_highgui.CvCapture;");
		// java.interpret("capture=(CvCapture)(((Java)java).interpret(\"((OpenCVFrameGrabber)((OpenCV)opencv).getFrameGrabber()).capture\"));opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_HEIGHT, 320);opencv_highgui.cvSetCaptureProperty(capture, opencv_highgui.CV_CAP_PROP_FRAME_WIDTH, 240);");
	}

	public void stopEverything() {
		tweetOn = false;
		lights(1, 1, 1);
		opencv.stopCapture();
	}

	public void tweetOn() {
		tweetOn = true;
		lights(0, 0, 0);
	}

	public void tweetOff() {
		tweetOn = false;
		lights(1, 1, 1);
	}

	public void speechOn() {
		speakOn = true;
		lights(0, 0, 0);
	}

	public void speechOff() {
		speakOn = false;
		lights(1, 1, 1);
	}

	private void lights(int i, int j, int k) {
		if (!raverLights)
			return;
		arduino.digitalWrite(10, i);
		arduino.digitalWrite(9, j);
		arduino.digitalWrite(5, k);

	}

	// spaghetti code starts here, beware, danger lurks ahead!
	// this method catches the opencvData published from the tracking filters
	public void input(OpenCVData opencvData) {
		// move center around randomly a litle
		// dx += Math.random() * 2d - 1d;
		// dy += Math.random() * 2d - 1d;
		// if (dx < 40 || dx > 140) {
		// dx = 90d;
		// }
		// if (dy < 40 || dy > 140) {
		// dy = 90d;
		// }
		
		
		if ((opencvData.getSelectedFilterName().equals("lk")
				&& opencvData.getPoints() != null && opencvData.getPoints()
				.size() > 0)
				|| (opencvData.getBoundingBoxArray() != null && opencvData
						.getBoundingBoxArray().size() > 0)) {
			if (opencvData.getSelectedFilterName().equals("lk")) {
				// do lktracking
				x = (int) (opencvData.getPoints().get(0).x * 160f);
				y = (int) (opencvData.getPoints().get(0).y * 120f);
			} else {
				// or face detection
				Rectangle rect = opencvData.getBoundingBoxArray().get(0);
				x = (int)(rect.x + (rect.width / 2));
				y = (int)(rect.y + (rect.height / 2));
			}
			// back up a little bit, if you think you glanced a face
			if (frameSkipHuman == 0) {
				for (int i = 0; i < 6; i++) {
					dist -= distdir;
					raddir = ((1.0d - (Math.abs(dist) / 90d)) / speed)
							* (raddir / Math.abs(raddir));
					rad -= raddir;
				}
				raddir = -raddir;
				actservox = (int) ((double) dx + Math.sin(rad) * dist);
				actservoy = (int) ((double) dy + Math.cos(rad) * dist);
				pan.moveTo(actservox);
				tilt.moveTo(actservoy);
				frameSkipHuman++;
			} else {
				if (frameSkipHuman == 5) {
					if (speakOn)
						speech.speak("hello");
					spokeSearch = false;
					lights(1, 0, 1);
				}
				frameSkip = 0;
				frameSkipHuman += 1;
				// found human, send Twitter image and switch to lkoptical
				if (frameSkipHuman == 40) {

					lights(1, 1, 0);
					opencv.setDisplayFilter("input");
					// ========================================================
					if (tweetOn) {
						twitter.uploadImage(opencv.getDisplay(),
								"#myrobotlab Human Detected!");
						if (speakOn)
							speech.speak("tweet");
					} else {
						if (speakOn)
							speech.speak("lock");
					}
					// ========================================================
					opencv.removeFilter("FaceDetect");
					OpenCVFilterLKOpticalTrack jj = new OpenCVFilterLKOpticalTrack(
							"lk");
					opencv.addFilter(jj);
					jj.samplePoint(x, y);
					opencv.setDisplayFilter("input");
				}

				// move servos to keep tracking
				if (frameSkipHuman < 30 || frameSkipHuman > 51) {
					xpid.setInput(x);
					xpid.compute();
					actservox += xpid.getOutput();
					ypid.setInput(y);
					ypid.compute();
					actservoy += ypid.getOutput();
					pan.moveTo(actservox);
					tilt.moveTo(actservoy);
				}
			}
		} else {
			frameSkip += 1;
			// switch to searching if no tracking point is available...waits 10
			// frames first
			if (frameSkip > 10) {
				{
					frameSkipHuman = 0;
					if (!spokeSearch) {
						if (speakOn)
							speech.speak("scanning");
						opencv.removeFilter("lk");
						OpenCVFilterFaceDetect fd = (OpenCVFilterFaceDetect)opencv.addFilter("FaceDetect", "FaceDetect");
						fd.useFloatValues = false;
						opencv.setDisplayFilter("input");
						spokeSearch = true;
					}
				}
				// spiral search pattern below. uses the power of math
				actservox = (int) (dx + Math.sin(rad) * dist);
				actservoy = (int) (dy + Math.cos(rad) * dist);
				raddir = ((1.0d - (Math.abs(dist) / 90d)) / speed)
						* (raddir / Math.abs(raddir));
				rad += raddir;
				dist += distdir;
				if (actservox < 40 || actservox > 140 || actservoy < 40
						|| actservoy > 140 || dist < 2) {
					distdir = -distdir;
					dist += distdir;
					dist += distdir;
				}
				pan.moveTo(actservox);
				tilt.moveTo(actservoy);
				lights(0, 0, 1);
				x = actservox;
				y = actservoy;
			}
		}
	}

	@Override
	public String getDescription() {
		return "Find Human pan/tilt camera";
	}

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public void releaseService() {
		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);
		FindHuman findhuman = new FindHuman("findhuman");
		findhuman.startService();
		Runtime.createAndStart("runtime", "Runtime");
		Runtime.createAndStart("gui", "GUIService");
	}
}
