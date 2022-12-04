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

package org.myrobotlab.swing;

import java.awt.BorderLayout;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.swing.widget.VideoDisplayPanel2;
import org.slf4j.Logger;

public class VideoWidget2 extends JFrame {

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(VideoWidget2.class);

  transient JPanel display = null;

  HashMap<String, VideoDisplayPanel2> displays = new HashMap<String, VideoDisplayPanel2>();

  boolean allowFork = false;

  int videoDisplayXPos = 0;

  int videoDisplayYPos = 0;

  String boundServiceName;

  public VideoWidget2(String serviceName) {
    super(serviceName);
    boundServiceName = serviceName;
    display = new JPanel(new BorderLayout());
    getContentPane().add(display);
    // set initial default output
    addVideoDisplayPanel("output", null);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
  }

  public VideoDisplayPanel2 addVideoDisplayPanel(String source, ImageIcon icon) {
    // FIXME FIXME FIXME - should be FlowLayout No?

    if (videoDisplayXPos % 2 == 0) {
      videoDisplayXPos = 0;
      ++videoDisplayYPos;
    }

    // source, this, boundServiceName);
    VideoDisplayPanel2 vp = new VideoDisplayPanel2(boundServiceName, this, source, null);

    // add it to the map of displays
    displays.put(source, vp);

    // add it to the display
    display.add(vp.myDisplay);

    ++videoDisplayXPos;
    display.invalidate();
    pack();
    return vp;
  }

  // multiplex images if desired
  public void displayFrame(SerializableImage img) {
    String source = img.getSource();
    if (displays.containsKey(source)) {
      displays.get(source).displayFrame(img);
    } else if (allowFork) {
      VideoDisplayPanel2 vdp = addVideoDisplayPanel(img.getSource(), null);
      vdp.displayFrame(img);
    } else {
      displays.get("output").displayFrame(img); // FIXME - kludgy !!!
    }
  }

  public void removeAllVideoDisplayPanels() {
    displays.clear();
    videoDisplayXPos = 0;
    videoDisplayYPos = 0;
  }

  public void removeVideoDisplayPanel(String source) {
    if (!displays.containsKey(source)) {
      log.error("cannot remove {}", source);
      return;
    }
    VideoDisplayPanel2 vdp = displays.remove(source);
    display.remove(vdp.myDisplay);
    display.invalidate();
    pack();
  }

  public void allowFork(boolean b) {
    allowFork = b;
  }

}
