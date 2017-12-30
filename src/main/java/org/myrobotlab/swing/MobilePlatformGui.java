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

package org.myrobotlab.swing;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.myrobotlab.service.MobilePlatform;
import org.myrobotlab.service.SwingGui;

public class MobilePlatformGui extends ServiceGui {

  MobilePlatform localReference = null;

  JLabel speed = new JLabel("0.0");

  // bindings begin --------------

  JLabel dimensionX = new JLabel("0.0");;
  JLabel dimensionY = new JLabel("0.0");
  JLabel dimensionZ = new JLabel("0.0");

  JLabel positionX = new JLabel("0.0");
  JLabel positionY = new JLabel("0.0");
  JLabel positionZ = new JLabel("0.0");

  JLabel targetX = new JLabel("0.0");
  JLabel targetY = new JLabel("0.0");
  JLabel targetZ = new JLabel("0.0");

  JLabel dhT = new JLabel("0.0");
  JLabel speedLast = new JLabel("0.0");

  JLabel power = new JLabel("0.0");

  JLabel headingCurrent = new JLabel("0.0");
  JLabel headingTarget = new JLabel("0.0");
  JLabel headingLast = new JLabel("0.0");
  JLabel headingDelta = new JLabel("0.0");
  JLabel headingSpeed = new JLabel("0.0");

  JLabel directionCurrent = new JLabel("0.0");
  JLabel directionTarget = new JLabel("0.0");

  JLabel inMotion = new JLabel("false");

  public MobilePlatformGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    display.setLayout(new FlowLayout());

    // position
    setLeftTitle("position");
    addLeftLine("(x,y) (", positionX, ",", positionY, ")");
    addLeftLine("inMotion ", inMotion);

    setTitle("heading");
    addLine("current ", headingCurrent);
    addLine("last    ", headingLast);
    addLine("dT      ", dhT);
    addLine("speed   ", headingSpeed);

    // target
    setRightTitle("target");
    addRightLine("(x,y)         (", targetX, ",", targetY, ")");
    addRightLine("bearing ", headingTarget);
    addRightLine("delta ", headingDelta);
    addRightLine("direction ", directionTarget);

  }

  @Override
  public void subscribeGui() {
  }

  @Override
  public void unsubscribeGui() {
  }

  /*
   * Service State change - this method will be called when a "broadcastState"
   * method is called which triggers a publishState.  This event handler is typically
   * used when data or state information in the service has changed, and the UI should
   * update to reflect this changed state.
   */
  public void onState(MobilePlatform template) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

      }
    });
  }


}
