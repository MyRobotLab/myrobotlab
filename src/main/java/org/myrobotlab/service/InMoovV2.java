package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggingFactory;

/**
 * InMoov V2 White page - The InMoov Service ( refactor WIP ).
 * 
 * The InMoov service allows control of the InMoov robot. This robot was created
 * by Gael Langevin. It's an open source 3D printable robot. All of the parts
 * and instructions to build are on http://www.inmoov.fr/). InMoov is a composite of servos, Arduinos,
 * microphone, camera, kinect and computer. The InMoov service is composed of
 * many other services, and allows easy initialization and control of these
 * sub systems.
 *
 */
public class InMoovV2 extends Service {

  public InMoovV2(String n) {
    super(n);
  }

  private static final long serialVersionUID = 1L;
  
  public static void main(String[] args) {
    try {
      LoggingFactory.init(Level.INFO);

      String leftPort = "COM3";
      String rightPort = "COM4";

      VirtualArduino vleft = (VirtualArduino) Runtime.start("vleft", "VirtualArduino");
      VirtualArduino vright = (VirtualArduino) Runtime.start("vright", "VirtualArduino");
      vleft.connect("COM3");
      vright.connect("COM4");
      Runtime.start("gui", "SwingGui");

      InMoovV2 i02 = (InMoovV2) Runtime.start("i02", "InMoovV2");
      
    } catch (Exception e) {
      log.error("main threw", e);
    }
  }

  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(_TemplateService.class);
    meta.setAvailable(false);
    return meta;
  }

  

}
