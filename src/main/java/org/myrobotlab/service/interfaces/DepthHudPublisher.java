package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.DepthHud;

/**
 * Publishes a colorized depth overlay on {@code publishDepthHud}.
 * <p>
 * MyRobotLab sends the returned image to each attached
 * {@link DepthHudListener#onDepthHud(DepthHud)}.
 * Implementations return the overlay listeners should receive.
 * The service name comes from {@link NameProvider#getName()}.
 */
public interface DepthHudPublisher extends NameProvider {

  /**
   * Publish one colorized depth overlay.
   *
   * @param hud
   *          packed RGB depth image to send
   * @return the overlay delivered to listeners, typically {@code hud}
   */
  DepthHud publishDepthHud(DepthHud hud);
}
