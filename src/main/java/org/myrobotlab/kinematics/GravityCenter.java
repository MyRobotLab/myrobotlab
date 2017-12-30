package org.myrobotlab.kinematics;

import java.util.HashMap;

import org.myrobotlab.service.IntegratedMovement;

/**
 * This class will compute the center of gravity of the links composing a robot
 * 
 * @author chris
 *
 */
public class GravityCenter extends Thread {

  private HashMap<String, Double> masses = new HashMap<String, Double>();
  private HashMap<String, Double> cogs = new HashMap<String, Double>();
  private transient IntegratedMovement service;
  private Point cog;
  private Point cogTarget = new Point(0,0,0,0,0,0);
  private int maxDistanceToCog = 25;
  
  public GravityCenter(IntegratedMovement im) {
    super("GravityCenter");
    service = im;
  }
  
  /**
   * Set the mass and center of mass of a link
   * @param name name
   * @param mass mass
   * @param centerOfMass  (0.0 - 1.0) representing where the center of mass is located, from the origin point. If you don't know, it's safe to put 0.5
   */
  public void setLinkMass(String name, double mass, double centerOfMass) {
    masses.put(name, mass);
    cogs.put(name, centerOfMass);
  }
  
  public synchronized Point computeCoG(CollisionDectection cd) {
    if (cd == null) {
      cd = service.collisionItems;
    }
    double totalMass = 0;
    for (double mass : masses.values()) {
      totalMass += mass;
    }
    cog = new Point(0,0,0,0,0,0);
    for (CollisionItem ci : cd.getItems().values()) {
      if (cogs.containsKey(ci.getName())) {
        Point icog = ci.getEnd().subtract(ci.getOrigin()).unitVector(1).multiplyXYZ(cogs.get(ci.getName())).multiplyXYZ(ci.getLength()).add(ci.getOrigin());
        double m = masses.get(ci.getName())/totalMass;
        Point ic = icog.multiplyXYZ(m);
        Point c = cog.add(ic);
        cog = c;
      }
    }
    //Log.info(cog.toString()+"gc");
    if (cog.getZ() <= 0.1){
      int x = 0;
      
    }
    return cog;
    
  }
  
  public Point getCoG() {
    return cog;
  }

  public Point getCoGTarget() {
    return cogTarget;
  }

  public double getMaxDistanceToCog() {
    return maxDistanceToCog;
  }
  
  public void setCoGTarget(double x, double y, double z) {
    cogTarget = new Point(x, y, z, 0, 0, 0);
  }
}
