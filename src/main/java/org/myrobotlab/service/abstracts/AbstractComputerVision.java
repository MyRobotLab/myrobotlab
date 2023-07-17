package org.myrobotlab.service.abstracts;

import org.myrobotlab.cv.ComputerVision;

public abstract class AbstractComputerVision extends AbstractVideoSource implements ComputerVision {

  private static final long serialVersionUID = 1L;

  public AbstractComputerVision(String n, String id) {
    super(n, id);
  }

}
