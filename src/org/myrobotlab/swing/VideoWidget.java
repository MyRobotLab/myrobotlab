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

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.border.TitledBorder;

import org.myrobotlab.image.SerializableImage;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.swing.widget.VideoDisplayPanel;

public class VideoWidget extends ServiceGui {

  HashMap<String, VideoDisplayPanel> displays = new HashMap<String, VideoDisplayPanel>();
  boolean allowFork = false;
  public Dimension normalizedSize = null;
  int videoDisplayXPos = 0;
  int videoDisplayYPos = 0;

  public VideoWidget(final String boundServiceName, final SwingGui myService) {
    super(boundServiceName, myService);
    // set initial default output
    addVideoDisplayPanel("output");
  }
  
  public void setTitle(String t) {
    TitledBorder title;
    title = BorderFactory.createTitledBorder(t);
    display.setBorder(title);
  }
  

  public VideoDisplayPanel addVideoDisplayPanel(String source) {
    return addVideoDisplayPanel(source, null);
  }

  public VideoDisplayPanel addVideoDisplayPanel(String source, ImageIcon icon) {
    // FIXME FIXME FIXME - should be FlowLayout No?

    if (videoDisplayXPos % 2 == 0) {
      videoDisplayXPos = 0;
      ++videoDisplayYPos;
    }

    // gc.gridx = videoDisplayXPos;
    // gc.gridy = videoDisplayYPos;

    VideoDisplayPanel vp = new VideoDisplayPanel(source, this, myService, boundServiceName);

    // add it to the map of displays
    displays.put(source, vp);

    // add it to the display
    display.add(vp.myDisplay);

    ++videoDisplayXPos;
    display.invalidate();
    myService.pack();

    return vp;
  }

  @Override
  public void subscribeGui() {
	// FIXME - should be to spec .. onDisplay no displayFrame
    subscribe("publishDisplay", "displayFrame");
  }

  public void attachGui(String srcMethod, String dstMethod) {
    subscribe(srcMethod, dstMethod);
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishDisplay", "displayFrame");
  }

  // multiplex images if desired
  public void displayFrame(SerializableImage img) {
    String source = img.getSource();
    if (displays.containsKey(source)) {
      displays.get(source).displayFrame(img);
    } else if (allowFork) {
      VideoDisplayPanel vdp = addVideoDisplayPanel(img.getSource());
      vdp.displayFrame(img);
    } else {
      displays.get("output").displayFrame(img); // FIXME - kludgy !!!
    }
  }

  public void removeAllVideoDisplayPanels() {
    Iterator<String> itr = displays.keySet().iterator();
    while (itr.hasNext()) {
      String n = itr.next();
      log.error("removing " + n);
      // removeVideoDisplayPanel(n);
      VideoDisplayPanel vdp = displays.get(n);
      display.remove(vdp.myDisplay);
    }
    displays.clear();
    videoDisplayXPos = 0;
    videoDisplayYPos = 0;
  }

  public void removeVideoDisplayPanel(String source) {
    if (!displays.containsKey(source)) {
      log.error("cannot remove VideoDisplayPanel " + source);
      return;
    }

    VideoDisplayPanel vdp = displays.remove(source);
    display.remove(vdp.myDisplay);
    display.invalidate();
    myService.pack();
  }

  public void allowFork(boolean b) {
    this.allowFork = b;
  }

}
