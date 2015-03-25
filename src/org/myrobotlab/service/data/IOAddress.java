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

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class IOAddress implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(IOAddress.class);

	public int ID;
	public String device; // device to write data to - follow comedi examples
							// "/dev/comedi0"
	public int subdevice; // subdevice to write to
	public int channel; // channel to write data to
	public int data; // data to be written or read

	// option constants

	// ctors begin ----
	public IOAddress() {
		device = new String();
	}

	public IOAddress(final IOAddress other) {
		this();
		set(other);
	}

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final IOAddress other) {
		ID = other.ID;
		device = other.device;
		subdevice = other.subdevice;
		channel = other.channel;
		data = other.data;

	}

	// assignment end ---

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<IOAddress");
		ret.append("{");
		ret.append("\"ID\":\"" + ID + "\"");
		ret.append("\"device\":" + "\"" + device + "\"");
		ret.append("\"subdevice\":" + "\"" + subdevice + "\"");
		ret.append("\"channel\":" + "\"" + channel + "\"");
		ret.append("\"data\":" + "\"" + data + "\"");

		// ret.append("</IOAddress>");
		ret.append("}");
		return ret.toString();
	}

}