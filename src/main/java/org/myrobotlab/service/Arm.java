/**
 *                    
 * @author grog (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the Apache License 2.0 as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Apache License 2.0 for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.slf4j.Logger;

/**
 * Arm
 * 
 */
public class Arm extends Service {

  public transient final static Logger log = LoggerFactory.getLogger(Arm.class.getCanonicalName());

  private static final long serialVersionUID = 1L;
  public transient final static int IR_PIN = 1;

  Servo shoulder = (Servo)Runtime.start("shoulder", "Servo");
  Servo elbow = (Servo)Runtime.start("elbow", "Servo");
  Servo wrist = (Servo)Runtime.start("wrist", "Servo");
  Servo hand = (Servo)Runtime.start("hand", "Servo");

  int armLength = 0;
  int formArmLength = 0;

  // TODO - do in Service
  public static void main(String[] args) {

    try {
      Arm arm = (Arm)Runtime.start("arm", "Arm"); //new Arm("arm");
      arm.startService();
      arm.startRobot();
    } catch (Exception e) {
      Logging.logError(e);
    }

  }

  public Arm(String n, String id) {
    super(n, id);
  }

  public void startRobot() {
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

    ServiceType meta = new ServiceType(Arm.class);
    meta.addDescription("robot arm service");
    meta.addCategory("robot");
    meta.setLicenseApache();
    meta.addTodo("add IK interfacing points");
    // FIXME - add IK & DH Parameters
    // not ready for primetime - nothing implemented
    meta.setAvailable(false);
    return meta;
  }
}
