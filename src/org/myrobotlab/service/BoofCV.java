package org.myrobotlab.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.myrobotlab.boofcv.ObjectTracker;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import boofcv.abst.feature.detect.interest.ConfigGeneralDetector;
import boofcv.abst.feature.tracker.PointTrack;
import boofcv.abst.feature.tracker.PointTracker;
import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

public class BoofCV extends Service {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(BoofCV.class);

	public BoofCV(String n) {
		super(n);
	}

	/**
	 * This static method returns all the details of the class without it having
	 * to be constructed. It has description, categories, dependencies, and peer
	 * definitions.
	 * 
	 * @return ServiceType - returns all the data
	 * 
	 */
	static public ServiceType getMetaData() {

		ServiceType meta = new ServiceType(BoofCV.class.getCanonicalName());
		meta.addDescription("used as a general template");
		meta.setAvailable(false);
		// add dependency if necessary
		// meta.addDependency("org.coolproject", "1.0.0");
		meta.addCategory("general");
		return meta;
	}

	public static void main(String[] args) {
		try {

			LoggingFactory.getInstance().configure();
			LoggingFactory.getInstance().setLevel(Level.INFO);

			ImageType<Planar<GrayU8>> colorType = ImageType.pl(3,GrayU8.class);

			TrackerObjectQuad tracker =
//					FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
//					FactoryTrackerObjectQuad.sparseFlow(null,GrayU8.class,null);
					FactoryTrackerObjectQuad.tld(null,GrayU8.class);
//					FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(), colorType);
//					FactoryTrackerObjectQuad.meanShiftComaniciu2003(new ConfigComaniciu2003(true),colorType);
//					FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,255, MeanShiftLikelihoodType.HISTOGRAM,colorType);


			ObjectTracker app = new ObjectTracker(tracker,640,480);

			app.process();
			
			// BoofCV template = (BoofCV) Runtime.start("template", "BoofCV");
			// Runtime.start("gui", "GUIService");
		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
