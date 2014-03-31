package org.myrobotlab.service;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_TM_SQDIFF;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.myrobotlab.fileLib.FileIO;
import org.myrobotlab.fileLib.FindFile;
import org.myrobotlab.framework.Service;
import org.myrobotlab.image.KinectImageNode;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.NodeDeprecate;
import org.myrobotlab.opencv.OpenCVFilterKinectDepthMask;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

/*
 * TODO - 
 * Test AFFIRMATION mode after success or non success
 */

public class FSMTest extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(FSMTest.class.getCanonicalName());

	// Context related
	String context = null; // current context identifier
	String contextPerson = null; // person of current context
	String lastAssociativeWord = null;

	// state info
	boolean isSpeaking = false;
	HashMap<String, NodeDeprecate> memory = new HashMap<String, NodeDeprecate>();

	// necessary services
	OpenCV opencv = null;
	Sphinx speechRecognition = null;
	Speech speech = null;
	GUIService gui = null;
	Arduino arduino = null;
	Motor right = null;

	// random generator
	Random generator = new Random();

	HashMap<String, HashMap<String, String>> phrases = new HashMap<String, HashMap<String, String>>();

	OpenCVFilterKinectDepthMask filter = null; // direct handle to filter

	// findObject
	// segmentation - run kinect at ramping range
	// color hue
	// lk track
	// identifyObject
	// reportObject
	// resolveObject - 2 objects - ask incrementally - send mail

	public FSMTest(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

	public void init() {
		speechRecognition = new Sphinx("sphinx");
		speechRecognition.startService();
		speech = new Speech("speech");
		speech.setBackendType(Speech.BACKEND_TYPE_GOOGLE);
		speech.startService();
		speech.setLanguage("en");
		opencv = new OpenCV("opencv");
		opencv.startService();
		gui = new GUIService("gui");
		gui.startService();

		speech.addListener("isSpeaking", getName(), "isSpeaking");

		speechRecognition.addListener("recognized", getName(), "heard", String.class);

		opencv.addListener("publish", getName(), "publish", KinectImageNode.class); // <---
																					// BUG
																					// -
																					// polygon,
																					// getName()
																					// (only
																					// should
																					// work)
		opencv.addListener("publishIplImageTemplate", getName(), "getImageTemplate", IplImage.class);
		opencv.addListener("publishIplImage", getName(), "publishIplImage", IplImage.class);

		// filter setup
		opencv.videoProcessor.getDepth = true;
		opencv.addFilter("KinectDepthMask1", "KinectDepthMask");
		filter = (OpenCVFilterKinectDepthMask) opencv.getFilter("KinectDepthMask1");

		// start vision
		opencv.videoProcessor.grabberType = "com.googlecode.javacv.OpenKinectFrameGrabber";
		
		opencv.capture();

		initPhrases();
		changeState(IDLE);
		speech.speak("my mouth is working");
		speech.speak("my eyes are open");
	}

	/**
	 * @param speaking
	 *            Event function - necessary to NOT listen while speaking.
	 *            Otherwise, robot gets confused when processing own speech as
	 *            commands. true = should not process listening events false =
	 *            may process listening events
	 */
	public void isSpeaking(Boolean speaking) {
		isSpeaking = speaking;
		log.error("isSpeaking" + speaking);
	}

	/*
	 * Context States language keys semantic phrases are stored in a structure
	 * simplified meaning can quickly be derived depending on what context key
	 * is supplied e.g.
	 * 
	 * phrases.get(FIND_OBJECT).containsKey(input) - can be a test if other
	 * input is in BagOfPhrases
	 */

	// TODO - organize & find patterns in the states
	public final static String FIND_OBJECT = "look"; // actor
	public final static String HELLO = "hello"; // response
	public final static String YES = "yes"; // response
	public final static String NO = "no"; // response
	public final static String I_AM_NOT_SURE = "i am not sure"; // response
	public final static String I_DO_NOT_UNDERSTAND = "i do not understand";
	public final static String GET_ASSOCIATIVE_WORD = "get associative word";
	public final static String QUERY_OBJECT = "i do not know what it is can you tell me";
	public final static String WAITING_FOR_POLYGONS = "i am waiting for polygons";
	public final static String IDLE = "i am in an idle state";
	public final static String HAPPY = "i am happy";
	public final static String SAD = "i am bummed";
	public final static String FOUND_POLYGONS = "i have found polygons";
	public final static String GET_CAMERA_FRAME = "i am getting an image";
	public final static String WAITING_FOR_AFFIRMATION = "is that correct?";

	public final static String UNKNOWN = "i don't know";

	public void initPhrases() {
		// load recognized grammar - keep in sync with simpl.gram
		// TODO - dynamically create a simple.gram file? vs programatically
		// change it??
		// ------------------ SIMPLE.GRAM SYNC BEGIN --------------------------
		HashMap<String, String> t = new HashMap<String, String>();
		t.put("find object", null);
		t.put("look", null);
		t.put("what is this", null);
		t.put("what do you see", null);
		t.put("and this", null);
		t.put("what about this", null);
		t.put("do you know what this is", null);
		phrases.put(FIND_OBJECT, t);

		t = new HashMap<String, String>();
		t.put("cup", null);
		t.put("measuring thingy", null);
		t.put("beer", null);
		t.put("box", null);
		t.put("hand", null);
		t.put("cup", null);
		t.put("guitar", null);
		t.put("phone", null);
		t.put("bucket", null);
		t.put("ball", null);
		// t.put("apple", null);
		// t.put("orange", null);
		// t.put("frits", null);
		// t.put("odd bot", null);
		// t.put("tin head", null);
		// t.put("chris the carpenter", null);
		phrases.put(GET_ASSOCIATIVE_WORD, t);
		// ------------------ SIMPLE.GRAM SYNC END --------------------------

		t = new HashMap<String, String>();
		t.put("i am looking and waiting", null);
		t.put("i am trying to see an object", null);
		phrases.put(WAITING_FOR_POLYGONS, t);

		t = new HashMap<String, String>();
		t.put("i have found something", null);
		t.put("i can see some object", null);
		t.put("there is an object", null);
		t.put("i see something", null);
		phrases.put(FOUND_POLYGONS, t);

		t = new HashMap<String, String>();
		t.put("i dont know. please tell me", null);
		t.put("can you please tell me what it is", null);
		t.put("please tell me what it is", null);
		t.put("what is it", null);
		t.put("would you tell me what that is", null);
		t.put("i do not recognize it. could you tell me", null);
		t.put("i wish i knew", null);
		t.put("what would you call it", null);
		t.put("i dont know. please tell me", null);
		t.put("i have never seen one of those before. what is it", null);
		phrases.put(QUERY_OBJECT, t);

		t = new HashMap<String, String>();
		t.put("hello", null);
		t.put("greetings", null);
		t.put("yes hello", null);
		t.put("hi there", null);
		t.put("good morning", null);
		phrases.put(HELLO, t);

		t = new HashMap<String, String>();
		t.put("no", null);
		t.put("i do not think so", null);
		t.put("no way", null);
		t.put("nope", null);
		t.put("i doubt it", null);
		phrases.put(NO, t);

		t = new HashMap<String, String>();
		t.put("yes", null);
		t.put("i believe so", null);
		t.put("most certainly", null);
		t.put("yep", null);
		t.put("affirmative", null);
		t.put("correct", null);
		t.put("yes of course", null);
		t.put("yeah", null);
		phrases.put(YES, t);

		t = new HashMap<String, String>();
		t.put(IDLE, null);
		t.put("i am at rest", null);
		t.put("i have stopped", null);
		t.put("i am ready", null);
		t.put("i am calm and will be listening for your next command", null);
		t.put("i am zen", null);
		t.put("i am very still", null);
		phrases.put(IDLE, t);

		t = new HashMap<String, String>();
		t.put(WAITING_FOR_AFFIRMATION, null);
		t.put("i that right?", null);
		t.put("am i right?", null);
		phrases.put(WAITING_FOR_AFFIRMATION, t);

		t = new HashMap<String, String>();
		t.put(HAPPY, null);
		t.put("great", null);
		t.put("wonderful", null);
		t.put("fabulous", null);
		t.put("kickass", null);
		t.put("i am rockin", null);
		t.put("that makes me feel good", null);
		t.put("excellent", null);
		phrases.put(HAPPY, t);

		t = new HashMap<String, String>();
		t.put(SAD, null);
		t.put("great", null);
		t.put("wonderful", null);
		t.put("fabulous", null);
		t.put("kickass", null);
		t.put("i am rockin", null);
		t.put("that makes me feel good", null);
		t.put("excellent", null);
		phrases.put(SAD, t);

	}

	public void heard(String data) {
		if (isSpeaking) {
			log.error("heard " + data + ", but I am speaking - not going to act on this");
			return;
		}

		if ("english".equals(data) || "danish".equals(data) || "dutch".equals(data) || "portuguese".equals(data) || "japanese".equals(data)) {
			speech.setLanguage(Speech.googleLanguageMap.get(data));
			speech.speak("i will speak " + data);
			return;
		}

		// if (phrases.get(HELLO).containsKey(data))
		if ("hello audrey".equals(data)) {
			speech.speak(getPhrase(HELLO));
			return;
		}

		if (data.equals("save")) {
			saveMemory();
			speech.speak("my memory has been saved");
			return;
		}

		if (data.equals("stop")) {
			context = IDLE;
			speech.speak(getPhrase(IDLE));
			return;
		}

		if (data.equals("context")) {
			speech.speak("my current context is " + context);
			return;
		}

		if (phrases.get(FIND_OBJECT).containsKey(data)) {
			findKinectPolygons();
			// changes -> WAITING_FOR_POLYGONS -> (once found) ->
			// FOUND_POLYGONS ->(processPolygons)->
			// (IDLE || GET_ASSOCIATIVE_WORD || GET_AFFIRMATION)
		} else if (context.equals(GET_ASSOCIATIVE_WORD) && phrases.get(GET_ASSOCIATIVE_WORD).containsKey(data)) {

			speech.speak("i will associate this with " + data);
			NodeDeprecate n = memory.get(UNKNOWN);
			log.error(n.imageData.get(0).cvBoundingBox + "," + n.imageData.get(0).boundingBox);
			n = memory.remove(UNKNOWN); // TODO - work with multiple unknowns
			log.error(n.imageData.get(0).cvBoundingBox + "," + n.imageData.get(0).boundingBox);
			n.word = data;
			if (!memory.containsKey(n.word)) {
				// i have learned something new
				speech.speak("i have learned something new");
				memory.put(data, n);
			} else {
				// i have bound it to something i previously new about
				speech.speak("i have categorized it under " + n.word);
				NodeDeprecate n2 = memory.get(n.word);
				n2.imageData.add(n.imageData.get(0)); // FIXME - messy
			}
			// speech.speak("i have " + memory.size() + " thing" +
			// ((memory.size()>1)?"s":"" + " in my memory"));
			lastAssociativeWord = n.word;
			changeState(IDLE);
		} else {
			speech.speak("i do not understand. we were in context " + context + " but you said " + data);
		}

		if (phrases.get(YES).containsKey(data) && context.equals(WAITING_FOR_AFFIRMATION)) {
			speech.speak(getPhrase(HAPPY));
		}

		// result of the computer incorrectly guessing and associating object
		// need to back out the change - guess only happens if there is a
		// pre-existing memory object - so the image data must be deleted and a
		// new UNKOWN object put back in
		if (phrases.get(NO).containsKey(data) && context.equals(WAITING_FOR_AFFIRMATION)) {
			speech.speak(getPhrase(SAD));
			// remove last KinectImageData from the "contextWord"
			// moving node out of word context and into the UNKNOWN
			// changing state back to GET_ASSOCIATIVE_WORD
			NodeDeprecate n = memory.get(lastAssociativeWord);
			// remove last image data
			KinectImageNode kin = n.imageData.remove(n.imageData.size() - 1);
			NodeDeprecate unknown = new NodeDeprecate();
			unknown.word = UNKNOWN;
			unknown.imageData.add(kin);
			memory.put(UNKNOWN, unknown);
			// try again - addListener ready for correct identification
			speech.speak(getPhrase(QUERY_OBJECT));
			changeState(GET_ASSOCIATIVE_WORD);
		}

	}

	public void findKinectPolygons() {
		filter.publishNodes = true;
		changeState(WAITING_FOR_POLYGONS);
	}

	public synchronized void publish(ArrayList<KinectImageNode> p) {
		log.error("found " + p.size() + " contextImageDataObjects");
		filter.publishNodes = false;

		// replacing all with current set - in future "unknown" objects can be
		// concatenated
		// / you could further guard by a new context
		if (context.equals(WAITING_FOR_POLYGONS)) {
			// invoking occurs on the same thread....
			// this "should" be thread safe with the syncrhonized call
			// invoke("changeState", FOUND_POLYGONS);
			changeState(FOUND_POLYGONS);

			NodeDeprecate n = new NodeDeprecate();
			n.word = UNKNOWN;
			n.imageData = p;
			memory.put(UNKNOWN, n);

			// the data arrives on the InBox (from the VideoProcessor Thread)
			// the processing of the InBox message is done by the FSMTest thread
			// which invoked by the Message call processPolygons

			processPolygons();
		}
	}

	public void processPolygons() {
		NodeDeprecate object = memory.get(UNKNOWN);
		invoke("", object);

		if (object.imageData.size() != 1) {
			speech.speak("i do not know how to deal with " + object.imageData.size() + " thing" + ((object.imageData.size() == 1) ? "" : "s yet"));
			changeState(IDLE);
			return;
		}

		// matchTemplate - adaptive match - non-match
		if (memory.size() == 1) // unknown objects only
		{
			// speech.speak("my memory is empty, except for the unknown");
			speech.speak(getPhrase(QUERY_OBJECT)); // need input from user
			changeState(GET_ASSOCIATIVE_WORD);
			return;
		}

		// run through - find best match - TODO - many other algorithms and
		// techniques
		Iterator<String> itr = memory.keySet().iterator();
		NodeDeprecate unknown = memory.get(UNKNOWN);
		log.error(unknown.imageData.get(0).cvBoundingBox.toString());
		log.error(unknown.imageData.get(0).boundingBox.toString());
		int bestFit = 1000;
		int fit = 0;
		String bestFitName = null;
		Integer index = new Integer(0);

		while (itr.hasNext()) {
			String n = itr.next();
			if (n.equals(UNKNOWN)) {
				continue; // we won't compare the unknown thingy with itself
			}
			NodeDeprecate toSearch = memory.get(n);
			fit = match(toSearch, unknown, index);

			toSearch.imageData.get(0).lastGoodFitIndex = fit;

			if (fit < bestFit) {
				bestFit = fit;
				bestFitName = n;
			}
		}

		log.error("bestFit" + bestFit);

		if (bestFit < 100) {
			// if found
			// announce - TODO - add map "i think it might be", i'm pretty sure
			// its a,
			speech.speak("i think it is a " + bestFitName);
			NodeDeprecate n = memory.get(bestFitName);
			n.imageData.add(unknown.imageData.get(0)); // FIXME - messy
			// with a match ratio of ....
			// is that correct?
			context = WAITING_FOR_AFFIRMATION;
			// publish index bestFitName bestFit
			// invoke("publishVideo0", memory);
			invoke("publishMatch", new MatchResult(n.word, bestFit, n.imageData.get(index), n.imageData.get(n.imageData.size() - 1)));

		} else {
			// else
			// associate word
			speech.speak("i do not know what it is");
			speech.speak(getPhrase(QUERY_OBJECT));
			changeState(GET_ASSOCIATIVE_WORD);
		}
	}

	IplImage result = null;
	double[] minVal = new double[1];
	double[] maxVal = new double[1];
	CvPoint minLoc = new CvPoint();
	CvPoint maxLoc = new CvPoint();
	CvPoint tempRect0 = new CvPoint();
	CvPoint tempRect1 = new CvPoint();

	int resultWidth = 0;
	int resultHeight = 0;

	// FIXME - bury in KinectDepthMask or other OpenCV filter to
	// get it working on the same thread only ...
	// Don't use CVObjects out of OpenCV
	int match(NodeDeprecate toSearch, NodeDeprecate unknown, Integer index) {

		// at the moment only uses one unknown image
		KinectImageNode templateImageData = unknown.imageData.get(0);
		IplImage template = templateImageData.cvCameraFrame;

		int bestFit = Integer.MAX_VALUE;

		log.error("searching through " + toSearch.imageData.size() + " " + toSearch.word + " images ");
		// iterate through the list of known templates
		for (int i = 0; i < toSearch.imageData.size(); ++i) {

			KinectImageNode imageData = toSearch.imageData.get(i);
			IplImage frame = imageData.cvCameraFrame;

			// TODO adaptive ROI in toSearch images
			// unfortunately cvMatchTemplate has to have the template smaller
			// than the
			// toSearchImage - without this, it is likely that plane surfaced
			// items will
			// match a wall or other large plain expanse

			CvRect searchROI = new CvRect();
			searchROI.x(imageData.boundingBox.x);
			searchROI.y(imageData.boundingBox.y);
			searchROI.width(imageData.boundingBox.width);
			searchROI.height(imageData.boundingBox.height);

			// TODO - dynamic adjustments will explode if on the edge
			// adaptive search area begin
			if (templateImageData.boundingBox.width > imageData.boundingBox.width) {
				searchROI.width(templateImageData.boundingBox.width + 2);
				searchROI.x(imageData.boundingBox.x - ((templateImageData.boundingBox.width - imageData.boundingBox.width) / 2));
			}

			if (templateImageData.boundingBox.height > imageData.boundingBox.height) {
				searchROI.height(templateImageData.boundingBox.height + 2);
				searchROI.y(imageData.boundingBox.y - ((templateImageData.boundingBox.height - imageData.boundingBox.height) / 2));
			}
			// adaptive search area end

			// create result area - TODO - can this be done with ROI
			// (optimization)
			resultWidth = searchROI.width() - (int) unknown.imageData.get(0).boundingBox.getWidth() + 1;
			resultHeight = searchROI.height() - (int) unknown.imageData.get(0).boundingBox.getHeight() + 1;

			if (result == null || resultWidth != result.width() || resultHeight != result.height()) {
				result = cvCreateImage(cvSize(resultWidth, resultHeight), IPL_DEPTH_32F, 1);
			}

			// set roi for the toSearch

			cvSetImageROI(frame, searchROI);

			// set roi of template for cropped template
			CvRect tmplROI = new CvRect();
			tmplROI.x(templateImageData.boundingBox.x);
			tmplROI.y(templateImageData.boundingBox.y);
			tmplROI.width(templateImageData.boundingBox.width);
			tmplROI.height(templateImageData.boundingBox.height);
			cvSetImageROI(template, tmplROI);

			cvMatchTemplate(frame, template, result, CV_TM_SQDIFF);
			cvResetImageROI(template);
			cvResetImageROI(frame);
			cvMinMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);

			// TODO - refactor
			tempRect0.x(minLoc.x());
			tempRect0.y(minLoc.y());
			tempRect1.x(minLoc.x() + template.width());
			tempRect1.y(minLoc.y() + template.height());

			int matchRatio = (int) (minVal[0] / ((tempRect1.x() - tempRect0.x()) * (tempRect1.y() - tempRect0.y())));
			if (matchRatio < bestFit) {
				bestFit = matchRatio;
				index = i;
			}
			log.error("image " + i + " match ratio " + matchRatio);
		}

		log.error("bestFit=" + bestFit);
		return bestFit;
	}

	/*
	 * TODO - add publishing points for image review back to the FSM Gui
	 */
	// -------------- CALLBACKS BEGIN -------------------------
	public CvPoint publish(CvPoint p) {
		log.info("got point " + p);
		return p;
	}

	public HashMap<String, NodeDeprecate> publishVideo0(HashMap<String, NodeDeprecate> memory) {
		return memory;
	}

	public class MatchResult {
		public String word;
		public KinectImageNode bestFit;
		public KinectImageNode newImage;
		public int matchIndex;

		MatchResult(String word, int matchIndex, KinectImageNode bestFit, KinectImageNode newImage) {
			this.word = word;
			this.bestFit = bestFit;
			this.newImage = newImage;
			this.matchIndex = matchIndex;
		}
	}

	public MatchResult publishMatch(MatchResult result) {
		return result;
	}

	// event to clear the GUIService's FSMTest video
	public void clearVideo0() {
	}

	public NodeDeprecate publishVideo0(NodeDeprecate o) {
		return o;
	}

	public NodeDeprecate publishVideo1(NodeDeprecate o) {
		return o;
	}

	public NodeDeprecate publishVideo2(NodeDeprecate o) {
		return o;
	}

	public IplImage publishMatchResult(IplImage o) {
		log.info("publishMatchResult" + o);
		return o;
	}

	public String changeState(String newState) {
		context = newState;
		// speech.speak(getPhrase(context));
		return newState;
	}

	// -------------- CALLBACKS END -------------------------

	public String getPhrase(String input) {
		if (phrases.containsKey(input)) {
			Object[] keys = phrases.get(input).keySet().toArray();
			if (keys.length == 0) {
				return "i can only find a single key context, which is, " + input;
			}
			String randomValue = (String) keys[generator.nextInt(keys.length)];
			return randomValue;
		} else {
			return "i would like to express what i am doing, but i can't for, " + input;
		}
	}

	// TODO - WebService Call to POST GET and search memory - Jibble it with
	// REST - use new MRL.net utils
	public void saveMemory() // saveMemory
	{
		// save to file system in html format vs database
		Iterator<String> itr = memory.keySet().iterator();

		StringBuffer html = new StringBuffer();
		html.append("<html><head><head><body>");
		html.append("<table class=\"memoryTable\">");
		html.append("<tr><td><b>word</b></td><td><b>image</b></td></tr>\n");

		// move old directory if exists - file indexes will be
		// regenerated
		File old = new File("html");
		old.renameTo(new File("html." + (new Date())));

		while (itr.hasNext()) {
			String n = itr.next();
			NodeDeprecate node = memory.get(n);
			html.append("<tr><td>");
			html.append(node.word);
			html.append("</td><td>");
			for (int i = 0; i < node.imageData.size(); ++i) {
				KinectImageNode kin = node.imageData.get(i);
				// kin.extraDataLabel
				// TODO - write bounding box - mask & crop image - do this at
				// node level?
				// in filter
				String word = node.word;
				new File("html/images/" + word).mkdirs();

				html.append("<img src=\"images/" + word + "/cropped_" + i + ".jpg\" />");
				Util.writeBufferedImage(kin.cameraFrame.getImage(), "html/images/" + word + "/cameraFrame_" + i + ".jpg");
				FileIO.writeBinary("html/images/" + word + "/boundingBox_" + i, kin.boundingBox);
				Util.writeBufferedImage(kin.cropped.getImage(), "html/images/" + word + "/cropped_" + i + ".jpg");
				// TODO - masked/alpha - info.txt file to parse (db at some
				// point) - index values - reference values
				/*
				 * Graphics g = bi.getGraphics(); g.setColor(Color.WHITE);
				 * Rectangle r = kin.boundingBox; g.drawRect(r.x, r.y, r.width,
				 * r.height); g.dispose();
				 */
			}
			html.append("</td></tr>\n");

		}
		html.append("</table>");
		html.append("</body>");
		html.append("</html>");

		Writer out;
		try {
			out = new OutputStreamWriter(new FileOutputStream("html/index.html"), "UTF-8");
			out.write(html.toString());
			out.close();
		} catch (Exception e) {
			Logging.logException(e);
		}

	}

	/*
	 * The conundrum of type conversions. OpenCV functions will want OpenCV but
	 * to serialize and store in the database we'll want SerializableImages,
	 * however the filesystem will want .jpgs or some other common readable
	 * image format
	 */
	public void loadMemory() {
		int imgCount = 0;
		try {
			List<File> files = FindFile.find("html", "cameraFrame_*");
			for (int i = 0; i < files.size(); ++i) {
				File f = files.get(i);
				String path = f.getAbsolutePath().substring(0, f.getAbsolutePath().lastIndexOf(File.separatorChar));
				String index = f.getName().substring(f.getName().lastIndexOf("_") + 1, f.getName().indexOf(".jpg"));
				String word = path.substring(path.lastIndexOf(File.separatorChar) + 1);
				// Integer index = Integer.parseInt(index);
				log.error(f.getAbsolutePath());
				// SerializableImage si =
				// (SerializableImage)FileIO.readBinary(f.getAbsolutePath());
				SerializableImage si = new SerializableImage(Util.readBufferedImage(f.getAbsolutePath()), word);

				if (si != null) {
					Rectangle r = (Rectangle) FileIO.readBinary(path + File.separatorChar + "boundingBox_" + index);

					if (r != null) {
						KinectImageNode kin = new KinectImageNode();
						kin.cameraFrame = si;
						kin.boundingBox = r;
						kin.cvBoundingBox = new CvRect();
						kin.cvBoundingBox.x(kin.boundingBox.x);
						kin.cvBoundingBox.y(kin.boundingBox.y);
						kin.cvBoundingBox.width(kin.boundingBox.width);
						kin.cvBoundingBox.height(kin.boundingBox.height);
						kin.cvCameraFrame = IplImage.createFrom(kin.cameraFrame.getImage());

						// create cropped image
						cvSetImageROI(kin.cvCameraFrame, kin.cvBoundingBox);
						kin.cvCropped = cvCreateImage(cvSize(kin.cvBoundingBox.width(), kin.cvBoundingBox.height()), 8, 3);
						cvCopy(kin.cvCameraFrame, kin.cvCropped);
						cvResetImageROI(kin.cvCameraFrame);
//FIXME						kin.cropped = OpenCV.publishFrame("", kin.cvCropped.getBufferedImage());

						++imgCount;

						if (memory.containsKey(word)) {
							memory.get(word).imageData.add(kin);
						} else {
							ArrayList<KinectImageNode> imgData = new ArrayList<KinectImageNode>();
							NodeDeprecate n = new NodeDeprecate();
							n.word = word;
							n.imageData = imgData;
							n.imageData.add(kin);
							memory.put(word, n);
						}

						// kin.cvBoundingBox = new CvRect();
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logException(e);
		}

		speech.speak("i have " + memory.size() + " things in my visual memory");
		speech.speak("and");
		speech.speak("" + imgCount);
		speech.speak("images");
	}

	public static void main(String[] args) {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.ERROR);

		FSMTest fsm = new FSMTest("fsm");
		fsm.startService();
		fsm.init();
		fsm.loadMemory();
		// template.speechRecognition.stopRecording();
		// template.speechRecognition.startRecording();

	}

	// this is a test
}
