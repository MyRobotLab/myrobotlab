package org.myrobotlab.swing.opencv;

import java.awt.event.ActionListener;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.myrobotlab.opencv.FilterWrapper;
import org.myrobotlab.service.SwingGui;

// NoImpl.. this is just to avoid the exception in the logs
public class OpenCVFilterMotionDetectGui extends OpenCVFilterGui implements ActionListener, ChangeListener {

  public OpenCVFilterMotionDetectGui(String boundFilterName, String boundServiceName, SwingGui myGui) {
    super(boundFilterName, boundServiceName, myGui);
    // TODO Auto-generated constructor stub
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void getFilterState(FilterWrapper filterWrapper) {
    // TODO Auto-generated method stub
    
  }

}
