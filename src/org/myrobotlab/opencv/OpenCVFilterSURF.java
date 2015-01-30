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

// http://stackoverflow.com/questions/11515072/how-to-identify-optimal-parameters-for-cvcanny-for-polygon-approximation
package org.myrobotlab.opencv;


import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacv.ObjectFinder;
import com.googlecode.javacv.ObjectFinder.Settings;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_core.cvRect;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvPoint;
import static com.googlecode.javacv.cpp.opencv_core.CvScalar;
import static com.googlecode.javacv.cpp.opencv_core.cvLine;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GRAY2BGR;

public class OpenCVFilterSURF extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSURF.class.getCanonicalName());
	public Settings settings = new Settings();
	public ObjectFinder objectFinder = new ObjectFinder(settings);
	private IplImage object = null;
	
	public OpenCVFilterSURF()  {
		super();
		
    }
	
	public OpenCVFilterSURF(String name)  {
		super(name);
	}

	/**
	 * Set the reference object to find with the surf filter.
	 * @param image
	 */
	public void setObjectImage(IplImage image){
		settings.setObjectImage(image);
	}
	
	@Override
	public IplImage process(IplImage image, OpenCVData data) {
		// TODO: Expose configuration of the object image to find.
		// TODO: create proper life cycle for the objectFinder obj.
		if (object == null || image == null) {
            return image;
        }
		
        IplImage objectColor = IplImage.create(object.width(), object.height(), 8, 3);
        cvCvtColor(object, objectColor, CV_GRAY2BGR);

        // a new image to hold the side by side comparison
        IplImage correspond = IplImage.create(image.width(), object.height()+ image.height(), 8, 1);
        cvSetImageROI(correspond, cvRect(0, 0, object.width(), object.height()));
        cvCopy(object, correspond);
        cvSetImageROI(correspond, cvRect(0, object.height(), correspond.width(), correspond.height()));
        cvCopy(image, correspond);
        cvResetImageROI(correspond);

        // set up the object finder
        // TOOD: potentially re-use this finder object?
        ObjectFinder.Settings settings = new ObjectFinder.Settings();
        settings.setObjectImage(object);
        settings.setUseFLANN(true);
        settings.setRansacReprojThreshold(5);
        ObjectFinder finder = new ObjectFinder(settings);

        // grab a timestamp
        long start = System.currentTimeMillis();
        
        // if we find it i guess the bounding box is here!  
        double[] dst_corners = finder.find(image);
        log.info("Finding time = " + (System.currentTimeMillis() - start) + " ms");

        if (dst_corners !=  null) {
        	//  TODO: add a callback
            for (int i = 0; i < 4; i++) {
                int j = (i+1)%4;
                int x1 = (int)Math.round(dst_corners[2*i    ]);
                int y1 = (int)Math.round(dst_corners[2*i + 1]);
                int x2 = (int)Math.round(dst_corners[2*j    ]);
                int y2 = (int)Math.round(dst_corners[2*j + 1]);
                cvLine(correspond, cvPoint(x1, y1 + object.height()),
                        cvPoint(x2, y2 + object.height()),
                        CvScalar.WHITE, 1, 8, 0);
            }
        }
        
        
        // TODO: what additional info do we get from the obj finder?
//        for (int i = 0; i < finder.ptpairs.size(); i += 2) {
//            CvPoint2D32f pt1 = finder.objectKeypoints[finder.ptpairs.get(i)].pt();
//            CvPoint2D32f pt2 = finder.imageKeypoints[finder.ptpairs.get(i+1)].pt();
//            cvLine(correspond, cvPointFrom32f(pt1),
//                    cvPoint(Math.round(pt2.x()), Math.round(pt2.y()+object.height())),
//                    CvScalar.WHITE, 1, 8, 0);
//        }
//        CanvasFrame objectFrame = new CanvasFrame("Object");
//        CanvasFrame correspondFrame = new CanvasFrame("Object Correspond");
//        correspondFrame.showImage(correspond);
//        for (int i = 0; i < finder.objectKeypoints.length; i++ ) {
//            CvSURFPoint r = finder.objectKeypoints[i];
//            CvPoint center = cvPointFrom32f(r.pt());
//            int radius = Math.round(r.size()*1.2f/9*2);
//            cvCircle(objectColor, center, radius, CvScalar.RED, 1, 8, 0);
//        }
//        objectFrame.showImage(objectColor);
//        objectFrame.waitKey();
//        objectFrame.dispose();
//        correspondFrame.dispose();
        
        return correspond;
	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO: impl something here?		
	}

}
