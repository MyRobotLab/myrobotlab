package org.myrobotlab.opencv;

import java.util.HashMap;
import java.util.Set;

import org.bytedeco.javacpp.opencv_core.IplImage;

/**
 * @author GroG
 * 
 *         The single source for all OpenCV Data in a key'ed structure OpenCV
 *         Images. The Images should be retrieved through one of the OpenCV
 *         methods, and not directly from the sources. This will allow a copy of
 *         references and the blocking data to effectively copy a set of
 *         references to the desired data.
 * 
 *         TODO create interface offer a switch - straight reference versus
 *         LinkedBlockingQueue
 * 
 * 
 */
public class VideoSources {

	private final static HashMap<String, Object> data = new HashMap<String, Object>();

	public IplImage get(String key) {
		if (data.containsKey(key)) {
			return (IplImage) data.get(key);// .clone();
		}
		return null;
	}

	public IplImage get(String serviceName, String filtername) {
		String key = (String.format("%s.%s", serviceName, filtername));
		return get(key);
	}

	public HashMap<String, Object> getData() {
		return new HashMap<String, Object>(data);
	}

	/*
	 * FIXME - handle OTHER DATA ? public void put(String serviceName, String
	 * filtername, ArrayList<Rectangle> boundingBoxes){
	 * nonImageData.put(String.format("%s.%s.%s", serviceName, filtername,
	 * OpenCVData.KEY_BOUNDING_BOXES), boundingBoxes); }
	 * 
	 * public void get()
	 */

	public Set<String> getKeySet() {
		return data.keySet();
	}

	public void put(String inputKey, IplImage frame) {
		data.put(inputKey, frame);
	}

	public void put(String serviceName, String filtername, IplImage img) {
		String key = (String.format("%s.%s", serviceName, filtername));
		data.put(key, img);
	}

	public void put(String serviceName, String filtername, String subkey, IplImage img) {
		String key = (String.format("%s.%s.%s", serviceName, filtername, subkey));
		data.put(key, img);
	}

}