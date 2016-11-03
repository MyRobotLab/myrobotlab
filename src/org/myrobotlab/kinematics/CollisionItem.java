package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.UUID;

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
}
