package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.DepthFrame;

/**
 * Publishes stereo depth frames on {@code publishDepthFrame}.
 * <p>
 * MyRobotLab sends the returned frame to each attached
 * {@link DepthFrameListener#onDepthFrame(DepthFrame)}.
 * Implementations return the frame listeners should receive.
 * {@link org.myrobotlab.service.OakD} is the usual publisher.
 * The service name comes from {@link NameProvider#getName()}.
 */
public interface DepthFramePublisher extends NameProvider {

  /**
   * Publish one depth frame to attached listeners.
   *
   * @param frame
   *          camera-frame depth image to send
   * @return the frame delivered to listeners, typically {@code frame}
   */
  DepthFrame publishDepthFrame(DepthFrame frame);
}
