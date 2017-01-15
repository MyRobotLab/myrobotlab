package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.myrobotlab.openni.PVector;
import org.python.jline.internal.Log;

public class CollisionItem {
  Point origin = null;
  Point end = null;
  String name;
  double radius=0.0;
  ArrayList<String> ignore = new ArrayList<String>();
  ArrayList<String> done = new ArrayList<String>();
  boolean fromKinect = false;
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
  
  public CollisionItem(HashMap<Integer[],Map3DPoint> cloudMap) {
  	name = UUID.randomUUID().toString();
  	buildCollisionItem(cloudMap);
  }

  private void buildCollisionItem(HashMap<Integer[], Map3DPoint> cloudMap) {
		//find the average point and the area cover by the cloudMap
  	double[] avg = new double[3];
  	int[] max = new int[]{-9999,-9999,-9999};
  	int[] min = new int[]{9999,9999,9999};
  	for (Map3DPoint m : cloudMap.values()) {
  		avg[0] += m.point.getX();
  		avg[1] += m.point.getY();
  		avg[2] += m.point.getZ();
  		if (m.point.getX() < min[0]) min[0] = (int) m.point.getX();
  		if (m.point.getX() > max[0]) max[0] = (int) m.point.getX();
  		if (m.point.getY() < min[1]) min[1] = (int) m.point.getY();
  		if (m.point.getY() > max[1]) max[1] = (int) m.point.getY();
  		if (m.point.getZ() < min[2]) min[2] = (int) m.point.getZ();
  		if (m.point.getZ() > max[2]) max[2] = (int) m.point.getZ();
  	}
  	//find the point closer to the corner of that cube
  	double[] distanceToCorner = new double[]{999,999,999,999,999,999,999,999,999,999,999,999};
  	Map3DPoint[] pointCloserToCorner = new Map3DPoint[12];
  	Point[] cornerPoint = new Point[]{new Point(min[0],max[1],max[2],0,0,0),new Point(min[0],min[1],max[2],0,0,0), new Point(min[0],min[1],min[2],0,0,0), new Point(min[0],max[1],min[2],0,0,0), new Point(max[0],max[1],max[2],0,0,0),new Point(max[0],min[1],max[2],0,0,0), new Point(max[0],min[1],min[2],0,0,0), new Point(max[0],max[1],min[2],0,0,0), new Point(min[0],min[1],((max[2]-min[2])/2)+min[2],0,0,0),new Point(((max[0]-min[0])/2)+min[0],min[1],max[2],0,0,0),new Point(max[0],min[1],((max[2]-min[2])/2)+min[2],0,0,0),new Point(((max[0]-min[0])/2)+min[0],min[1],min[2],0,0,0)};
  	int[] closestPointToCornerType = new int[3];
  	double[] closestPointToCornerDistance = new double[]{999,999,999};
  	for (Map3DPoint m : cloudMap.values()) {
  		for (int i = 0; i < 12; i++) {
  			if (cornerPoint[i].distanceTo(m.point) < distanceToCorner[i]) {
  				distanceToCorner[i] = cornerPoint[i].distanceTo(m.point);
  				pointCloserToCorner[i] = m;
  			}
  		}
  	}
  	//of the point closer to the corner, find the two smaller distance
  	for (int i = 0; i < 8; i++){
  		if (distanceToCorner[i] < closestPointToCornerDistance[1]) {
  			closestPointToCornerDistance[2] = distanceToCorner[i];
  			closestPointToCornerType[2] = i;
  			if (closestPointToCornerDistance[2] < closestPointToCornerDistance[1]) {
  				double cptcd = closestPointToCornerDistance[1];
  				int cptct = closestPointToCornerType[1];
  				closestPointToCornerDistance[1] = closestPointToCornerDistance[2];
  				closestPointToCornerType[1] = closestPointToCornerType[2];
  				closestPointToCornerDistance[2] = cptcd;
  				closestPointToCornerType[2] = cptct;
  			}
  			if (closestPointToCornerDistance[1] < closestPointToCornerDistance[0]) {
  				double cptcd = closestPointToCornerDistance[0];
  				int cptct = closestPointToCornerType[0];
  				closestPointToCornerDistance[0] = closestPointToCornerDistance[1];
  				closestPointToCornerType[0] = closestPointToCornerType[1];
  				closestPointToCornerDistance[1] = cptcd;
  				closestPointToCornerType[1] = cptct;
  			}
  		}
		}
  	List<Integer> backpoint = Arrays.asList(0,3,4,7);
  	if (closestPointToCornerType[0] == (closestPointToCornerType[1] + 6) % 8) { //cylinder seen at angle
  		//make a vector from the opposite point
  		origin = pointCloserToCorner[closestPointToCornerType[0]].point;
  		end = pointCloserToCorner[closestPointToCornerType[1]].point;
  		double[] vector = getVector();
  		PVector vectorOE = new PVector((float)vector[0],(float)vector[1],(float)vector[2]);
  		//radius will be equal to half the biggest distance between points and the vector
  		double distance = 0;
  		for (Map3DPoint m : cloudMap.values()) {
  			PVector vectorPO = new PVector((float)origin.getX(), (float)origin.getY(), (float)origin.getZ());
  			vectorPO.sub((float)m.point.getX(), (float)m.point.getY(), (float)m.point.getZ());
  			PVector cross = vectorOE.cross(vectorPO);
  			double d = Math.pow(cross.x, 2)+ Math.pow(cross.y, 2) + Math.pow(cross.z, 2);
  			double d1 = Math.pow(vectorOE.x, 2) + Math.pow(vectorOE.y, 2) + Math.pow(vectorOE.z, 2);
  			double d2 = Math.sqrt(d/d1);
  			if (d2 > distance) distance = d2;
  		}
  		radius = distance / 2;
  		//adjust the origin point and the end point with the radius data
  		Point pointToUse = new Point(0,0,0,0,0,0);
  		if (closestPointToCornerType[0] == 0) pointToUse = cornerPoint[5];
  		if (closestPointToCornerType[0] == 1) pointToUse = cornerPoint[4];
  		if (closestPointToCornerType[0] == 2) pointToUse = cornerPoint[7];
  		if (closestPointToCornerType[0] == 3) pointToUse = cornerPoint[6];
  		if (closestPointToCornerType[0] == 4) pointToUse = cornerPoint[1];
  		if (closestPointToCornerType[0] == 5) pointToUse = cornerPoint[0];
  		if (closestPointToCornerType[0] == 6) pointToUse = cornerPoint[3];
  		if (closestPointToCornerType[0] == 7) pointToUse = cornerPoint[2];
  		PVector vO = new PVector((float)origin.getX(), (float)origin.getY(), (float)origin.getZ());
  		PVector vC = new PVector((float)pointToUse.getX(),(float)pointToUse.getY(),(float)pointToUse.getZ());
  		PVector vA = PVector.sub(vO, vC);
  		float lengthOfA = (float) Math.sqrt(Math.pow(vA.x, 2) + Math.pow(vA.y, 2) + Math.pow(vA.z, 2));
  		vA.div(lengthOfA);
  		vA.mult((float) radius);
  		Point delta = new Point(vA.x,vA.y,vA.z,0,0,0);
  		origin = origin.add(delta);
  		end = end.subtract(delta);
  	}
  	else if (backpoint.contains(closestPointToCornerType[0]) && backpoint.contains(closestPointToCornerType[1]) && backpoint.contains(closestPointToCornerType[2])){
  		//cylinder seen from the side (both end and origin at similar distance from the kinect
  		//can be horizontal or vertical need to test wich fit the best
  		if (distanceToCorner[8] < distanceToCorner[9]) {
  			origin = pointCloserToCorner[8].point;
  			end = pointCloserToCorner[10].point;
    		radius = Math.max(((max[2]-min[2])/2), max[1]-min[1]);
  		}
  		else {
  			origin = pointCloserToCorner[9].point;
  			end = pointCloserToCorner[11].point;
    		radius = Math.max(((max[0]-min[0])/2), max[1]-min[1]);
  		}
  	}
  	else { //other shape... for now doing cylinder seen from end side
  		//origin is in the front (minY), radius = max(maxX-minX, maxZ-minZ), end = maxY
  		origin = new Point((double)((max[0]-min[0])/2)+min[0],(double)min[1],(double)((max[2]-min[2])/2)+min[2],0,0,0);
  		end = new Point((double)((max[0]-min[0])/2)+min[0],(double)max[1],(double)((max[2]-min[2])/2)+min[2],0,0,0);
  		radius = Math.max(((max[0]-min[0])/2), (max[2]-min[2])/2);
  	}
  	fromKinect = true;
  	Log.info(name);
  	Log.info(" origin ", origin.getX(), ",", origin.getY(), ",", origin.getZ());
  	Log.info(" end ", end.getX(), ",", end.getY(), ",", end.getZ());
  	Log.info(" radius ", radius);
  	Log.info("------------");
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
  
  public double getLength(){
  	return (Math.sqrt(Math.pow(origin.getX()-end.getX(), 2)+Math.pow(origin.getY()-end.getY(), 2)+Math.pow(origin.getZ()-end.getZ(), 2)));
  }
  
  public boolean isFromKinect() {
  	return fromKinect;
  }
}