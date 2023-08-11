package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.math.geometry.Point2df;
import org.myrobotlab.service.config.ServiceConfig;
import org.myrobotlab.service.interfaces.Point2DfListener;
import org.myrobotlab.service.interfaces.Point2DfPublisher;
import org.slf4j.Logger;

public class BoofCv extends Service<ServiceConfig> implements Point2DfPublisher, Point2DfListener {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(BoofCv.class);

  public BoofCv(String n, String id) {
    super(n, id);
  }

  @Override
  public Point2df publishPoint2Df(Point2df point) {
    return point;
  }

  @Override
  public Point2df onPoint2Df(Point2df point) {
    // System.out.println("Receinvig");
    return point;
  }

  public static void main(String[] args) {
    try {

      LoggingFactory.init(Level.INFO);

      // ImageType<Planar<GrayU8>> colorType = ImageType.pl(3,GrayU8.class);
      BoofCv boofcv = (BoofCv) Runtime.start("boofcv", "BoofCv");

      // BoofCV template = (BoofCV) Runtime.start("template", "BoofCV");
      // Runtime.start("gui", "SwingGui");
    } catch (Exception e) {
      Logging.logError(e);
    }
  }

}
