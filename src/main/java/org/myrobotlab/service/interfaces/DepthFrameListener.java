package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.DepthFrame;

public interface DepthFrameListener {

  void onDepthFrame(DepthFrame frame);
}
