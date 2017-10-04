package org.myrobotlab.service;

import org.myrobotlab.boofcv.ObjectTracker;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.data.Point2Df;
import org.myrobotlab.service.interfaces.Point2DfListener;
import org.myrobotlab.service.interfaces.Point2DfPublisher;
import org.slf4j.Logger;

import boofcv.abst.tracker.TrackerObjectQuad;
import boofcv.factory.tracker.FactoryTrackerObjectQuad;
import boofcv.struct.image.GrayU8;

public class BoofCv extends Service implements Point2DfPublisher, Point2DfListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(BoofCv.class);

  public BoofCv(String n) {
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

    ServiceType meta = new ServiceType(BoofCv.class.getCanonicalName());
    meta.addDescription("a very portable vision library using pure Java");
    meta.setAvailable(true);
    // add dependency if necessary
    meta.addDependency("net.sourceforge.boofcv", "0.23");
    meta.addDependency("pl.sarxos.webcam", "0.3.10");
    meta.addCategory("vision", "video");
    return meta;
  }

  public Point2Df publishPoint2Df(Point2Df point) {
    return point;

  }

  public Point2Df onPoint2Df(Point2Df point) {
    System.out.println("Receinvig");
    return point;

  }
  
  public ObjectTracker<GrayU8> createTracker(){
    TrackerObjectQuad<GrayU8> tracker =
        // FactoryTrackerObjectQuad.circulant(null, GrayU8.class);
        // FactoryTrackerObjectQuad.sparseFlow(null,GrayU8.class,null);
        FactoryTrackerObjectQuad.tld(null, GrayU8.class);
        // FactoryTrackerObjectQuad.meanShiftComaniciu2003(new
        // ConfigComaniciu2003(), colorType);
        // FactoryTrackerObjectQuad.meanShiftComaniciu2003(new
        // ConfigComaniciu2003(true),colorType);
        // FactoryTrackerObjectQuad.meanShiftLikelihood(30,5,255,
        // MeanShiftLikelihoodType.HISTOGRAM,colorType);

        ObjectTracker<GrayU8> app = new ObjectTracker<GrayU8>(tracker, 640, 480);
        return app;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // ImageType<Planar<GrayU8>> colorType = ImageType.pl(3,GrayU8.class);
      BoofCv boofcv = (BoofCv)Runtime.start("boofcv", "BoofCv");
      ObjectTracker<GrayU8> tracker = boofcv.createTracker();
      tracker.start();
      Service.sleep(5000);
      tracker.stop();
      
      // BoofCV template = (BoofCV) Runtime.start("template", "BoofCV");
      // Runtime.start("gui", "SwingGui");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
