package org.myrobotlab.kinematics;

import java.io.Serializable;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.math.MathUtils;
import org.myrobotlab.service.Servo;
import org.slf4j.Logger;

//import marytts.util.math.MathUtils;

/**
 * A link class to encapsulate the D-H parameters for a given link in a robotic
 * arm.
 * 
 * d - the "depth" along the previous joint's z axis theta - the rotation about
 * the previous z (the angle between the common normal and the previous x axis)
 * r - the radius of the new origin about the previous z (the length of the
 * common normal) alpha - the rotation about the new x axis (the common normal)
 * to align the old z to the new z.
 * 
 * @author kwatters
 *
 */
public class DHLink implements Serializable {
 
  private static final long serialVersionUID = 1L;
  private double d;
  private double theta;
  // rename this to a ?)
  private double r;
  private double alpha;
  private DHLinkType type;
  // -180 / +180 as min/max i guess?
  private double min = -Math.PI;
  private double max = Math.PI;
  private double initialTheta;

  // TODO: figure this out.
  private String name;

  public transient final static Logger log = LoggerFactory.getLogger(DHLink.class);
  
  private double velocity;
  private int state = Servo.SERVO_EVENT_STOPPED;
  private double targetPos;
  public boolean hasServo = false;
  public double servoMin;
  public double servoMax;
  private double currentPos = 0.0;
  // private Matrix m;
  // TODO: add max/min angle
  public DHLink(String name, double d, double r, double theta, double alpha) {
    super();
    // The name of the servo that we are controlling.
    this.name = name;
    this.d = d;
    this.r = r;
    this.theta = theta;
    initialTheta = theta;
    this.alpha = alpha;
    //
    this.type = DHLinkType.REVOLUTE;
    // m = resolveMatrix();
  }
  
  public DHLink(DHLink copy){
    super();
    this.d = copy.d;
    this.theta = copy.theta;
    this.r = copy.r;
    this.alpha = copy.alpha;
    this.type = copy.type;
    this.min = copy.min;
    this.max = copy.max;
    this.name = copy.name;
    this.initialTheta = copy.initialTheta;
    this.state = copy.state;
    this.targetPos = copy.targetPos;
    this.velocity = copy.velocity;
    this.hasServo = copy.hasServo;
    this.servoMax = copy.servoMax;
    this.servoMin = copy.servoMin;
    this.currentPos = copy.currentPos;
  }
  

  /**
   * @return a 4x4 homogenous transformation matrix for the given D-H parameters
   */
  public Matrix resolveMatrix() {
    Matrix m = new Matrix(4, 4);
    // elements we need
    double cosTheta = Math.cos(theta);
    double sinTheta = Math.sin(theta);
    double cosAlpha = Math.cos(alpha);
    double sinAlpha = Math.sin(alpha);

    // cosTheta = zeroQuantize(cosTheta);
    // sinTheta = zeroQuantize(sinTheta);
    // cosAlpha= zeroQuantize(cosAlpha);
    // sinAlpha = zeroQuantize(sinAlpha);

    // // first row of homogenous xform
    // m.elements[0][0] = cosTheta;
    // m.elements[0][1] = -1 * sinTheta;
    // m.elements[0][2] = 0;
    // m.elements[0][3] = r;
    //
    // // 2nd row of homogenous xform
    // m.elements[1][0] = sinTheta * cosAlpha;
    // m.elements[1][1] = cosTheta * cosAlpha;
    // m.elements[1][2] = -1 * sinAlpha;
    // m.elements[1][3] = -1 * d * sinAlpha;
    //
    // // 3rd row of homogenous xform
    // m.elements[2][0] = sinTheta * sinAlpha;
    // m.elements[2][1] = cosTheta * sinAlpha;
    // m.elements[2][2] = cosAlpha;
    // m.elements[2][3] = d * cosAlpha;
    //
    // // 4th row of homogenous xform
    // m.elements[3][0] = 0;
    // m.elements[3][1] = 0;
    // m.elements[3][2] = 0;
    // m.elements[3][3] = 1;

    // first row of homogenous xform
    m.elements[0][0] = cosTheta;
    m.elements[0][1] = -1 * cosAlpha * sinTheta;
    m.elements[0][2] = sinAlpha * sinTheta;
    m.elements[0][3] = r * cosTheta;

    // 2nd row of homogenous xform
    m.elements[1][0] = sinTheta;
    m.elements[1][1] = cosAlpha * cosTheta;
    m.elements[1][2] = -1 * sinAlpha * cosTheta;
    m.elements[1][3] = r * sinTheta;

    // 3rd row of homogenous xform
    m.elements[2][0] = 0;
    m.elements[2][1] = sinAlpha;
    m.elements[2][2] = cosAlpha;
    m.elements[2][3] = d;

    // 4th row of homogenous xform
    m.elements[3][0] = 0;
    m.elements[3][1] = 0;
    m.elements[3][2] = 0;
    m.elements[3][3] = 1;

    return m;

  }

  public double zeroQuantize(double value) {
    // TODO: move this to a math utils class.
    double resolution = 0.000001;
    if (value < resolution && value > -resolution) {
      value = 0;
    }
    return value;
  }

  // move to an angle
  public void rotate(double angle) {
    // TODO: which parameter?
    if (DHLinkType.REVOLUTE.equals(this.type)) {
      if (angle <= max && angle >= min) {
        this.theta = angle;
      } else {
        // TODO: it's out of range!
        System.out.println("Rotation out of range for link " + angle);
      }
    } 
    if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      if (angle <= max && angle >= min) {
        alpha = angle;
      } else {
        // TODO: it's out of range!
        System.out.println("Rotation out of range for link " + angle);
      }
    }
    else {
      // TODO: You can't rotate a prismatic joint!
      // TODO Throw something?
    }
  }

  public void translate(double d) {
    // TODO: which parameter?
    if (DHLinkType.PRISMATIC.equals(this.type)) {
      this.d = d;
    } else {
      // TODO: You can't translate a revolute joint!
      // TODO Throw something?
    }
  }

  public double getD() {
    return d;
  }

  public void setD(double d) {
    this.d = d;
  }

  public double getA() {
    return r;
  }

  public void setA(double a) {
    this.r = a;
  }

  public double getTheta() {
    return theta;
  }

  public void setTheta(double theta) {
    this.theta = theta;
  }

  public double getAlpha() {
    return alpha;
  }

  public void setAlpha(double alpha) {
    this.alpha = alpha;
  }

  @Override
  public String toString() {
    return "DHLink [d=" + d + ", theta=" + theta + ", r=" + r + ", alpha=" + alpha + " min=" + min + " max=" + max + "]";
  }

  public void incrRotate(double delta) {
    if (DHLinkType.REVOLUTE.equals(type)){
      // we shouldn't go beyond the max
      double destAngle = this.theta + delta;
      // I suppose this means min/max are in radians..
      if (destAngle > max || destAngle < min) {
        // we're out of range
        // log.info("Link {} angle out of range {} ", name, destAngle);
      }
      else {
        this.theta = destAngle;
      }
    }
    else if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      // we shouldn't go beyond the max
      double destAngle = alpha + delta;
      // I suppose this means min/max are in radians..
      if (destAngle > max || destAngle < min) {
        // we're out of range
        // log.info("Link {} angle out of range {} ", name, destAngle);
      }
      else {
        alpha = destAngle;
      }
    }
  }

  public double getThetaDegrees() {
    return this.theta * 180 / Math.PI;
  }

  public double getMin() {
    return min;
  }

  public void setMin(double min) {
    this.min = min;
  }

  public double getMax() {
    return max;
  }

  public void setMax(double max) {
    this.max = max;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void addPositionValue(double positionDeg) {
    if (DHLinkType.REVOLUTE.equals(type)) {
      theta = initialTheta + MathUtils.degToRad(positionDeg);
    }
    else if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      alpha = initialTheta + MathUtils.degToRad(positionDeg);
    }
  }
  
  public double getInitialTheta() {
    return initialTheta;
  }
  
  public Double getPositionValueDeg() {
    if (DHLinkType.REVOLUTE.equals(type)) {
      return (theta * 180/Math.PI) - (initialTheta*180/Math.PI);
    }
    else if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
      return (alpha * 180/Math.PI) - (initialTheta*180/Math.PI);
    }
    return 0.0;
  }

	public double getVelocity() {
		return velocity;
	}

	public void setVelocity(double velocity) {
		this.velocity = velocity;
	}

	public Integer getState() {
		return state;
	}

	public void setState(Integer state) {
		this.state = state;
	}

	/**
	 * @return the targetPos
	 */
	public Double getTargetPos() {
		return targetPos;
	}

	/**
	 * @param targetPos2 the targetPos to set
	 */
	public void setTargetPos(Double targetPos2) {
		this.targetPos = targetPos2;
	}

  public void setCurrentPos(double pos) {
    currentPos = pos;
    
  }
  
  public Double getCurrentPos(){
    return currentPos;
  }

  public DHLinkType getType() {
  	return type;
  }
  
  public void setType(DHLinkType type) {
  	this.type = type;
  	if (DHLinkType.REVOLUTE_ALPHA.equals(type)) {
  	  initialTheta = alpha;
  	}
  }
}

