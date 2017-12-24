package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.myrobotlab.openni.PVector;
import org.python.jline.internal.Log;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

public class CollisionItem{
  Point origin = null;
  Point end = null;
  String name;
  double radius=0.0;
  ArrayList<String> ignore = new ArrayList<String>();
  ArrayList<String> done = new ArrayList<String>();
  boolean fromKinect = false;
  public HashMap<Integer[], Map3DPoint> cloudMap;
  private boolean render = false;
  private Mesh mesh;
  
  public CollisionItem(Point origin, Point end, String name, double radius, boolean render) {
    super();
    this.origin = origin;
    this.end = end;
    if (name == null) {
      name = UUID.randomUUID().toString();
    }
    this.name = name;
    this.radius = radius;
    this.render = render;
  }
  
  public CollisionItem(Point origin, Point end, String name) {
    super();
    this.origin = origin;
    this.end = end;
    this.name = name;
  }
  
  public CollisionItem(HashMap<Integer[],Map3DPoint> cloudMap) {
    super();
  	name = UUID.randomUUID().toString();
  	this.cloudMap = cloudMap;
  	setRender(true);
  	buildCollisionItem(cloudMap);
  	//buildMesh(cloudMap);
  }

  public void buildMesh(HashMap<Integer[], Map3DPoint> cloudMap2) {
    mesh = new Mesh();
    Vector3f[] vertices = new Vector3f[cloudMap2.size()];
    int i=0;
    for (Map3DPoint point : cloudMap2.values()) {
      vertices[i++] = new Vector3f((float)point.point.getX(), (float)point.point.getZ(), (float)point.point.getY());
    }
    ArrayList<Integer> index = new ArrayList<Integer>();
    for (int j = 0; j < vertices.length; j++){
      //find the two closest vertices
      int[] closest = new int[]{0,0,0,0};
      float[] distances = new float[]{9999,9999,9999,9999}; 
      for (int jj = 0; jj < vertices.length; jj++){
        if (jj == j) {
          continue;
        }
        if (vertices[j].distance(vertices[jj]) < distances[2]){
          closest[2] = jj;
          distances[2] = vertices[j].distance(vertices[jj]);
          if (distances[2] < distances[1]) {
            distances[3] = distances[1];
            closest[3] = closest[1];
            distances[1] = distances[2];
            closest[1] = closest[2];
            distances[2] = distances[3];
            closest[2] = closest[3];
          }
          if (distances[1] < distances[0]) {
            distances[3] = distances[0];
            closest[3] = closest[0];
            distances[0] = distances[1];
            closest[0] = closest[1];
            distances[1] = distances[3];
            closest[1] = closest[3];
          }
        }
      }
      index.add(j);
      index.add(closest[0]);
      index.add(closest[1]);
      index.add(j);
      index.add(closest[1]);
      index.add(closest[0]);
      index.add(j);
      index.add(closest[1]);
      index.add(closest[2]);
      index.add(j);
      index.add(closest[2]);
      index.add(closest[1]);
      index.add(j);
      index.add(closest[0]);
      index.add(closest[2]);
      index.add(j);
      index.add(closest[2]);
      index.add(closest[0]);
    }
    mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
    int[] indexes = new int[index.size()];
    int kk = 0;
    for(Integer k:index) {
      indexes[kk++] = k.intValue();
    }
    mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(indexes));
  }

  /**
   * @return the mesh
   */
  public Mesh getMesh() {
    return mesh;
  }

  public CollisionItem(CollisionItem ci) {
    super();
  	this.origin = new Point(ci.origin);
  	this.end = new Point(ci.end);
  	this.name = ci.name;
  	this.radius = ci.radius;
  	this.fromKinect = ci.fromKinect;
		this.render = ci.render;
		this.ignore = new ArrayList<String>(ci.ignore);
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
  	//List<Integer> backpoint = Arrays.asList(0,3,4,7);
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
  		origin = origin.subtract(delta);
  		end = end.add(delta);
  	}
  	else {
  	  //build 3 cylinders and find the one that contains more point
  	  PVector[] vector = new PVector[]{new PVector(max[0]-min[0],0,0), new PVector(0,max[1]-min[1], 0), new PVector(0, 0, max[2]-min[2])};
  	  PVector[] origin = new PVector[]{new PVector(min[0], vector[1].y/2+min[1], vector[2].z/2+min[2]), new PVector(vector[0].x/2+min[0], min[1], vector[2].z/2+min[2]), new PVector(vector[0].x/2+min[0], vector[1].y/2+min[1], min[2])};
      double[] radius = new double[]{(max[0]-min[0])/2, (max[1]-min[1])/2, (max[2]-min[2])/2};
      int count[] = new int[]{0,0,0};
      for (Map3DPoint map : cloudMap.values()) {
        PVector point = new PVector((float)map.point.getX(), (float)map.point.getY(), (float)map.point.getZ());
        PVector d = PVector.div(vector[0], vector[0].x);
        PVector v = PVector.sub(origin[0], point);
        float t = v.dot(d);
        PVector P = PVector.add(origin[0], PVector.mult(d, t));
        float distance = P.dist(point);
        if (distance <= radius[0]) count[0]++;
        d = PVector.div(vector[1], vector[1].y);
        v = PVector.sub(origin[1], point);
        t = v.dot(d);
        P = PVector.add(origin[1], PVector.mult(d, t));
        distance = P.dist(point);
        if (distance <= radius[1]) count[1]++;
        d = PVector.div(vector[2], vector[2].z);
        v = PVector.sub(origin[2], point);
        t = v.dot(d);
        P = PVector.add(origin[2], PVector.mult(d, t));
        distance = P.dist(point);
        if (distance <= radius[2]) count[2]++;
      }
      if (count[0] >= count[1] && count[0] >= count[2]){
        this.origin = new Point(origin[0].x, origin[0].y, origin[0].z, 0, 0, 0);
        PVector e = PVector.add(origin[0], vector[0]);
        end = new Point(e.x, e.y, e.z, 0, 0, 0);
        this.radius = radius[0];
      }
      else if (count[1] >= count[0] && count[1] >= count[2]) {
        this.origin = new Point(origin[1].x, origin[1].y, origin[1].z, 0, 0, 0);
        PVector e = PVector.add(origin[1], vector[1]);
        end = new Point(e.x, e.y, e.z, 0, 0, 0);
        this.radius = radius[1];
      }
      else {
        this.origin = new Point(origin[2].x, origin[2].y, origin[2].z, 0, 0, 0);
        PVector e = PVector.add(origin[2], vector[2]);
        end = new Point(e.x, e.y, e.z, 0, 0, 0);
        this.radius = radius[2];

      }
  	//else if (backpoint.contains(closestPointToCornerType[0]) && backpoint.contains(closestPointToCornerType[1]) && backpoint.contains(closestPointToCornerType[2])){
  		//cylinder seen from the side (both end and origin at similar distance from the kinect
  		//can be horizontal or vertical need to test wich fit the best
//  		if (distanceToCorner[8] < distanceToCorner[9]) {
//  			origin = pointCloserToCorner[8].point;
//  			end = pointCloserToCorner[10].point;
//    		radius = Math.max(((max[2]-min[2])/2), max[1]-min[1]);
//  		}
//  		else {
//  			origin = pointCloserToCorner[9].point;
//  			end = pointCloserToCorner[11].point;
//    		radius = Math.max(((max[0]-min[0])/2), max[1]-min[1]);
//  		}
  	}
//  	else { //other shape... for now doing cylinder seen from end side
//  		//origin is in the front (minY), radius = max(maxX-minX, maxZ-minZ), end = maxY
//  		origin = new Point((double)((max[0]-min[0])/2)+min[0],(double)min[1],(double)((max[2]-min[2])/2)+min[2],0,0,0);
//  		end = new Point((double)((max[0]-min[0])/2)+min[0],(double)max[1],(double)((max[2]-min[2])/2)+min[2],0,0,0);
//  		radius = Math.max(((max[0]-min[0])/2), (max[2]-min[2])/2);
//  	}
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
  
  public void removeIgnore(String ignore) {
    for (String x : this.ignore) {
      if (x.equals(ignore)){
        this.ignore.remove(x);
        break;
      }
    }
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

  /**
   * @return the render
   */
  public boolean isRender() {
    return render;
  }

  /**
   * @param render the render to set
   */
  public void setRender(boolean render) {
    this.render = render;
  }
  

}