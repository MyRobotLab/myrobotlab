package org.myrobotlab.vision;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.data.Point2Df;
import org.slf4j.Logger;

public class OpenCVUtils {
	public final static Logger log = LoggerFactory.getLogger(OpenCVUtils.class);

	// directional constants
	transient final static public String DIRECTION_FARTHEST_FROM_CENTER = "DIRECTION_FARTHEST_FROM_CENTER";
	transient final static public String DIRECTION_CLOSEST_TO_CENTER = "DIRECTION_CLOSEST_TO_CENTER";
	transient final static public String DIRECTION_FARTHEST_LEFT = "DIRECTION_FARTHEST_LEFT";
	transient final static public String DIRECTION_FARTHEST_RIGHT = "DIRECTION_FARTHEST_RIGHT";
	transient final static public String DIRECTION_FARTHEST_TOP = "DIRECTION_FARTHEST_TOP";
	transient final static public String DIRECTION_FARTHEST_BOTTOM = "DIRECTION_FARTHEST_BOTTOM";

	public static Point2Df findPoint(ArrayList<Point2Df> data, String direction, Double minValue) {

		double distance = 0;
		int index = 0;
		double targetDistance = 0.0f;

		if (data == null || data.size() == 0) {
			log.error("no data");
			return null;
		}

		if (minValue == null) {
			minValue = 0.0;
		}

		if (DIRECTION_CLOSEST_TO_CENTER.equals(direction)) {
			targetDistance = 1;
		} else {
			targetDistance = 0;
		}

		for (int i = 0; i < data.size(); ++i) {
			Point2Df point = data.get(i);

			if (DIRECTION_FARTHEST_FROM_CENTER.equals(direction)) {
				distance = (float) Math.sqrt(Math.pow((0.5 - point.x), 2) + Math.pow((0.5 - point.y), 2));
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_CLOSEST_TO_CENTER.equals(direction)) {
				distance = (float) Math.sqrt(Math.pow((0.5 - point.x), 2) + Math.pow((0.5 - point.y), 2));
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_LEFT.equals(direction)) {
				distance = point.x;
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_RIGHT.equals(direction)) {
				distance = point.x;
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_TOP.equals(direction)) {
				distance = point.y;
				if (distance < targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			} else if (DIRECTION_FARTHEST_BOTTOM.equals(direction)) {
				distance = point.y;
				if (distance > targetDistance && point.value >= minValue) {
					targetDistance = distance;
					index = i;
				}
			}

		}

		Point2Df p = data.get(index);
		log.info(String.format("findPointFarthestFromCenter %s", p));
		return p;
	}

	/**
	 * new way of converting IplImages to BufferedImages
	 * 
	 * @param src
	 * @return
	 */
	public static BufferedImage IplImageToBufferedImage(IplImage src) {
		OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
		Java2DFrameConverter converter = new Java2DFrameConverter();
		Frame frame = grabberConverter.convert(src);
		return converter.getBufferedImage(frame, 1);
	}

	/**
	 * new way of converting BufferedImages to IplImages
	 * 
	 * @param src
	 * @return
	 */
	public static IplImage BufferedImageToIplImage(BufferedImage src) {
		OpenCVFrameConverter.ToIplImage grabberConverter = new OpenCVFrameConverter.ToIplImage();
		Java2DFrameConverter jconverter = new Java2DFrameConverter();
		return grabberConverter.convert(jconverter.convert(src));
	}

	/**
	 * new way of converting BufferedImages to IplImages
	 * 
	 * @param src
	 * @return
	 */
	public static Frame BufferedImageToFrame(BufferedImage src) {
		Java2DFrameConverter jconverter = new Java2DFrameConverter();
		return jconverter.convert(src);
	}
}
