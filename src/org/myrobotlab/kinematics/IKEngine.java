package org.myrobotlab.kinematics;

/**
 * class IKEngine
 *
 *
 */
public class IKEngine {

  /**
   * Used to synchronize the methods w/ the animating thread
   */
  boolean stopped, safeToChangeInternalState;
  double[] angles;
  double[] lengths;
  double[] thetas;
  double myx;
  double myy;

  /**
   * The number of links in the arm
   */
  int numLinks;

  /**
   * Column vectors indicating the end affector and the goal point
   */
  Matrix endPoint;
  Matrix goal;

  /**
   * array of column vectors indicating positions of each joint
   */
  Matrix[] jointLocations;

  /**
   * Used in ik iteration: jInverse= jacobian.pseudoinverse dTheta=jInverse*dX
   */
  Matrix jacobian, jInverse;
  Matrix dX;
  Matrix dTheta;

  /**
   * Indicates how small dX is (smaller means more iterations)
   */
  double stepScaleFactor;

  /**
   * init variables
   * @param nLinks number of links
   */
  public IKEngine(int nLinks) {
    stopped = true;
    safeToChangeInternalState = true;

    goal = new Matrix(2, 1);
    endPoint = new Matrix(2, 1);

    dX = new Matrix(2, 1);

    stepScaleFactor = 0.1;

    setMode(nLinks);

  }

  public void calculate() {
    boolean done = false;
    while (done == false) {

      if (Math.abs(endPoint.subtractFrom(goal).elements[0][0]) <= .01 && Math.abs(endPoint.subtractFrom(goal).elements[1][0]) <= .01) {
        done = true;
      }
      moveToGoal(goal);
    }
  }

  public double[] getArmAngles() {
    double x1, z1, x2, z2;
    x1 = z1 = 0;
    for (int i = 0; i < numLinks; i++) {
      x2 = jointLocations[i].elements[0][0];
      z2 = jointLocations[i].elements[1][0];
      // System.out.println("x" + i + " " + x2);
      // System.out.println("y" + i + " " + z2);
      angles[i] = Math.toDegrees(Math.atan((z2 - z1) / (x2 - x1)));
      System.out.println("angle" + i + " " + angles[i]);
      x1 = x2;
      z1 = z2;

    }
    return angles;
  }

  public double getBaseAngle() {
    double bas;
    bas = Math.toDegrees(Math.atan2(myy, myx));
    System.out.println("base angle is: " + bas);
    return bas;

  }

  public void moveToGoal(Matrix goal) {
    safeToChangeInternalState = false;
    moveToGoal_NLink(goal);
    safeToChangeInternalState = true;
  }

  /**
   * calculates jointLocations[] and jacobian, then updates thetas[] Makes one
   * ik iteration.
   *
   * @param gl
   *          the goal point
   */
  void moveToGoal_NLink(Matrix gl) {
    int i;

    double[] ct = new double[numLinks];
    double[] st = new double[numLinks];

    // Pre-calculate sines and cosines so:
    // ct[i]== cos(theta[0]+...+theta[i])
    // st[i]== sin(theta[0]+...+theta[i])
    double sum = 0.0;
    for (i = 0; i < numLinks; i++) {
      sum += thetas[i];

      ct[i] = Math.cos(sum);
      st[i] = Math.sin(sum);
    }

    jointLocations[0].elements[0][0] = lengths[0] * ct[0];
    jointLocations[0].elements[1][0] = lengths[0] * st[0];

    for (i = 1; i < numLinks; i++) {
      jointLocations[i].elements[0][0] = jointLocations[i - 1].elements[0][0] + lengths[i] * ct[i];
      jointLocations[i].elements[1][0] = jointLocations[i - 1].elements[1][0] + lengths[i] * st[i];
    }

    endPoint = jointLocations[numLinks - 1];

    // dX is a vector in the direction of the end-affector to the goal point
    dX = gl.subtractFrom(jointLocations[numLinks - 1]).multiply(stepScaleFactor);

    // set up the jacobian
    for (i = 0; i < numLinks; i++) {
      jacobian.elements[0][i] = jacobian.elements[1][i] = 0.0;
      for (int j = i; j < numLinks; j++) {
        jacobian.elements[0][i] += -lengths[j] * st[j];
        jacobian.elements[1][i] += lengths[j] * ct[j];
      }
    }

    // dTheta= J^-1 * dX
    jInverse = jacobian.pseudoInverse();
    dTheta = jInverse.multiply(dX);

    // increase theta by dTheta
    for (i = 0; i < numLinks; i++) {
      thetas[i] += dTheta.elements[i][0];
    }
  }

  /**
   * @param gx x components of the goal point
   * @param gy y components of the goal point
   * @param gz z components of the goal point
   */
  public void setGoal(double gx, double gy, double gz) {
    goal.elements[0][0] = gx;
    goal.elements[1][0] = gz;
    myx = gx;
    myy = gy;

  }

  /**
   * Changes the length of a link
   * 
   * @param link
   *          the link to change
   * @param length
   *          the new length
   */
  public void setLinkLength(int link, double length) {
    // wait until moveToGoal_NLink() finishes for this critical section...
    while (!safeToChangeInternalState)
      ;

    // **
    // critical section: nothing depends on local vars while this runs
    // **
    if (link < 0 || link >= numLinks)
      return;
    if (length <= 0.0)
      return;

    lengths[link] = length;
  }

  /**
   * Changes the number of links and sets up data structrues. Waits until all ik
   * computation is done and it is safe to change variables.
   * 
   * @param nLinks
   *          the number of links
   */
  public void setMode(int nLinks) {
    // wait until moveToGoal_NLink() finishes for this critical section...
    while (!safeToChangeInternalState)
      ;

    // **
    // critical section: nothing depends on local vars while this runs
    // **
    numLinks = nLinks;

    jointLocations = new Matrix[numLinks];

    angles = new double[numLinks];
    thetas = new double[numLinks];
    lengths = new double[numLinks];

    for (int i = 0; i < numLinks; i++) {
      jointLocations[i] = new Matrix(2, 1);
      jointLocations[i].elements[0][0] = 2.0;
      jointLocations[i].elements[1][0] = 0.0;
      thetas[i] = Math.PI / 50.0;
      lengths[i] = 2.0 / numLinks;
    }

    jacobian = new Matrix(2, numLinks);
    jInverse = new Matrix(numLinks, 2);

    dTheta = new Matrix(numLinks, 1);
  }

}
