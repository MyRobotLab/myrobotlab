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

import java.util.ArrayList;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class TestThrower extends Service {
	
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(TestThrower.class);
	public int cnt = 0;
	public int pulseLimit = 20;
	public ArrayList<RapidThrower> pitchers = new ArrayList<RapidThrower>();

	public class RapidThrower extends Thread {
		Service myService;
		int count = 300;
		int throwInterval = 10;

		RapidThrower(Service myService, int count, int throwInterval) {
			this.myService = myService;
			this.start();
		}
		
		RapidThrower(Service myService) {
			this(myService, 100, 10);
		}

		@Override
		public void run() {
			for (int i = 0; i < count; ++i)
			{
					++count;
					invoke("pitch", count);
					Service.sleep(throwInterval);
			}

		}
	}

	public TestThrower(String n) {
		super(n);
	}

	@Override
	public String[] getCategories() {
		return new String[] { "framework", "testing" };
	}

	@Override
	public String getDescription() {
		return "service for test message sending";
	}

	public Integer pitch(Integer number) {
		++cnt;
		log.info("noPitchInteger null ");
		return number;
	}
	
	public void pitchInt(int number){
		for (int i = 0; i < number; ++i) {
			invoke("pitch", i);
		}
	}
	
	public void multiPitcher(int pitchers){
		for (int i = 0; i < pitchers; ++i){
			new RapidThrower(this, 300, 10);
		}
	}
	
	public void multiPitcher(int pitchers, int pitches, int throwInterval){
		for (int i = 0; i < pitchers; ++i){
			new RapidThrower(this, pitches, throwInterval);
		}
	}

	public Integer throwInteger(Integer count) {
		log.info("throwInteger " + count);
		return count;
	}

	public void throwNothing() {
		log.info("throwNothing");
	}

	public String throwString(String nameOfTargetService, String nameOfMethod, String data) {
		send(nameOfTargetService, nameOfMethod, data);
		return data;
	}

}
