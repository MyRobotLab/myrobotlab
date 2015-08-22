package org.myrobotlab.math;

/**
 * A place to have some handy math functions.
 * 
 * @author kwatters
 *
 */
public class MathUtils {

	// convert degrees to radians.
	public static double degToRad(double degrees) {
		return degrees * Math.PI/180.0;
	};
	public static double radToDeg(double radians) {
		return radians * 57.2957795;
	}
	
}
