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

package org.myrobotlab.swing.widget;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Vector;

import javax.swing.JPanel;

public class RadarWidget extends JPanel implements Runnable {

  public class PolarPoint {
    public double theta;
    public double distance;
    public int age;

    public PolarPoint(double theta, double distance) {
      this.theta = theta;
      this.distance = distance;
    }

  }

  private static final long serialVersionUID = 1L;
  Vector<PolarPoint> points;
  double sensorAngle = 0;
  double minAngle = 0;
  double maxAngle = 180;
  double maxRange = 80;

  int state, gotostate;

  public RadarWidget() {
    // setSize(160, 160);
    points = new Vector<PolarPoint>();
    state = 2;
    gotostate = 2;
    points.add(new PolarPoint(39, 7));
    points.add(new PolarPoint(175, 15));
    points.add(new PolarPoint(78, 30));
    points.add(new PolarPoint(95, 60));
    // points.add(new PolarPoint(10,5));
    // points.add(new PolarPoint(70,30));
    // points.add(new PolarPoint(80,17));

  }

  @Override
  public void paint(Graphics g1) {
    PolarPoint curDot;
    double curAngle;
    double curDist;
    double rad = 0;
    int width = 160;
    int height = 160;
    Point sensorCenter = new Point(width / 2, height / 2);

    Graphics2D g = (Graphics2D) g1;

    g.setColor(Color.black);
    // g.fillRect(0, 0, 160, 160);
    // g.fillArc(0, 0, 160, 160, 0, 360);
    g.fillOval(0, 0, 160, 160);
    // Color shade = new Color(0, 255, 0);
    g.setColor(Color.green);
    int zoom = 1;

    rad = sensorAngle * Math.PI / 180;
    g.drawLine(sensorCenter.x, sensorCenter.y, sensorCenter.x - (int) ((maxRange / zoom) * Math.cos(rad)), sensorCenter.y - (int) ((maxRange / zoom) * Math.sin(rad)));

    for (int x = 0; x < points.size(); x++) {
      curDot = points.elementAt(x);
      curAngle = curDot.theta;
      curDist = curDot.distance;

      rad = curAngle * Math.PI / 180;
      // rad = Math.toRadians(curAngle);

      // g.setColor(curDot.getShade());
      // sensor direction line

      g.fillOval(sensorCenter.x - (int) ((curDist / zoom) * Math.cos(rad)), sensorCenter.y - (int) ((curDist / zoom) * Math.sin(rad)), 2, 2);
      // if (curDot.isOld())
      // isDead = 1;
    }
    // if (rad != 0)
    // g.drawLine(400, 410, 398+(int)(570*Math.cos(rad)),
    // 408-(int)(570*Math.sin(rad)));
    // if (isDead == 1)
    // points.removeElementAt(0);
  }

  @Override
  public void run() {
    // doNextLine();
    int x = 0;
    int y = 0;
    int step = 1;
    while (true) {
      // move servo
      // read data
      // repaint

      // ----------------------
      // temporary setData - thread would only move sensor
      x += step;
      if (x > maxAngle) {
        step = -1;
      } else if (x < minAngle) {
        step = 1;
      }
      setData(new PolarPoint(x, y));
      try {
        Thread.sleep(40);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

  }

  public void setData(double theta, double distance) {
    /*
     * if (distance < maxRange) {
     * 
     * }
     */
    sensorAngle = theta;
    repaint();
  }

  public void setData(PolarPoint p) {
    setData(p.theta, p.distance);
  }

}
