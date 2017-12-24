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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
//import org.myrobotlab.service.WiiDAR;
import org.myrobotlab.service.Pingdar.Point;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.slf4j.Logger;

public class PingdarGui extends ServiceGui implements ListSelectionListener, VideoGUISource {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(PingdarGui.class.toString());

  VideoWidget screen = null;
  Graphics2D g = null;

  BufferedImage camImage = null;
  BufferedImage graphImage = null;

  int scale = 1;
  int height = 600;
  int width = 800;
  int vheight = height / scale;
  int vwidth = width / scale;

  ArrayList<ArrayList<Point>> left = new ArrayList<ArrayList<Point>>();
  ArrayList<ArrayList<Point>> right = new ArrayList<ArrayList<Point>>();
  ArrayList<ArrayList<Point>> history = new ArrayList<ArrayList<Point>>();

  ArrayList<Point> hist = new ArrayList<Point>();

  DecimalFormat df = new DecimalFormat("#.##");

  int cnt = 0;

  public PingdarGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        screen = new VideoWidget(boundServiceName, myService);

        camImage = new BufferedImage(width / scale, height / scale, BufferedImage.TYPE_INT_RGB);
        graphImage = new BufferedImage(width / scale, height / scale, BufferedImage.TYPE_INT_RGB);

        g = (Graphics2D)graphImage.getGraphics();

        g.setColor(Color.green);

       // DON'T DO THIS - IT WILL BORK THE UI !!!
       // screen.displayFrame(new SerializableImage(graphImage, boundServiceName));

       display.add(screen.display);
      }
    });

  }

  @Override
  public void subscribeGui() {
    subscribe("publishPingdar");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishPingdar");
  }

  public void displayFrame(SerializableImage camImage) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        screen.displayFrame(camImage);
      }
    });
  }

  public void drawStaticInfo() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        g.setColor(Color.gray);

        int inches = 0;

        int r = 160;

        for (r = 160; r < vheight * 2; r += 160) {
          inches += 10;
          g.drawArc(vwidth / 2 - r / 2, vheight - r / 2, r, r, 0, 180);
          g.drawString("" + inches, vwidth / 2 + r / 2, vheight - 10);
        }

      }
    });

  }

  @Override
  public VideoWidget getLocalDisplay() {
    return screen;
  }

  public Point onPingdar(Point p) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {

        int x;
        int y;
        int x0;
        int y0;
        int zScale = 8; // 11
        int xOffset = vwidth / 2; // set origin of polar in middle

        ++cnt;
        if (cnt % 180 == 0) {
          drawStaticInfo();
        }

        if (p.r < 8) {
          // return p;
          return;
        }

        // calculate xy for p
        x = ((int) (p.r * Math.cos(Math.toRadians(p.theta)) * zScale) + xOffset);
        y = vheight - ((int) (p.r * Math.sin(Math.toRadians(p.theta)) * zScale));
        // log.info(String.format(" x y %d %d", x, y));

        // take care of history

        if (hist.size() > 0) {
          // get historical coordinates
          Point h = hist.get(hist.size() - 1);
          x0 = ((int) (h.r * Math.cos(Math.toRadians(h.theta)) * zScale) + xOffset);
          y0 = vheight - ((int) (h.r * Math.sin(Math.toRadians(h.theta)) * zScale));
          double distance = Math.sqrt((x0 - x) * (x0 - x) + (y0 - y) * (y0 - y));

          // black out previous info
          g.setColor(Color.black);
          g.drawString(h.theta + " " + df.format(h.r), x0, y0 - 40);
          g.drawLine(x0, y0, x0, y0 - 40);

          // gray out previous point
          g.setColor(Color.green);
          // draw line if under min distance from previous point
          if (distance < 40) {
            g.drawLine(x, y, x0, y0);
          }

          // black historical lidar vector
          g.setColor(Color.black);
          x0 = ((int) (5 * Math.cos(Math.toRadians(h.theta)) * zScale) + xOffset);
          y0 = vheight - ((int) (5 * Math.sin(Math.toRadians(h.theta)) * zScale));
          g.drawLine(vwidth / 2, vheight, x0, y0);

        }

        int maxHistory = 800;
        if (hist.size() > maxHistory) {
          // aggressive removal - doesnt check on distance
          g.setColor(Color.black);
          Point h = hist.remove(0);
          x0 = ((int) (h.r * Math.cos(Math.toRadians(h.theta)) * zScale) + xOffset);
          y0 = vheight - ((int) (h.r * Math.sin(Math.toRadians(h.theta)) * zScale));

          Point h1 = hist.get(0);
          int x1 = ((int) (h1.r * Math.cos(Math.toRadians(h1.theta)) * zScale) + xOffset);
          int y1 = vheight - ((int) (h1.r * Math.sin(Math.toRadians(h1.theta)) * zScale));
          g.drawLine(x1, y1, x0, y0);

          // remove line segment
        }

        // draw the point & info

        if (p.r > 0) {
          // draw point
          g.setColor(Color.green);
          // x = vwidth - ((int)(p.z * Math.cos(Math.toRadians(p.servoPos)) *
          // zScale) + xOffset);
          // y = vheight - ((int) (p.z * Math.sin(Math.toRadians(p.servoPos))
          // * zScale));

          g.drawLine(x, y, x, y);
          g.drawLine(x, y, x, y - 40);
          g.drawString(p.theta + " " + df.format(p.r), x, y - 40);
        } else {
          // out of range - dump the point
          // return p; - need point to refresh lidar vector
        }

        // draw lidar vector
        x = ((int) (5 * Math.cos(Math.toRadians(p.theta)) * zScale) + xOffset);
        y = vheight - ((int) (5 * Math.sin(Math.toRadians(p.theta)) * zScale));
        g.setColor(Color.gray);
        g.drawLine(vwidth / 2, vheight, x, y);

        hist.add(p);

        // screen image
        screen.displayFrame(new SerializableImage(graphImage, boundServiceName));
      }
    });

    return p;

  }

  @Override
  public void valueChanged(ListSelectionEvent arg0) {
  }

}
