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

import java.awt.Rectangle;

import org.myrobotlab.framework.Service;
import org.myrobotlab.gp.GP;
import org.myrobotlab.gp.GPMessageBestFound;
import org.myrobotlab.gp.GPMessageEvaluatingIndividual;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class GeneticProgramming extends Service {

	public final static Logger log = LoggerFactory.getLogger(GeneticProgramming.class.getCanonicalName());
	private static final long serialVersionUID = 1L;

	GP gp1 = null;

	public GeneticProgramming(String n) {
		super(n);
	}



	Thread p = null;

	public void createGP() {
		gp1 = new GP(this);
		p = new Thread(gp1);
		p.start();
	}

	public void evalCallBack(Rectangle o) {
		if (gp1 != null) {
			gp1.evalCallBack(o); // use abstract class for GP and/or its eval
									// data
		}
	}

	public GPMessageEvaluatingIndividual publishInd(GPMessageEvaluatingIndividual ind) {
		return ind;
	}

	public GPMessageBestFound publish(GPMessageBestFound best) {
		return best;
	}

	public static void main(String[] args) {
		Arduino arduino = new Arduino("arduino");
		Servo hip = new Servo("hip");
		hip.attach(arduino.getName(), 9);
		Servo knee = new Servo("knee");
		knee.attach(arduino.getName(), 10);
		GeneticProgramming gp = new GeneticProgramming("gp");
		arduino.startService();
		knee.startService();
		hip.startService();
		gp.startService();
		gp.createGP();
		boolean forever = true;
		while (forever) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				logException(e);
			}
		}
	}

	@Override
	public String getDescription() {
		return "experiment in genetic programming";
	}

}
