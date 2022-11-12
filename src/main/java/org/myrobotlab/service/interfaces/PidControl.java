package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.Pid.PidData;
import org.myrobotlab.service.Pid.PidOutput;

public interface PidControl extends NameProvider {

  /**
   * delete and existing pid - if successful the PidData is returned if the pid
   * does not exist null is returned
   * 
   * @param key
   *          - key of pid params
   * @return - the data that was deleted
   */
  PidData deletePid(String key);

  /**
   * Add a new pid controller to the service. The PidData.key will be the way to
   * refer to this controller in subsequent method calls.
   * 
   * @param pid
   *          - all pid parameters for this controller
   * @return the data added
   */
  PidData addPid(PidData pid);

  boolean enable(String key, boolean b);

  /**
   * Publishing the computed pid. If value cannot be caculated - it isn't
   * published
   * 
   * @param data
   *          - contains the input, source, time and computed value of the
   *          controller
   * @return the data pid published
   */
  public default PidOutput publishPid(PidOutput data) {
    return data;
  }

  /**
   * The function that drives the pid. Typically, this would be called by the
   * sensor.
   * 
   * @param key
   *          - key of pid params
   * @param sensorValue
   *          - input data
   * @return the computed value
   */
  public Double compute(String key, double sensorValue);

  /**
   * Setting the set point - in general simple systems, this is usually done
   * once for the desired PV (Plant Value)
   * 
   * @param key
   *          - key of pid params
   * @param setPoint
   *          - set point target
   */
  void setSetpoint(String key, double setPoint);

  /**
   * Resets the controller. <br>
   * This should be used any time the PID is disabled or inactive for extended
   * duration, and the controlled portion of the system may have changed due to
   * external forces.
   * 
   * Reset erases the I term buildup, and removes D gain on the next loop.
   * 
   * @param key
   *          - the pid key
   */
  public void reset(String key);

}
