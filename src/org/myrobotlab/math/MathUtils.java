package org.myrobotlab.math;

/**
 * A place to have some handy math functions.
 * 
 * @author kwatters
 *
 */
public class MathUtils {

	// convert degrees to radians.
	static public double degToRad(double degrees) {
		return degrees * Math.PI / 180.0;
	};

	static public double radToDeg(double radians) {
		return radians * 57.2957795;
	}

	static public String msToString(long ms) {
		long seconds = ms / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;
		String time = days + ":" + hours % 24 + ":" + minutes % 60 + ":" + seconds % 60;
		return time;
	}

	/*
	 * Converts Gray encoded values to their corresponding decimal value
	 */
	static public int decimalToGray(int decimal) {

		return (decimal ^ (decimal >> 1));
	}

	/*
	 * Converts a decimal number to it's corresponding Gray encoded value
	 */
	static public int grayToDecimal(int gray) {
		gray ^= (gray >> 16);
		gray ^= (gray >> 8);
		gray ^= (gray >> 4);
		gray ^= (gray >> 2);
		gray ^= (gray >> 1);
		return gray;
	}
}
