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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.image.Util;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.ServiceGui;

public class PhotoReelWidget extends ServiceGui {

  public class VideoMouseListener implements MouseListener {

    @Override
    public void mouseClicked(MouseEvent e) {
      mouseInfo.setText("clicked " + e.getX() + "," + e.getY());
      // myService.send(boundServiceName, "invokeFilterMethod",
      // "samplePoint", boundFilterName, e);
      Object[] d = new Object[2];
      d[0] = e.getX();
      d[0] = e.getY();
      myService.send(boundServiceName, "invokeFilterMethod", boundFilterName, "samplePoint", d); // TODO
      // -
      // overload
      // and
      // hind
      // boundServiceName
      // in
      // ServiceGUI
      // 2DPoint p = new 2DPoint(e.getX(), e.getY());
    }

    @Override
    public void mouseEntered(MouseEvent e) {
      // mouseInfo.setText("entered");
    }

    @Override
    public void mouseExited(MouseEvent e) {
      // mouseInfo.setText("exit");

    }

    @Override
    public void mousePressed(MouseEvent e) {
      // mouseInfo.setText("pressed");
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      // mouseInfo.setText("release");
    }

  }

  JLabel screen = new JLabel();
  JLabel mouseInfo = new JLabel("mouse x y");
  JLabel resolutionInfo = new JLabel("width x height");

  JLabel deltaTime = new JLabel("0");

  HashMap<String, JLabel> screens = new HashMap<String, JLabel>();
  public SerializableImage lastImage = null;
  public ImageIcon lastIcon = new ImageIcon();
  public ImageIcon myIcon = new ImageIcon();
  public VideoMouseListener vml = new VideoMouseListener();

  public String boundFilterName = "";

  public int lastImageWidth = 0;

  public PhotoReelWidget(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    


    ImageIcon icon = Util.getResourceIcon("photoreel.1.png");
    if (icon != null) {
      screen.setIcon(icon);
    }

    screen.addMouseListener(vml);
    myIcon.setImageObserver(screen); // WWOOAH - THIS MAY BE A BIG
    // OPTIMIZATION !
    
    setTitle(boundServiceName + " " + boundFilterName + " photo reel widget");
    addLine(screen, mouseInfo, resolutionInfo, deltaTime);
  }

  @Override
  public void subscribeGui() {
    subscribe("publishTemplate");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishTemplate");
  }

  public JComboBox<String> getServices(JComboBox<String> cb) {
    if (cb == null) {
      cb = new JComboBox<String>();
    }
    return cb;
  }

  public void onTemplate(BufferedImage img) {
    if (lastImage != null) {
      screen.setIcon(lastIcon);
    }
    myIcon.setImage(img);
    screen.setIcon(myIcon);
    screen.repaint();
    if (lastImage != null) {
      // if timestamp != null)
      // deltaTime.setText(""+ ( img.timestamp.getTime() -
      // lastImage.timestamp.getTime()));
    }

    lastIcon.setImage(img);

    // resize gui if necessary
    if (lastImageWidth != img.getWidth()) {
      screen.invalidate();
      myService.pack();
      lastImageWidth = img.getWidth();
    }

    img = null;

  }

  public void publishTemplate(SerializableImage img) {
    String source = img.getSource();
    if (source != null) {
      publishTemplate(img.getSource(), img);
    } else {
      publishTemplate("unknown", img);
    }
  }

  // TODO - need an explanation of why there are two and why one does
  // not call the other
  public void publishTemplate(String filterName, SerializableImage img) {
    if (!screens.containsKey(filterName)) {
      screens.put(filterName, new JLabel());
    }

    if (lastImage != null) {
      screen.setIcon(lastIcon);
    }
    boundFilterName = img.getSource();
    myIcon.setImage(img.getImage());
    screen.setIcon(myIcon);
    if (lastImage != null) {
      deltaTime.setText("" + (img.getTimestamp() - lastImage.getTimestamp()));
    }
    lastImage = img;
    lastIcon.setImage(img.getImage());

    // resize gui if necessary
    if (lastImageWidth != img.getImage().getWidth()) {
      screen.invalidate();
      myService.pack();
      lastImageWidth = img.getImage().getWidth();
      resolutionInfo.setText(" " + lastImageWidth + " x " + img.getImage().getHeight());
    }

    img = null;

  }

}
