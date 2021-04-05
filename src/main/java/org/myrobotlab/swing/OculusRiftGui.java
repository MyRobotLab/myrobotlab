package org.myrobotlab.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.SwingGui;
import org.myrobotlab.service.interfaces.VideoGUISource;
import org.slf4j.Logger;

public class OculusRiftGui extends ServiceGui implements VideoGUISource, ActionListener {

  static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(OculusRiftGui.class.toString());

  // Left and right eye video widgets
  VideoWidget leftEye = null;
  VideoWidget rightEye = null;

  public OculusRiftGui(String boundServiceName, SwingGui myService) {
    super(boundServiceName, myService);

    // use the monitor window

    // Create the 2 video widgets
    leftEye = new VideoWidget("left", myService);

    rightEye = new VideoWidget("right", myService);

    JPanel leftVideoPanel = new JPanel();
    leftVideoPanel.add(leftEye.display);

    JPanel rightVideoPanel = new JPanel();
    rightVideoPanel.add(rightEye.display);

    // the two video widgets add to display.
    display.add(leftVideoPanel);
    display.add(rightVideoPanel);

  }

  @Override
  public void actionPerformed(ActionEvent e) {
    // TODO Auto-generated method stub
  }

  @Override
  public VideoWidget getLocalDisplay() {
    // TODO : Who calls this ever?
    return null;
  }

  @Override
  public void subscribeGui() {
    subscribe("publishRiftFrame");
  }

  @Override
  public void unsubscribeGui() {
    unsubscribe("publishRiftFrame");
  }

}
