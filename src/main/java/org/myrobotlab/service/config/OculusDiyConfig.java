package org.myrobotlab.service.config;

import org.myrobotlab.framework.Peer;
import org.myrobotlab.framework.Plan;

public class OculusDiyConfig extends ServiceConfig {
  
  public Peer arduino = new Peer("arduino", "Arduino");
  public Peer mpu6050 = new Peer("mpu6050", "Mpu6050");

  @Override
  public Plan getDefault(Plan plan, String name) {
    super.getDefault(plan, name);
    
    addDefaultPeerConfig(plan, name, "arduino", "Arduino");
    addDefaultPeerConfig(plan, name, "mpu6050", "Mpu6050");
    
    // could attach them ...
    return plan;
  }

}
