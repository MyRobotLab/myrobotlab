package org.myrobotlab.framework.interfaces;

import org.myrobotlab.service.config.ServiceConfig;

public interface ConfigurableService<T extends ServiceConfig> {
  T getConfig();
  T apply(T c);
}
