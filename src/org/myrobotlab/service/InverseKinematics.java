package org.myrobotlab.service;

import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceType;
import org.myrobotlab.kinematics.IKEngine;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

/**
 * 
 * InverseKinematics provides basic 2D inverse kinematics features. This class
 * will be replaced with the DH parameter based InverseKinematics3D service.
 *
 */
public class InverseKinematics extends Service {

  protected IKEngine ikEngine;

  private static final long serialVersionUID = 1L;

  public final static Logger log = LoggerFactory.getLogger(InverseKinematics.class.getCanonicalName());

  public static void main(String[] args) {
    LoggingFactory.init(Level.INFO);
    InverseKinematics inversekinematics = new InverseKinematics("inversekinematics");
    inversekinematics.setDOF(3);
    inversekinematics.setStructure(0, 100);
    inversekinematics.setStructure(1, 100);
    inversekinematics.setStructure(2, 100);
    inversekinematics.setPoint(200, 200, 200);
    inversekinematics.compute();

    Runtime.createAndStart("gui", "SwingGui");
    /*
     * SwingGui gui = new SwingGui("gui"); gui.startService();
     */
  }

  public InverseKinematics(String n) {
    super(n);
  }

  public void compute() {
    ikEngine.calculate();
    ikEngine.getBaseAngle();
    ikEngine.getArmAngles();

  }

  public double getArmAngles(int i) {
    double[] b = ikEngine.getArmAngles();
    return b[i];
  }

  public double getBaseAngle() {
    double b = ikEngine.getBaseAngle();
    return b;
  }

  public void setDOF(int dof) {
    ikEngine = new IKEngine(dof);
    ikEngine.setMode(dof);
  }

  public void setPoint(double x, double y, double z) {
    ikEngine.setGoal(x, y, z);
  }

  public void setPoint(float x, float y, float z) {
    ikEngine.setGoal(x, y, z);
  }

  public void setPoint(int x, int y, int z) {
    ikEngine.setGoal(x, y, z);
  }

  public void setStructure(int nlink, double length) {
    ikEngine.setLinkLength(nlink, length);
  }

  /**
   * This static method returns all the details of the class without it having
   * to be constructed. It has description, categories, dependencies, and peer
   * definitions.
   * 
   * @return ServiceType - returns all the data
   * 
   */
  static public ServiceType getMetaData() {

    ServiceType meta = new ServiceType(InverseKinematics.class.getCanonicalName());
    meta.addDescription("Inverse Kinematics");
    meta.addCategory("robot", "control");
    meta.setAvailable(false);
    return meta;
  }

}
