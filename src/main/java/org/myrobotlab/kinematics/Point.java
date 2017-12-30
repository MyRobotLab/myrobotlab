package org.myrobotlab.kinematics;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Represents a 3d point in space. TODO: add rotation (roll/pitch/yaw -
 * rz,rx,ry)
 * 
 * @author kwatters
 *
 */
public class Point implements Serializable {
  
  private static final long serialVersionUID = 1L;
  private double x;
  private double y;
  private double z;

  private double roll;
  private double pitch;
  private double yaw;

  /**
   * A 6 dimensional vector representing the 6 degrees of freedom in space.
   * 
   * @param x
   *          - left / right axis
   * @param y
   *          - up / down axis
   * @param z
   *          - forward / backward axis
   * @param roll
   *          - rotation about the z axis
   * @param pitch
   *          - rotation about the x axis
   * @param yaw
   *          - rotation about the y axis
   * 
   */
  public Point(double x, double y, double z, double roll, double pitch, double yaw) {
    super();
    // linear information
    this.x = x;
    this.y = y;
    this.z = z;
    // angular information
    this.roll = roll;
    this.pitch = pitch;
    this.yaw = yaw;
  }
  
  public Point(Point copy) {
    this.x = copy.x;
    this.y = copy.y;
    this.z = copy.z;
    this.roll = copy.roll;
    this.pitch = copy.pitch;
    this.yaw = copy.yaw;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Point other = (Point) obj;
    if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
      return false;
    if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
      return false;
    if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
      return false;
    if (Double.doubleToLongBits(roll) != Double.doubleToLongBits(other.roll))
      return false;
    if (Double.doubleToLongBits(pitch) != Double.doubleToLongBits(other.pitch))
      return false;
    if (Double.doubleToLongBits(yaw) != Double.doubleToLongBits(other.yaw))
      return false;
    return true;
  }

  public double getX() {
    return x;
  }

  public double getY() {
    return y;
  }

  public double getZ() {
    return z;
  }

  public double getRoll() {
    return roll;
  }

  public double getPitch() {
    return pitch;
  }

  public double getYaw() {
    return yaw;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(x);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(y);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(z);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(roll);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(pitch);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(yaw);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  public double magnitude() {
    // TODO Auto-generated method stub
    return Math.sqrt(x * x + y * y + z * z);
  }

  public Point subtract(Point p) {
    // TODO Auto-generated method stub
    Point newPoint = new Point(x - p.getX(), y - p.getY(), z - p.getZ(), roll - p.getRoll(), pitch - p.getPitch(), yaw - p.getYaw());
    return newPoint;
  }

  @Override
  public String toString() {
    // TODO: round this out
    NumberFormat formatter = new DecimalFormat("#0.000");
    return "(x=" + formatter.format(x) + ", y=" + formatter.format(y) + ", z=" + formatter.format(z) + ", roll=" + formatter.format(roll) + ", pitch=" + formatter.format(pitch)
        + ", yaw=" + formatter.format(yaw) + ")";
  }

  public void setX(double x) {
    this.x = x;
  }

  public void setY(double y) {
    this.y = y;
  }

  public void setZ(double z) {
    this.z = z;
  }

  public void setRoll(double roll) {
    this.roll = roll;
  }

  public void setPitch(double pitch) {
    this.pitch = pitch;
  }

  public void setYaw(double yaw) {
    this.yaw = yaw;
  }

  /**
   * add the x,y,z,roll,pitch,yaw of the point passed in, to the current point.
   * return a new point with the individual components summed.
   * @param p the point to be added
   * @return the new point
   */
  public Point add(Point p) {
    // add the linear and angular parts and return the resulting sum.
    // TODO: move this to a utils class and keep this a POJO.
    Point p2 = new Point(p.x + x, p.y + y, p.z + z, p.roll + roll, p.pitch + pitch, p.yaw + yaw);
    return p2;
  }

  /**
   * return a new point with the x,y,z values multipled by the xyzScale
   * @param xyzScale the scaling (maintain aspect ratios)
   * @return  the point as scaled
   */
  public Point multiplyXYZ(double xyzScale) {
    // add the linear and angular parts and return the resulting sum.
    // TODO: move this to a utils class and keep this a POJO.
    Point p2 = new Point(xyzScale * x, xyzScale * y, xyzScale * z, roll, pitch, yaw);
    return p2;
  }

  public Double distanceTo(Point point) {
    Point calcPoint = subtract(point);
    return Math.sqrt(Math.pow(calcPoint.getX(), 2) + Math.pow(calcPoint.getY(), 2) + Math.pow(calcPoint.getZ(), 2));
  }
  
  public Point unitVector(double unitSize){
    if (magnitude() == 0){
     return this;
    }
    Point retval = multiplyXYZ(unitSize/magnitude());
    return retval;
  }
}
