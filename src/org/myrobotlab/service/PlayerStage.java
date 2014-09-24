/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.service;

import java.text.FieldPosition;
import java.text.NumberFormat;

import javaclient3.PlayerClient;
import javaclient3.PlayerException;
import javaclient3.Position2DInterface;
import javaclient3.SonarInterface;
import javaclient3.structures.PlayerConstants;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author GroG
 * 
 *         Interface service for player/stage
 *         http://playerstage.sourceforge.net/ using Javaclient3
 *         http://java-player.sourceforge.net/examples-3.php#Navigator
 */

public class PlayerStage extends Service {

	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(PlayerStage.class.getCanonicalName());

	public PlayerStage(String n) {
		super(n);
	}

	// define minimum/maximum allowed values for the SONAR sensors
	static float SONAR_MIN_VALUE = 0.2f;
	static float SONAR_MAX_VALUE = 5.0f;
	// define the threshold (any value under this is considered an obstacle)
	static float SONAR_THRESHOLD = 0.5f;
	// define the wheel diameter (~example for a Pioneer 3 robot)
	static float WHEEL_DIAMETER = 24.0f;

	// define the default rotational speed in rad/s
	static float DEF_YAW_SPEED = 0.50f;

	// array to hold the SONAR sensor values
	static float[] sonarValues;
	// translational/rotational speed
	static float xspeed, yawspeed;
	static float leftSide, rightSide;

	static NumberFormat fmt = NumberFormat.getInstance();

	public static void main(String[] args) {
		PlayerClient robot = null;
		Position2DInterface posi = null;
		SonarInterface soni = null;

		try {
			// Connect to the Player server and request access to Position and
			// Sonar
			robot = new PlayerClient("localhost", 6665);
			posi = robot.requestInterfacePosition2D(0, PlayerConstants.PLAYER_OPEN_MODE);
			soni = robot.requestInterfaceSonar(0, PlayerConstants.PLAYER_OPEN_MODE);
		} catch (PlayerException e) {
			System.err.println("SpaceWandererExample: > Error connecting to Player: ");
			System.err.println("    [ " + e.toString() + " ]");
			System.exit(1);
		}

		robot.runThreaded(-1, -1);

		while (true) {
			// get all SONAR values
			while (!soni.isDataReady())
				;
			sonarValues = soni.getData().getRanges();

			// ignore erroneous readings/keep interval [SONAR_MIN_VALUE;
			// SONAR_MAX_VALUE]
			for (int i = 0; i < soni.getData().getRanges_count(); i++)
				if (sonarValues[i] < SONAR_MIN_VALUE)
					sonarValues[i] = SONAR_MIN_VALUE;
				else if (sonarValues[i] > SONAR_MAX_VALUE)
					sonarValues[i] = SONAR_MAX_VALUE;
			System.out.println(decodeSonars(soni));

			// read and average the sonar values on the left and right side
			leftSide = (sonarValues[1] + sonarValues[2]) / 2; // + sonarValues
																// [3]) / 3;
			rightSide = (sonarValues[5] + sonarValues[6]) / 2; // + sonarValues
																// [4]) / 3;

			// use a divider for the velocities depending on your desired speed
			// (mm/s, m/s, etc)
			leftSide = leftSide / 10;
			rightSide = rightSide / 10;

			// calculate the translational and rotational velocities
			xspeed = (leftSide + rightSide) / 2;
			yawspeed = (float) ((leftSide - rightSide) * (180 / Math.PI) / WHEEL_DIAMETER);

			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}

			// if the path is clear on the left OR on the right, use
			// {x,yaw}speed
			if (((sonarValues[1] > SONAR_THRESHOLD) && (sonarValues[2] > SONAR_THRESHOLD) && (sonarValues[3] > SONAR_THRESHOLD))
					|| ((sonarValues[4] > SONAR_THRESHOLD) && (sonarValues[5] > SONAR_THRESHOLD) && (sonarValues[6] > SONAR_THRESHOLD)))
				posi.setSpeed(xspeed, yawspeed);
			else
			// if we have obstacles in front (both left and right), rotate
			if (sonarValues[0] < sonarValues[7])
				posi.setSpeed(0, -DEF_YAW_SPEED);
			else
				posi.setSpeed(0, DEF_YAW_SPEED);
		}
	}

	// Misc routines for nice alignment of text on screen
	static String align(NumberFormat fmt, float n, int sp) {
		StringBuffer buf = new StringBuffer();
		FieldPosition fpos = new FieldPosition(NumberFormat.INTEGER_FIELD);
		fmt.format(n, buf, fpos);
		for (int i = 0; i < sp - fpos.getEndIndex(); ++i)
			buf.insert(0, ' ');
		return buf.toString();
	}

	public static String decodeSonars(SonarInterface soni) {
		String out = "\nSonar vars: \n";
		for (int i = 0; i < soni.getData().getRanges_count(); i++) {
			out += " [" + align(fmt, i + 1, 2) + "] = " + align(fmt, soni.getData().getRanges()[i], 5);
			if (((i + 1) % 8) == 0)
				out += "\n";
		}
		return out;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return "stubbed out for Player Stage - partially implemented";
	}
}
