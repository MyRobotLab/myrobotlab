package org.myrobotlab.service;

//import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.myrobotlab.framework.Service;
import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.memory.Memory;
import org.myrobotlab.memory.MemoryChangeListener;
import org.myrobotlab.memory.Node;
import org.myrobotlab.opencv.OpenCVData;
import org.myrobotlab.opencv.OpenCVFilterFaceDetect;
import org.myrobotlab.service.data.Rectangle;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;

/**
 * @author GroG TODO - move Tracking in Cortex objects to step through
 *         algorithms in call-backs All peer services are accessable directly
 *         revert Tracking
 */
public class Cortex extends Service implements MemoryChangeListener {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(Cortex.class.getCanonicalName());


	// ------- begin names --------------
	// FIXME - get composite names of sub peers - FIXME NEED INTERFACE
	public String trackingName = "tracking";
	public String faceDetectorName = "faceDetector";
	// ------- end names --------------

	// peer services
	transient Tracking tracking;
	transient OpenCV faceDetector;
	
	public OpenCVFilterFaceDetect faceFilter = new OpenCVFilterFaceDetect();

	// TODO - store all config in memory too?
	private Memory memory = new Memory();

	public Cortex(String n) {
		super(n);
	}

	@Override
	public String getDescription() {
		return "used as a general template";
	}

		
	public void startService()
	{
		super.startService();
		memory.addMemoryChangeListener(this);

		memory.put("/", new Node("past"));
		memory.put("/", new Node("present"));
		memory.put("/", new Node("future")); // <- predictive
		memory.put("/", new Node("locations"));

		memory.put("/present", new Node("background"));
		memory.put("/present", new Node("foreground"));
		memory.put("/present", new Node("faces"));
		memory.put("/present/faces", new Node("unknown"));
		memory.put("/present/faces", new Node("known"));
		memory.put("/present", new Node("objects"));

		memory.put("/past", new Node("background"));
		memory.put("/past", new Node("foreground"));		
		
		// FIXME - check if exists ! - IF EXISTS THEN COMES THE RESPONSIBLITY OF BEING TOTALLY CONFIGURED 
		// EXTERNALLY
		tracking = (Tracking) Runtime.createAndStart(trackingName, "Tracking"); // FIXME - needs to pass in reference? dunno
//		tracking.opencvName = "cameraTracking";
		tracking.connect("COM12");
		tracking.startService();
		tracking.trackPoint(); 
		
		faceDetector = (OpenCV) Runtime.create(faceDetectorName, "OpenCV");
		faceDetector.setPipeline(String.format("%s.PyramidDown", tracking.opencv.getName()));// set key
		faceDetector.addFilter(faceFilter);
		faceDetector.setDisplayFilter(faceFilter.name);
		faceDetector.startService();
		faceDetector.capture();
		subscribe("publishOpenCVData", faceDetector.getName(), "foundFace", OpenCVData.class);
		faceDetector.broadcastState();

		subscribe("toProcess", tracking.getName(), "process", OpenCVData.class);	
		
		
		// FIXME - cascading broadcast !! in composites especially !!
		tracking.broadcastState();
		
	}
	
	Node currentFace;
	
	// FIXME - only publish when faces are actually found
	public void foundFace(OpenCVData faces)
	{
		
		ArrayList<Rectangle> bb = faces.getBoundingBoxArray();
		if (bb != null)
		{
			currentFace = memory.getNode("/present/faces/unknown/face1");
			if (currentFace == null)
			{
				currentFace = new Node("face1");
				memory.put("/present/faces/unknown", currentFace);
			}
			
			ArrayList<SerializableImage> templates = (ArrayList<SerializableImage>)currentFace.get("templates");
			if (templates == null)
			{
				templates = new ArrayList<SerializableImage>();
				currentFace.put("templates", templates);
			}
			
			// non machine build of template stack
			if (templates.size() < 30){
				//templates.addAll(faces.cropBoundingBoxArray());
			} else {
				templates.remove(0);
				//templates.add(faces.cropBoundingBoxArray().get(0));
			}
			log.error("{}",templates.size());
			int width = faces.getWidth();
			int height = faces.getHeight();
			
			if (bb.size()>0){
				Rectangle r = bb.get(0);
				float foreheadX = (float)(r.x + r.width/2)/width;
				float foreheadY = (float)(r.y + r.height/2)/height;
				tracking.trackPoint(foreheadX, foreheadY);
				
				// must determine if this is the "same" face by location !
				//memory.put("/present/faces/unknown", new Node("face1", (Object)faces));
				
			}
		}
	}

	public void process(String src, String dst) {
		// for node blah blah
		Node node = memory.getNode(src);

		if (node == null) {
			log.error("could not process {} not valid node", src);
			return;
		}

		for (Map.Entry<String, ?> nodeData : node.getNodes().entrySet()) {
			String key = nodeData.getKey();
			Object object = nodeData.getValue();
			log.info("{}{}", key, object);

			// display based on type for all non-recursive memory
			Class<?> clazz = object.getClass();
			if (clazz != Node.class) {
				if (clazz == OpenCVData.class) {
					OpenCVData data = (OpenCVData) object;
					// single output - assume filter is set to last
					OpenCVData cv = faceDetector.add(data.getInputImage());
					Node pnode = new Node(node.getName());
					//pnode.put(MEMORY_OPENCV_DATA, cv);
					if (cv.getBoundingBoxArray() != null)
					{
						log.info("found faces");
						memory.put(dst, pnode);
					}
					
				}
			}
		}
	}

	public void saveMemory() {
		saveMemory(null);
	}

	public void saveMemory(String infilename) {
		String filename;

		if (infilename == null) {
			filename = String.format("memory.%d.xml", System.currentTimeMillis());
		} else {
			filename = infilename;
		}

		try {
			Serializer serializer = new Persister();

			// SerializableImage img = new SerializableImage(ImageIO.read(new
			// File("opencv.4084.jpg")), "myImage");
			File xml = new File(filename);
			serializer.write(memory, xml);

		} catch (Exception e) {
			Logging.logException(e);
		}
	}

	// ---------publish begin ----------
	// publish means update if it already exists
	public void publish(String path, Node node) {
		invoke("publishNode", new Node.NodeContext(path, node));
	}

	public Node.NodeContext publishNode(Node.NodeContext nodeContext) {
		return nodeContext;
	}

	// callback from memory tree - becomes a broadcast
	public void onPut(String parentPath, Node node) {
		invoke("putNode", parentPath, node);
	}

	// TODO - broadcast onAdd event - this will sync gui
	public Node.NodeContext putNode(String parentPath, Node node) {
		return new Node.NodeContext(parentPath, node);
	}

	// ---------publish end ----------

	/*
	public void videoOff() {
		tracking.opencv.publishOpenCVData(false);
	}
	*/
	
	public void crawlAndPublish() {
		memory.crawlAndPublish();
	}


	public OpenCV getProcessor() {
		return faceDetector;
	}

	public Memory getMemory() {
		return memory;
	}

	public Tracking getTracking() {
		return tracking;
	}
	
	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		Cortex cortex = (Cortex) Runtime.createAndStart("cortex", "Cortex");
		
		cortex.tracking.trackPoint();
		
		//Runtime.createAndStart("python", "Python");
		// cortex.videoOff();

		GUIService gui = new GUIService("gui");
		gui.startService();
		

		// cortex.add("root", new Node("background"));
		// cortex.add("root", new Node("foreground"));

		log.info("here");

	}


}
