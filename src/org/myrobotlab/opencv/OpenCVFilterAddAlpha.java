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
import static org.bytedeco.javacpp.opencv_core.cvZero;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class OpenCVFilterAddAlpha extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterAddAlpha.class.getCanonicalName());
	public String sourceName;

	transient IplImage dst = null;
	transient IplImage negativeImage = null;

	public OpenCVFilterAddAlpha() {
		super();
	}

	public OpenCVFilterAddAlpha(String name) {
		super(name);
	}

	@Override
	public void imageChanged(IplImage image) {
		dst = null;
	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) throws InterruptedException {

		// cvAnd (src1, src2, dst, mask)
		// f'ing rocks ! -
		// http://www.neuroforge.co.uk/index.php/masking-colour-images
		if (sourceName != null) {
			// INFO - This filter has 2 keys !!!
			// IplImage src = vp.sources.get(String.format("%s.%s",
			// vp.boundServiceName, sourceName));
			IplImage src = data.get(sourceName);
			if (src != null) {
				if (dst == null) {
					dst = src.clone();
				}
				cvZero(dst);
				cvCopy(src, dst, image);
			}
			return dst;
		} else {
			return image;
		}

	}

}
