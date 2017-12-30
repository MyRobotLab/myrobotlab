package org.myrobotlab.service.abstracts;

import org.myrobotlab.framework.Service;
import org.myrobotlab.math.Mapper;
import org.myrobotlab.service.interfaces.MotorController;

public abstract class AbstractMotorController extends Service implements MotorController {

  private static final long serialVersionUID = 1L;
  
  protected Mapper powerMapper = null;

  public AbstractMotorController(String reservedKey) {
    super(reservedKey);    
  }

}
