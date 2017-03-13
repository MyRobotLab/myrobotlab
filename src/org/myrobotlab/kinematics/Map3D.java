/**
 * 
 */
package org.myrobotlab.kinematics;

import java.util.ArrayList;
import java.util.HashMap;

import org.myrobotlab.math.MathUtils;
import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.openni.PVector;
import org.python.jline.internal.Log;

/**
 * 
 * This class register the 3d environement detected by the kinect sensor
 * @author Christian
 *
 */
public class Map3D {

	public enum CoordStateValue {
		UNDEFINED,
		EMPTY,
		FILL;
	}
	static double fx_d = 1.0 / 5.9421434211923247e+02;
	static double fy_d = 1.0 / 5.9104053696870778e+02;
	static double cx_d = 3.3930780975300314e+02;
	static double cy_d = 2.4273913761751615e+02;
	
	public int widthImage = 640;
	public int heighImage = 480;
	public int maxDepthValue = 2000;
	public int closestDistance = 450;
	public int fartestDistance = 1000;
	
	public int skip = 10;
	HashMap<Integer,HashMap<Integer,HashMap<Integer,Map3DPoint>>> coordValue = new HashMap<Integer,HashMap<Integer,HashMap<Integer,Map3DPoint>>>();
	private Point kinectPosition;
	//ArrayList<Map3DPoint> cloudMap = new ArrayList<Map3DPoint>();
	HashMap<Integer[],Map3DPoint> cloudMap = new HashMap<Integer[],Map3DPoint>();
	ArrayList<HashMap<Integer[],Map3DPoint>> cloudMapGroup = new ArrayList<HashMap<Integer[],Map3DPoint>>();
	

	private int distanceBetweenPoints = 4 * skip;	
	
	
	public Map3D() {
		
	}

	public void processDepthMap(OpenNiData data) {
		cloudMap.clear();
		PVector[] depthData = data.depthMapRW;
		for (int x = skip; x < widthImage - skip; x+=skip ) {
			for (int y = skip; y < heighImage - skip; y+=skip) {
				int index = x + y*widthImage;
				PVector loc = null;
				if (depthData[index].z > closestDistance && depthData[index].z <= fartestDistance) {
					for (float z = closestDistance; z < depthData[index].z - skip; z+=(float)skip) {
						loc = PVector.div(depthData[index], depthData[index].z);
						loc = PVector.mult(loc, z);
						addCoordValue(loc.x, loc.y, loc.z, CoordStateValue.EMPTY);
					}
					addCoordValue(depthData[index].x, depthData[index].y, depthData[index].z, CoordStateValue.FILL);
					for (float z = depthData[index].z + (float)skip; z < fartestDistance; z+=(float)skip) {
						loc = PVector.div(depthData[index], depthData[index].z);
						loc = PVector.mult(loc, z);
						addCoordValue(loc.x, loc.y, loc.z, CoordStateValue.UNDEFINED);
					}
				}
				if (depthData[index].z > fartestDistance) {
					for (float z = closestDistance; z <= depthData[index].z && z <= fartestDistance; z+=(float)skip) {
						loc = PVector.div(depthData[index], depthData[index].z);
						loc = PVector.mult(loc, z);
						addCoordValue(loc.x, loc.y, loc.z, CoordStateValue.EMPTY);
					}
				}
			}
		}
//		for (Map3DPoint p : cloudMap.values()) {
//			Log.info("x:", p.point.getX());
//			Log.info("y:", p.point.getY());
//			Log.info("z:", p.point.getZ());
//			Log.info("-----------------");
//		}
		groupPoints();
		//buildMesh();
	}


	private void groupPoints() {
		cloudMapGroup.clear();
		for (Map3DPoint point : cloudMap.values()) {
			boolean found = false;
			Integer[] newPoint = new Integer[]{(int)point.point.getX(), (int)point.point.getY(), (int)point.point.getZ()};
			for (int i = 0; i < cloudMapGroup.size(); i++) {
				HashMap<Integer[],Map3DPoint> group = cloudMapGroup.get(i);
				if (group.size() > 0) {
					for (Map3DPoint checkPoint : group.values()) {
						if (checkPoint.point.distanceTo(point.point) < distanceBetweenPoints) {
							Integer[] index = new Integer[]{(int)point.point.getX(), (int)point.point.getY(), (int)point.point.getZ()};
							group.put(index, point);
							cloudMapGroup.set(i, group);
							found = true;
							break;
						}
					}
				}
//				for (int x = (int)point.point.getX() - distanceBetweenPoints; x < (int)point.point.getX() + distanceBetweenPoints && !found; x++) {
//					for (int y = (int)point.point.getY() - distanceBetweenPoints; y < (int)point.point.getY() + distanceBetweenPoints && !found; y++) {
//						for (int z = (int)point.point.getZ() - distanceBetweenPoints; z < (int)point.point.getZ() + distanceBetweenPoints && !found; z++) {
//							Integer[] index = new Integer[]{x, y, z};
//							if (cloudMapGroup.get(i).containsKey(index)) {
//								cloudMapGroup.get(i).put(newPoint, point);
//								found = true;
//							}
//						}
//					}
//				}
			}
			if (!found) {
				HashMap<Integer[],Map3DPoint> e = new HashMap<Integer[],Map3DPoint>();
				e.put(newPoint, point);
				cloudMapGroup.add(e);
			}
		}
		for (int i = 0; i < cloudMapGroup.size(); i++){
			if (cloudMapGroup.get(i).size() < 5) {
				cloudMapGroup.remove(i);
				i--;
			}
		}
		//do a second pass
		boolean merge = false;
		for (int i = 0; i < cloudMapGroup.size(); i++){
			for (Map3DPoint p : cloudMapGroup.get(i).values()) {
				for (int ii = i + 1; ii < cloudMapGroup.size(); ii++){
					for (Map3DPoint pp : cloudMapGroup.get(ii).values()) {
						if (p.point.distanceTo(pp.point) < distanceBetweenPoints) {
							HashMap<Integer[],Map3DPoint> group = cloudMapGroup.get(i);
							group.putAll(cloudMapGroup.get(ii));
							cloudMapGroup.set(i, group);
							cloudMapGroup.remove(ii);
							ii--;
							merge = true;
							break;
						}
					}
					if (merge) break;
				}
				if (merge) break;
			}
			if (merge) {
				merge = false;
				i = -1;
			}
		}
		Log.info("Found {} object(s)", cloudMapGroup.size());
	}

	private void addCoordValue(double xpos, double ypos, double zpos, CoordStateValue value) {
		addCoordValue((int)xpos, (int)ypos, (int)zpos, value);
	}
	
	private void addCoordValue(int xpos, int ypos, int zpos, CoordStateValue value) {
		//need to rotate and translate the location depending on the position of the kinect
		//rotate
		//must change the x axis so the coordinate are in the right orientation
		//xpos *= -1;
    double roll = MathUtils.degToRad(kinectPosition.getRoll());
    double pitch = MathUtils.degToRad(kinectPosition.getPitch());
    double yaw = MathUtils.degToRad(kinectPosition.getYaw());
    Matrix trMatrix = Matrix.translation(kinectPosition.getX(), kinectPosition.getY(), kinectPosition.getZ());
    Matrix rotMatrix = Matrix.xRotation(roll).multiply(Matrix.yRotation(pitch).multiply(Matrix.zRotation(yaw)));
    Matrix coord = Matrix.translation(xpos, zpos, ypos);
    Matrix inputMatrix = trMatrix.multiply(rotMatrix).multiply(coord);

    Point pOut = new Point(inputMatrix.elements[0][3], inputMatrix.elements[1][3], inputMatrix.elements[2][3], 0, 0, 0);
		
		//convert to the coordinate use by our ik engine and reduce the resolution
		double posx = (int)((int)pOut.getX()/skip*skip);
		double posy = (int)((int)pOut.getY()/skip*skip);
		double posz = (int)((int)pOut.getZ()/skip*skip);
		
		Map3DPoint map = new Map3DPoint();
		map.point = pOut;
		map.value = value;
		Integer[] index = new Integer[]{(int)posx,(int)posy,(int)posz};
		//int mapIndex = getCloudMapIndex((int)posx, (int)posy, (int)posz);
		switch(value){
			case EMPTY: {
				if (cloudMap.containsKey(index)) {
					cloudMap.remove(index);
				}
				break;
			}
			case FILL: {
				cloudMap.put(index, map);
				break;
			}
			case UNDEFINED: {
//				cloudMap.putIfAbsent(index, map);
				break;
			}
		}
		
//		HashMap<Integer,HashMap<Integer,Map3DPoint>> y = coordValue.get((int)posx);
//		if (y == null) {
//			y = new HashMap<Integer,HashMap<Integer,Map3DPoint>>();
//		}
//		HashMap<Integer,Map3DPoint> z = y.get((int)posy);
//		if (z == null) {
//			z = new HashMap<Integer,Map3DPoint>();
//		}
//		switch (value){
//			case EMPTY:
//				if (z.get((int)posz) != null) {
//					z.remove((int)posz);
//				}
//				break;
//			case FILL: {
//				Map3DPoint o = new Map3DPoint();
//				o.value = value;
//				z.put((int)posz, o);
//				break;
//			}
//			case UNDEFINED:{
//				if (z.get((int)posz) == null){
//					Map3DPoint o = new Map3DPoint();
//					o.value = value;
//					z.put((int)posz, o);
//				}
//				break;
//			}
//		}
//		y.put((int)posy, z);
//		coordValue.put((int)posx, y);
	}
	
	public CoordStateValue getCoordValue(double xpos, double ypos, double zpos) {
		return getCoordValue((int)xpos, (int)ypos, (int)zpos);
	}
	
	public CoordStateValue getCoordValue(int xpos, int ypos, int zpos) {
		int posx = xpos/skip*skip;
		int posy = ypos/skip*skip;
		int posz = xpos/skip*skip;
		Integer[] index = new Integer[]{posx,posy,posz};
		if (cloudMap.containsKey(index)) {
			return cloudMap.get(index).value;
		}
		return CoordStateValue.EMPTY;
//		HashMap<Integer,HashMap<Integer,Map3DPoint>> y = coordValue.get(posx);
//		if (y == null) {
//			return CoordStateValue.EMPTY;
//		}
//		HashMap<Integer,Map3DPoint> z = y.get(posy);
//		if (z == null) {
//			return CoordStateValue.EMPTY;
//		}
//		return CoordStateValue.FILL; //return FILL even if it's undefined (we don't know if it's fill or not, better not go.
//		//return z.get(posz);
	}

	public void updateKinectPosition(Point currentPosition) {
		kinectPosition = currentPosition;
	}
	
	public ArrayList<HashMap<Integer[],Map3DPoint>> getObject() {
		return cloudMapGroup;
	}
	
}

