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

public class IPAndPort implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static Logger log = LoggerFactory.getLogger(IPAndPort.class);

	public String IPAddress; // address
	public int port; // address

	// ctors begin ----
	public IPAndPort() {
	}

	public IPAndPort(final IPAndPort other) {
		this();
		set(other);
	}

	public IPAndPort(final String IPAddress, final int port) {
		this.IPAddress = IPAddress;
		this.port = port;
	}

	// ctors end ----
	// assignment begin --- todo - look @ clone copy
	public void set(final IPAndPort other) {
		IPAddress = other.IPAddress;
		port = other.port;

	}

	// assignment end ---

	/*
	 * Default format was xml is now JSON TODO - make toStringStyler like spring
	 */
	@Override
	public String toString() {
		StringBuffer ret = new StringBuffer();
		// ret.append("{<PinData");
		ret.append("{");
		ret.append("\"IPAddress\":\"" + IPAddress + "\"");
		ret.append("\"port\":" + "\"" + port + "\"");

		// ret.append("</PinData>");
		ret.append("}");
		return ret.toString();
	}

}