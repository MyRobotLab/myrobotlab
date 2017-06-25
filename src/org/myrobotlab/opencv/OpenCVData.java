package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_imgcodecs.cvEncodeImage;
import static org.myrobotlab.opencv.VideoProcessor.INPUT_KEY;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.CvMat;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.service.OpenCV;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.data.Rectangle;
import org.slf4j.Logger;

/**
 * This is the data returned from a single pass of an OpenCV pipeline of
 * filters. The amount of data can be changed depending on individual
 * configuration of the filters. The filters had some limited ability to add a
 * copy of the image and add other data structures such as arrays of point,
 * bounding boxes, masks and other information.
 * 
 * The default behavior is to return the data from the LAST FILTER ON THE
 * PIPELINE
 * 
 * Some optimizations are done by saving the results of type conversions. For
 * example if a JPG is asked for it is saved back into the data map, so that if
 * its asked again, the cached copy will be returned
 * 
 * All data is put in with keys with the following format
 * [ServiceName].[FilterName].[Format].[Data Type] - e.g.
 * opencv.PyramidDown.jpg.Bytes -- lame re-work ByteArray
 * 
 * choices of images are "by filter name", the "input", the display, and the
 * "last filter" == "output" choices of return types are IplImage, CVMat,
 * BufferedImage, ByteBuffer, ByteArrayOutputStream, byte[]
 * 
 * method naming conventions (get|set) (display | input | filtername) (format -
 * IplImage=image CVMat | BufferedImage | ByteBuffer | Bytes
 * 
 * internal keys - there are several expected keys - they are
 * 
 * input IplImage image = [[servicename].input] input IplImage image =
 * [[servicename].input].display
 * 
 * filter IplImage image = [[servicename].[filtername]] display IplImage image =
 * [[servicename].[filtername]].display
 *
 * optional type keys filter IplImage image =
 * [[servicename].[filtername]].bufferedImage display IplImage image =
 * [[servicename].[filtername]].display.bufferedImage filter IplImage image =
 * [[servicename].[filtername]].jpg.bytes display IplImage image =
 * [[servicename].[filtername]].display.jpg.bytes
 * 
 * @author GroG
 * 
 */
public class OpenCVData implements Serializable {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVData.class);

	/**
	 * serializable objects - these can be transported
	 */
	HashMap<String, Object> serializable = new HashMap<String, Object>();
	
	/**
	 * all non-serializable data including frames an IplImages
	 * It will also contain a global source set of keys
	 */
	transient static final HashMap<String, Object> sources = new HashMap<String, Object>();

	// TODO add KEY_INPUT .. take away from OpenCV
	public static final String KEY_DEPTH = "depth";
	public static final String KEY_JPG = "jpg";
	public static final String KEY_BYTES = "bytes";
	public static final String KEY_WIDTH = "width";
	public static final String KEY_HEIGHT = "height";
	public static final String KEY_BUFFERED_IMAGE = "bufferedImage";

	/**
	 * return type - an ArrayList&lt;Rectangles&gt;
	 */
	public static final String KEY_BOUNDING_BOXES = "boundingBoxes";

	/**
	 * return type - IplImage - either references original filtername IplImage
	 * or a filter.display() processes IplImage
	 */
	public static final String KEY_DISPLAY = "display";
	// public final static String DEPTH_KEY = "depth";
	// Bytes

	private String name;

	/**
	 * the filter's name - used as a key to get or put data associated with a
	 * specific filter
	 */
	private String inputFilterName = INPUT_KEY;
	private String selectedFilter = INPUT_KEY;
	private String displayFilterName = INPUT_KEY;
	private long timestamp;
	private int frameIndex;
	private int eyesDifference;

	static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

	/**
	 * constructed by the 'name'd service
	 */
	public OpenCVData() {
		this(null, 0);
	}

	public OpenCVData(String name, int frameIndex) {
		this.name = name;
		this.timestamp = System.currentTimeMillis();
		this.frameIndex = frameIndex;
	}

	public boolean containsAttribute(String name) {
		return sources.containsKey(String.format("%s.attribute.%s", selectedFilter, name));
	}

	public boolean containsKey(String key) {
		return sources.containsKey(key);
	}

	public Object getAttribute(String name) {
		String key = makeKey(name);
		if (sources.containsKey(key)) {
			return sources.get(key);
		}
		return null;
	}

	public ArrayList<Rectangle> getBoundingBoxArray() {
		String key = String.format("%s.%s.boundingboxes", name, selectedFilter);
		if (sources.containsKey(key)) {
			return (ArrayList<Rectangle>) sources.get(key);
		} else {
			return null;
		}
	}

	// -------- IplImage begin ----------------

	public BufferedImage getBufferedImage() {
		return getBufferedImage(selectedFilter);
	}

	/*
	 * FIXME (FIX OTHERS) NEEDS TO BE ONE AND ONLY ONE TYPE PROCESSOR LIKE THIS
	 * ONE !!!! WITH SAME SUBKEY SIGNATURE lowest level - full key path always
	 * required
	 * 
	 * @return the image stored in the cv data
	 */
	public BufferedImage getBufferedImage(String filterName) {
		String bufferedImageKey;

		bufferedImageKey = String.format("%s.%s.%s", name, filterName, KEY_BUFFERED_IMAGE);

		if (serializable.containsKey(bufferedImageKey)) {
			return (BufferedImage) serializable.get(bufferedImageKey);
		} else {

			String imgKey = String.format("%s.%s", name, filterName);

			IplImage img = (IplImage) sources.get(imgKey);

			BufferedImage image = OpenCV.IplImageToBufferedImage(img);

			serializable.put(bufferedImageKey, image);
			return image;
		}
	}

	// -------- ByteBuffer begin ----------------
	public ByteBuffer getByteBufferImage(String filtername) {
		IplImage img = getImage(filtername);
		return img.asByteBuffer();
	}

	public IplImage getDepthImage() {
		return getImage(String.format("%s.%s.%s", name, selectedFilter, KEY_DEPTH));
	}

	public IplImage getDisplay() {
		String key = String.format("%s.%s", name, displayFilterName);
		if (sources.containsKey(key)) {
			return (IplImage) sources.get(key);
		}
		return null;
	}

	// -------- IplImage end ----------------

	// -------- BufferedImage begin ----------------

	// ---------- BufferedImage begin ------------
	public BufferedImage getDisplayBufferedImage() {
		return getBufferedImage(displayFilterName);
	}

	public String getDisplayFilterName() {
		return displayFilterName;
	}

	public CvMat getEncoded(String filterName, String encoding) {

		// should you go to CvMat ?? - or ByteBuffer ???
		String key = String.format("%s.%s.%s", name, filterName, encoding);
		if (sources.containsKey(key)) {
			return (CvMat) sources.get(key);
		} else {
			IplImage img = getImage(filterName);
			if (img == null)
				return null;

			try {
				String e = encoding.toLowerCase();
				CvMat encodedImg = cvEncodeImage(e, img);
				return encodedImg;
				/*
				 * 
				 * ByteBuffer byteBuffer = encodedImg.getByteBuffer(); byte[]
				 * barray = new byte[byteBuffer.remaining()];
				 * byteBuffer.get(barray); log.info(String.format("%d size",
				 * barray.length));
				 * 
				 * FileOutputStream fos = new
				 * FileOutputStream("memoryEncoded.jpg"); fos.write(barray);
				 * fos.close();
				 * 
				 * ByteArrayOutputStream bos = new ByteArrayOutputStream();
				 * bos.write(encodedImg.data_ptr().getStringBytes()); byte[] b =
				 * bos.toByteArray(); log.info("%d size", barray.length);
				 */

			} catch (Exception e) {
				Logging.logError(e);
			}

			/*
			 * cvSaveImage("direct.jpg", img); cvSaveImage("direct.png", img);
			 */

			/*
			 * ByteBuffer bb = encodedImg.asByteBuffer();
			 * 
			 * byte[] b = new byte[bb.remaining()]; bb.get(b);
			 * 
			 * data.put(String.format("%s.JPG", filterName), b);
			 */
			return null;
		}

	}

	/*
	 * FIXME implement
	 * 
	 * @return null
	 */
	public OpenCVFilter getFilter(String name) {
		return null;
	}

	// ---------- BufferedImage end ------------

	public Point2Df getFirstPoint() {
		ArrayList<Point2Df> points = (ArrayList<Point2Df>) sources.get(String.format("%s.points", selectedFilter));
		if (points != null && points.size() > 0)
			return points.get(0);
		return null;
	}

	// -------- ByteBuffer end ----------------

	public int getHeight() {
		return getImage().height();
	}

	/**
	 * parameterless tries to retrieve image based on current filtername
	 * 
	 * @return - the image as represented by the currently selected filter.
	 */
	public IplImage getImage() {
		return getImage(selectedFilter);
	}

	/**
	 * OpenCV VideoProcessor will set this data collection to the last
	 * @param filtername - when asked for an "image" it will give the last filter's
	 * 
	 * @return the filter's IplImage
	 */

	public IplImage getImage(String filtername) {
		String key = String.format("%s.%s", name, filtername);
		return ((IplImage) sources.get(key));
	}

	public BufferedImage getInputBufferedImage() {
		return getBufferedImage(inputFilterName);
	}

	/**
	 * get the original "camera" image - or the image which started the pipeline
	 * 
	 * @return the original image at the beginning of the video pipeline
	 */
	public IplImage getInputImage() {
		return getImage(inputFilterName);
	}

	// WTF ??
	public CvMat getJPG(String filterName) {
		// FIXME FIXME FIXME - before doing ANY CONVERSION EVER - ALWAYS CHECK
		// CACHE !!
		CvMat mat = getEncoded(filterName, ".jpg");
		return mat;
	}

	public ByteBuffer getJPGByteBuffer(String filterName) {
		CvMat mat = getJPG(filterName);
		ByteBuffer byteBuffer = mat.getByteBuffer();
		return byteBuffer;
	}

	// FIXME FIXME FIXME - always push result back into data structure
	public byte[] getJPGBytes(String filterName) {
		String key = String.format("%s.%s.jpg.Bytes", name, filterName);
		if (sources.containsKey(key)) {
			return (byte[]) sources.get(key);
		}

		CvMat mat = getJPG(filterName);

		ByteBuffer byteBuffer = mat.getByteBuffer();
		byte[] barray = new byte[byteBuffer.remaining()];
		byteBuffer.get(barray);
		sources.put(key, barray);
		return barray;
	}

	// -------- JPG to file end ----------------
	// -------- HashMap begin ----------------

	public ArrayList<Point2Df> getPoints() {
		return (ArrayList<Point2Df>) sources.get(String.format("%s.points", selectedFilter));
	}

	public String getSelectedFilterName() {
		return selectedFilter;
	}

	// -------- HashMap end ----------------

	public long getTimestamp() {
		return timestamp;
	}

	public int getEyesDifference() {
		return eyesDifference;
	}

	public int getWidth() {
		return getImage().width();
	}

	public Integer getX() {
		return (Integer) sources.get(String.format("%s.x", selectedFilter));
	}

	public Integer getY() {
		return (Integer) sources.get(String.format("%s.y", selectedFilter));
	}

	public Set<String> keySet() {
		return sources.keySet();
	}

	public void logKeySet() {
		for (Map.Entry<String, Object> o : sources.entrySet()) {
			log.info(o.getKey());
		}
	}

	public String makeKey(String attributeName) {
		return String.format("%s.%s", selectedFilter, attributeName);
	}

	public void put(ArrayList<Rectangle> bb) {
		sources.put(String.format("%s.%s.boundingboxes", name, selectedFilter), bb);
	}

	// // -----------continue------------------
	@SuppressWarnings("unchecked")
	public void put(Rectangle boundingBox) {

		// String key = String.format("%s.%s", filterName,
		// KEY_BOUNDING_BOX_ARRAY);
		String key = String.format("%s.%s.%s", name, selectedFilter, KEY_BOUNDING_BOXES);
		ArrayList<Rectangle> list;
		if (!sources.containsKey(key)) {
			list = new ArrayList<Rectangle>();
			sources.put(key, list);
		} else {
			list = (ArrayList<Rectangle>) sources.get(key);
		}

		list.add(boundingBox);
	}

	/*
	 * the main and typically first image data put into the OpenCVData object
	 * 
	 */
	public void put(String key, IplImage image) {
		sources.put(String.format("%s.%s", name, key), image);
	}
	
	public IplImage get(String key) {
		return (IplImage)sources.get(String.format("%s.%s", name, key));
	}

	public void putAll(HashMap<String, Object> inSources) {
		sources.putAll(inSources);
	}

	public void set(ArrayList<Point2Df> pointsToPublish) {
		sources.put(String.format("%s.points", selectedFilter), pointsToPublish);
	}

	public void setAttribute(String key, Object value) {
		sources.put(makeKey(key), value);
	}

	/*
	 * public ArrayList<SerializableImage> crop() { return
	 * cropBoundingBoxArray(String.format(filtername)); }
	 */

	/*
	 * public ArrayList<SerializableImage> cropBoundingBoxArray() { return
	 * cropBoundingBoxArray(filtername); }
	 */

	/*
	 * public ArrayList<IplImage> cropBoundingBoxArray(String key) { IplImage
	 * img = getImage(key); ArrayList<Rectangle> bbxs = getBoundingBoxArray();
	 * ArrayList<SerializableImage> ret = new ArrayList<SerializableImage>(); if
	 * (bbxs != null) { for (int i = 0; i < bbxs.size(); ++i) { Rectangle r =
	 * bbxs.get(i); //ret.add(new
	 * SerializableImage(img.getImage().getSubimage(r.x, r.y, r.width,
	 * r.height), filtername)); // expand to use pixel values - int width =
	 * img.width(); int height = img.height(); int sx = (int)(r.x * width); int
	 * sy = (int)(r.y * height); int swidth = (int)(r.width * width); int
	 * sheight = (int)(r.height * height); ret.add(new
	 * SerializableImage(deepCopy(img.getImage()).getSubimage(sx, sy, swidth,
	 * sheight), filtername)); } } return ret; }
	 */

	public void setDisplayFilterName(String displayFilterName) {
		this.displayFilterName = displayFilterName;
	}

	/*
	 * sets the selected filter name in the OpenCVData structure provisioned
	 * later to save entire filter? or parts ?
	 * 
	 */
	public void setFilter(OpenCVFilter inFilter) {
		this.selectedFilter = inFilter.name;
	}

	public void setInputFilterName(String inputFilterName) {
		this.inputFilterName = inputFilterName;
	}

	/*
	 * sets the key - used to access the various data of a particular filter -
	 * first set the filter name the access images, points, etc
	 */
	public void setSelectedFilterName(String name) {
		this.selectedFilter = name;
	}

	public void setEyesDifference(int difference) {
		this.eyesDifference = difference;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void setX(int x) {
		sources.put(String.format("%s.x", selectedFilter), x);
	}

	public void setY(int y) {
		sources.put(String.format("%s.y", selectedFilter), y);
	}

	// -------- JPG to file begin ----------------
	public String writeDisplay() {
		return writeImage(selectedFilter, KEY_DISPLAY, null);
	}

	public String writeImage() {
		return writeImage(selectedFilter, null, null);
	}

	public String writeImage(String filter, String subkey, String format) {
		String filename = null;
		if (format == null) {
			format = "jpg";
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			BufferedImage bi = getBufferedImage(filter);
			if (bi == null)
				return null;
			// FIXME OPTIMIZE - USE CONVERT & OPENCV !!!
			ImageIO.write(bi, format, baos);
			filename = String.format("%s.%s.%d.%s", name, filter, frameIndex, format);
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(baos.toByteArray());
			fos.close();

		} catch (IOException e) {
			Logging.logError(e);
		}

		return filename;
	}

	public String writeInput() {
		return writeImage(INPUT_KEY, null, null);
	}

	public void put(String key) {
		sources.put(key, null);
	}
}
