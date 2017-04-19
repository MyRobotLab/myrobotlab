/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.opencv;

import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvSize;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterMask extends OpenCVFilter {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterMask.class.getCanonicalName());
	transient IplImage dst = null;
	public String maskName = "";

	// TODO - get list of masks for gui
	public OpenCVFilterMask() {
		super();
	}

	public OpenCVFilterMask(String name) {
		super(name);
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub

	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {

		if (maskName != null) {
			// INFO - This filter has 2 keys !!!
			IplImage mask = data.get(maskName);
			if (mask != null) {
				if (dst == null || dst.width() != image.width() || image.nChannels() != image.nChannels()) {
					dst = cvCreateImage(cvSize(image.width(), image.height()), image.depth(), image.nChannels());
				}
				cvCopy(image, dst, mask);
				return dst;
			}
		}
		return image;
	}

}
