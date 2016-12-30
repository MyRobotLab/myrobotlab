package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.UUID;

import org.python.jline.internal.Log;

public class CollisionItem {
  Point origin = null;
  Point end = null;
  String name;
  double radius=0.0;
  ArrayList<String> ignore = new ArrayList<String>();
  ArrayList<String> done = new ArrayList<String>();
  /**
   * @param origin
   * @param end
   * @param name
   * @param radius
   */
  public CollisionItem(Point origin, Point end, String name, double radius) {
    this.origin = origin;
    this.end = end;
    if (name == null) {
      name = UUID.randomUUID().toString();
    }
    this.name = name;
    this.radius = radius;
  }
  
  public CollisionItem(Point origin, Point end, String name) {
    this.origin = origin;
    this.end = end;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Point getOrigin() {
    return origin;
  }

  public void setOrigin(Point origin) {
    this.origin = origin;
  }

  public Point getEnd() {
    return end;
  }

  public void setEnd(Point end) {
    this.end = end;
    
  }

  /**
   * @return the ignore
   */
  public ArrayList<String> getIgnore() {
    return ignore;
  }

  public void addIgnore(String ignore) {
    this.ignore.add(ignore);
  }

  public double getRadius() {
    return radius;
  }
  
  public boolean isDone(String name) {
    if (done.contains(name)) {
      return true;
    }
    return false;
  }
  
  public void clearDone() {
    done.clear();
  }
  
  void haveDone(String name) {
    done.add(name);
  }
  public double[] getVector(){		
    double[] vect = new double[3]; //[x,y,z]		
    vect[0] = getEnd().getX() - getOrigin().getX();		
    vect[1] = getEnd().getY() - getOrigin().getY();		
    vect[2] = getEnd().getZ() - getOrigin().getZ();		
    return vect;		
  }		
  public double[][] calcPar(double[] vect, int pos) {		
    double[][] par = new double[3][3];//[x,y,z][c,t,k]		
    par[0][0] = getOrigin().getX();		
    par[0][pos] = vect[0];		
    par[1][0] = getOrigin().getY();		
    par[1][pos] = vect[1];		
    par[2][0] = getOrigin().getZ();		
    par[2][pos] = vect[2];		
    return par;		
  }		
  		
  public double[] getVectorT(){		
  	double[] vectorT = new double[]{1,1,0};		
  	double[] vector = getVector();		
  	if (vector[2] != 0) {		
			vectorT[2] = (-vector[0] - vector[1]) / vector[2];		
			double d = Math.sqrt(Math.pow(vectorT[0],2) + Math.pow(vectorT[1], 2) + Math.pow(vectorT[2], 2));		
			double mult = getRadius() / d;		
			vectorT[0] *= mult;		
			vectorT[1] *= mult;		
			vectorT[2] *= mult;		
  	}		
  	else {		
  		Log.info("cannot calculate when item is horizontal");		
  	}		
  	return vectorT;		
  }		
}