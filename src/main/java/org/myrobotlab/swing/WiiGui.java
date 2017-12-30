/**
 * This file is part of WiiuseJ.
 *
 *  WiiuseJ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  WiiuseJ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with WiiuseJ.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.myrobotlab.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.Wii.IRData;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.myrobotlab.swing.widget.Number;
import org.slf4j.Logger;

import wiiusej.values.IRSource;

public class WiiGui extends ServiceGui implements ListSelectionListener, VideoGUISource {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(WiiGui.class.toString());

  VideoWidget video0 = null;
  Graphics cam = null;
  transient BufferedImage camImage = null;

  int width = 1024;
  int height = 768;
  int divisor = 2;

  ArrayList<IRData> irdata = new ArrayList<IRData>();

  final int LEFT = 0;
  final int RIGHT = 1;
  final int UNKNOWN = -1;
  int currentDirection = UNKNOWN;
  int lastDirection = UNKNOWN;

  Number sensitivity = new Number("sensitivity", 5, 1, 5, "ir camera sensitivity");

  public Random rand = new Random();
  public IRData lastIRData = null;

  int x;

  int y;

  int cnt = 0;

  int x0 = 0;

  int y0 = 0;

  int lastMin = width;
  int lastMax = 0;

  long timeStart = 0;

  long timeEnd = 0;
  int sweepTimeDelta = 0;
  int deltaTime = 0;

  public WiiGui(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);

    video0 = new VideoWidget(boundServiceName, myService);

    camImage = new BufferedImage(width / divisor, height / divisor, BufferedImage.TYPE_INT_RGB);

    cam = camImage.getGraphics();

    video0.displayFrame(new SerializableImage(camImage, boundServiceName));

    addLine(video0.display, sensitivity.getDisplay());
    setCurrentFilterMouseListener();

  }

  @Override
  public void subscribeGui() {
    video0.subscribeGui();
    subscribe("publishIR");
    video0.displayFrame(new SerializableImage(camImage, boundServiceName));
  }

  protected ImageIcon createImageIcon(String path, String description) {
    java.net.URL imgURL = getClass().getResource(path);
    if (imgURL != null) {
      return new ImageIcon(imgURL, description);
    } else {
      System.err.println("Couldn't find file: " + path);
      return null;
    }
  }

  @Override
  public void unsubscribeGui() {
    video0.unsubscribeGui();
    unsubscribe("publishIR");
  }

  public void display(IRData ire, Color color) {
    cam.setColor(color);
    for (int i = 0; i < ire.event.getIRPoints().length; ++i) {
      IRSource ir = ire.event.getIRPoints()[i];

      if (ir != null) {
        x = width / divisor - ir.getX() / divisor;
        y = height / divisor - ir.getY() / divisor;
        cam.fillArc(x, y, ir.getSize() * 3, ir.getSize() * 3, 0, 360);
        // cam.drawString(ire.event.getWiimoteId() + " s" + ir.getSize()
        // + " " + ir.getX() + "," + ir.getY(), x + 5, y);
        cam.drawString(ire.event.getWiimoteId() + " " + ir.getX() + "," + ir.getY() + " s" + ir.getSize(), x - 30, y);
      }

    }

  }

  public void displayFrame(SerializableImage camImage) {
    video0.displayFrame(camImage);
  }

  @Override
  public VideoWidget getLocalDisplay() {
    return video0;
  }

  public void onIR(IRData ire) {
    ++cnt;

    if (lastIRData != null) {
      // remove last point
      display(lastIRData, Color.black);
    }

    // display this point
    display(ire, Color.red);

    lastIRData = ire;

    video0.displayFrame(new SerializableImage(camImage, boundServiceName));

  }

  // TODO - encapsulate this
  // MouseListener mouseListener = new MouseAdapter() {
  public void setCurrentFilterMouseListener() {
    MouseListener mouseListener = new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent mouseEvent) {
        JList theList = (JList) mouseEvent.getSource();
        if (mouseEvent.getClickCount() == 2) {
          int index = theList.locationToIndex(mouseEvent.getPoint());
          if (index >= 0) {
            Object o = theList.getModel().getElementAt(index);
            System.out.println("Double-clicked on: " + o.toString());
          }
        }
      }
    };

  }

  @Override
  public void valueChanged(ListSelectionEvent arg0) {
  }

}
