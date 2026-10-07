package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.DepthHud;

/**
 * Receives a colorized depth overlay from a {@link DepthHudPublisher}.
 * <p>
 * A publisher invokes {@code publishDepthHud}, and MyRobotLab delivers each
 * image here as {@link #onDepthHud(DepthHud)}.
 * {@link org.myrobotlab.service.JMonkeyEngine} draws it as a 2D heads-up
 * display. Pixel layout is described on {@link DepthHud}.
 */
public interface DepthHudListener {

  /**
   * Accept one colorized depth overlay.
   *
   * @param hud
   *          packed RGB image of the depth view. A {@code null} value, or a
   *          hud whose {@code rgb} buffer is missing, means there is nothing
   *          to draw.
   */
  void onDepthHud(DepthHud hud);
}
