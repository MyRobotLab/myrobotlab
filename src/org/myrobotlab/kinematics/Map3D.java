/**
 * 
 */
package org.myrobotlab.kinematics;

import java.util.HashMap;

import org.myrobotlab.openni.OpenNiData;
import org.myrobotlab.openni.PVector;

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
	HashMap<Integer,HashMap<Integer,HashMap<Integer,CoordStateValue>>> coordValue = new HashMap<Integer,HashMap<Integer,HashMap<Integer,CoordStateValue>>>();
	public Map3D() {
		
	}

	public void processDepthMap(OpenNiData data) {
		PVector[] depthData = data.depthMapRW;
		for (int x = 0; x < widthImage; x+=skip ) {
			for (int y = 0; y < heighImage; y+=skip) {
				int index = x + y*widthImage;
				PVector loc = null;
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
		}
	}


	private void addCoordValue(double xpos, double ypos, double zpos, CoordStateValue value) {
		addCoordValue((int)xpos, (int)ypos, (int)zpos, value);
	}
	
	private void addCoordValue(int xpos, int ypos, int zpos, CoordStateValue value) {
		int posx = xpos/skip*skip;
		int posy = ypos/skip*skip;
		int posz = xpos/skip*skip;
		HashMap<Integer,HashMap<Integer,CoordStateValue>> y = coordValue.get(posx);
		if (y == null) {
			y = new HashMap<Integer,HashMap<Integer,CoordStateValue>>();
		}
		HashMap<Integer,CoordStateValue> z = y.get(posy);
		if (z == null) {
			z = new HashMap<Integer,CoordStateValue>();
		}
		switch (value){
			case EMPTY:
				if (z.get(posz) != null) {
					z.remove(posz);
				}
				break;
			case FILL:
				z.put(posz, value);
				break;
			case UNDEFINED:
				if (z.get(posz) == null){
					z.put(posz, value);
				}
				break;
		}
		y.put(posy, z);
		coordValue.put(posx, y);
	}
	
	public CoordStateValue getCoordValue(double xpos, double ypos, double zpos) {
		return getCoordValue((int)xpos, (int)ypos, (int)zpos);
	}
	
	public CoordStateValue getCoordValue(int xpos, int ypos, int zpos) {
		int posx = xpos/skip*skip;
		int posy = ypos/skip*skip;
		int posz = xpos/skip*skip;
		HashMap<Integer,HashMap<Integer,CoordStateValue>> y = coordValue.get(posx);
		if (y == null) {
			return CoordStateValue.EMPTY;
		}
		HashMap<Integer,CoordStateValue> z = y.get(posy);
		if (z == null) {
			return CoordStateValue.EMPTY;
		}
		return CoordStateValue.FILL; //return FILL even if it's undefined (we don't know if it's fill or not, better not go.
		//return z.get(posz);
	}
}

