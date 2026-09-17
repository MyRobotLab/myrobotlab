package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.DepthHud;

public interface DepthHudPublisher extends NameProvider {

  DepthHud publishDepthHud(DepthHud hud);
}
