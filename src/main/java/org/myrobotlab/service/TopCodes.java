package org.myrobotlab.service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.repo.Repo;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.config.TopCodesConfig;
import org.slf4j.Logger;

import topcodes.Scanner;
import topcodes.TopCode;

/**
 * 
 * TopCodes - This service allows to recognize a special codes. You can print
 * these codes using the attached topcodes.pdf. The service gives back the
 * following information : Number of the code recognized Coordinates of the
 * center of the code (x,y) Diameter of the code (which can be used to find
 * distance) Angular rotation of the code ​THERE ARE 99 DIFFERENT CODES. CAMERA
 * SHOULD BE AS PERPENDICULAR AS POSSIBLE TO THE CODE, IN ORDER TO WORK WELL.
 *
 */
public class TopCodes extends Service<TopCodesConfig> {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(TopCodes.class.getCanonicalName());
  transient Scanner scanner = new Scanner();

  public static void main(String[] args) {
    LoggingFactory.init(Level.DEBUG);

    try {
      Runtime runtime = Runtime.getInstance();
      Repo repo = runtime.getRepo();
      repo.install("TopCodes");

      TopCodes topcodes = (TopCodes) Runtime.start("topcode", "TopCodes");

      topcodes.startService();
      List<TopCode> codes = topcodes.scan("topcodetest.png");

      if (codes.size() == 0) {
        log.info("no codes found");
      }
      for (int i = 0; i < codes.size(); ++i) {
        TopCode code = codes.get(i);
        log.info("number {} code {} x {} y {} diameter {}", i, code.getCode(), code.getCenterX(), code.getCenterY(), code.getDiameter());
      }

    } catch (Exception e) {
      Logging.logError(e);
    }
  }

  /*
   * Static list of third party dependencies for this service. The list will be
   * consumed by Ivy to download and manage the appropriate resources
   * 
   */

  public TopCodes(String n, String id) {
    super(n, id);
  }

  public List<TopCode> scan(BufferedImage img) {
    return scanner.scan(img);
  }

  public List<TopCode> scan(String filename) {
    try {
      BufferedImage img;
      img = ImageIO.read(new File(filename));
      return scanner.scan(img);
    } catch (IOException e) {
      error(e.getMessage());
      Logging.logError(e);
    }

    return null;
  }

}
