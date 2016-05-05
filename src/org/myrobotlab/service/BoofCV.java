package org.myrobotlab.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

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
import boofcv.alg.tracker.klt.PkltConfig;
import boofcv.factory.feature.tracker.FactoryPointTracker;
import boofcv.gui.feature.VisualizeFeatures;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.image.GrayF32;

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

			// tune the tracker for the image size and visual appearance
			ConfigGeneralDetector configDetector = new ConfigGeneralDetector(-1,8,1);
			PkltConfig configKlt = new PkltConfig(3,new int[]{1,2,4,8});
	 
			PointTracker<GrayF32> tracker = FactoryPointTracker.klt(configKlt,configDetector,GrayF32.class,null);
	 
			// Open a webcam at a resolution close to 640x480
			com.github.sarxos.webcam.Webcam webcam = UtilWebcamCapture.openDefault(640,480);
	 
			// Create the panel used to display the image and feature tracks
			ImagePanel gui = new ImagePanel();
			gui.setPreferredSize(webcam.getViewSize());
	 
			ShowImages.showWindow(gui,"KLT Tracker",true);
	 
			int minimumTracks = 100;
			while( true ) {
				BufferedImage image = webcam.getImage();
				GrayF32 gray = ConvertBufferedImage.convertFrom(image,(GrayF32)null);
	 
				tracker.process(gray);
	 
				List<PointTrack> tracks = tracker.getActiveTracks(null);
	 
				// Spawn tracks if there are too few
				if( tracks.size() < minimumTracks ) {
					tracker.spawnTracks();
					tracks = tracker.getActiveTracks(null);
					minimumTracks = tracks.size()/2;
				}
	 
				// Draw the tracks
				Graphics2D g2 = image.createGraphics();
	 
				for( PointTrack t : tracks ) {
					VisualizeFeatures.drawPoint(g2,(int)t.x,(int)t.y,Color.RED);
				}
	 
				gui.setBufferedImageSafe(image);
			}
			
			// BoofCV template = (BoofCV) Runtime.start("template", "BoofCV");
			// Runtime.start("gui", "GUIService");

		} catch (Exception e) {
			Logging.logError(e);
		}
	}

}
