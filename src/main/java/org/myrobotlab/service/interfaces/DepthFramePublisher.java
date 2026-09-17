package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.DepthFrame;

public interface DepthFramePublisher extends NameProvider {

  DepthFrame publishDepthFrame(DepthFrame frame);
}
