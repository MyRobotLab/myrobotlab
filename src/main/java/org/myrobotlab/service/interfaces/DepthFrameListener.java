package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.DepthFrame;

/**
 * Receives stereo depth frames from a {@link DepthFramePublisher}.
 * <p>
 * A publisher invokes {@code publishDepthFrame}, and MyRobotLab delivers each
 * frame here as {@link #onDepthFrame(DepthFrame)}.
 * {@link org.myrobotlab.service.JMonkeyEngine} uses the frame to build an
 * RGB-textured depth mesh. Coordinates follow {@link DepthFrame}: camera
 * frame, X right, Y down, Z forward, depth in millimeters.
 */
public interface DepthFrameListener {

  /**
   * Accept one depth frame.
   *
   * @param frame
   *          depth image, camera intrinsics, and optional aligned color. A
   *          {@code null} frame means the publisher has no sample.
   */
  void onDepthFrame(DepthFrame frame);
}
