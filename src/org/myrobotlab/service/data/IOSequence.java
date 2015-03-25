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

package org.myrobotlab.service.data;

import java.io.Serializable;
import java.util.ArrayList;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class IOSequence implements Serializable {
	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(IOSequence.class);

	public int ID;
	public ArrayList<IOSequenceEntry> sequenceList; // list of a IOAddress
													// values to run sequentialy

	// option constants

	public static String name() {
		if (log.isDebugEnabled()) {
			StringBuilder logString = new StringBuilder("IOSequence.getName()()");
			log.debug("{}", logString);
		} // if

		String ret = new String("IOSequence");
		return ret;
	}

	public static String scope() {
		String ret = new String("myrobotlab");
		return ret;
	}

	// ctors begin ----
	public IOSequence() {
		sequenceList = new ArrayList<IOSequenceEntry>();
	}

	// assignment end ---

	public IOSequence(final IOSequence other) {
		this();
		set(other);
	};

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final IOSequence other) {
		ID = other.ID;
		sequenceList = other.sequenceList;

	};

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<IOSequence");
		ret.append("{");
		ret.append("\"ID\":\"" + ID + "\"");
		ret.append("\"sequenceList\":" + "\"" + sequenceList.toString() + "\"");

		// ret.append("</IOSequence>");
		ret.append("}");
		return ret.toString();
	}

}