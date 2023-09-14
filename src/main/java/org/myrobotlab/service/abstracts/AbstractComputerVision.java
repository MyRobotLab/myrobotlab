package org.myrobotlab.service.abstracts;

import org.myrobotlab.cv.ComputerVision;
import org.myrobotlab.service.config.ServiceConfig;

public abstract class AbstractComputerVision<C extends ServiceConfig> extends AbstractVideoSource<C> implements ComputerVision {

  private static final long serialVersionUID = 1L;

  public AbstractComputerVision(String n, String id) {
    super(n, id);
  }

}
