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

package org.myrobotlab.framework;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementArray;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 * @author GroG
 * 
 */
@Root
public class Message implements Serializable {
	private static final long serialVersionUID = 1L;
	public final static String BLOCKING = "B";
	public final static String RETURN = "R";

	/**
	 * unique identifier for this message - TODO remove
	 */
	@Element
	public long msgID;
	/**
	 * datetimestamp when message is created GMT - hashCode?
	 */
	@Element
	public long timeStamp;
	/**
	 * globally unique name of destination Service. This will be the Service
	 * endpoint of this Message.
	 */
	@Element
	public String name;
	/**
	 * name of the sending Service which sent this Message
	 */
	@Element
	public String sender;
	/**
	 * originating source method which generated this Message
	 */
	@Element
	public String sendingMethod;
	/**
	 * history of the message, its routing stops and Services it passed through.
	 * This is important to prevent endless looping of messages.
	 * Turns out ArrayList is quicker than HashSet on small sets
	 * http://www.javacodegeeks.com/2010/08/java-best-practices-vector-arraylist.html
	 */
	//public ArrayList<RoutingEntry> historyList;
	public HashSet<String> historyList;
	public HashMap<String,String> security;
	/*
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data);
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((msgID == null) ? 0 : msgID.hashCode());
		result = prime * result + ((msgType == null) ? 0 : msgType.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sender == null) ? 0 : sender.hashCode());
		result = prime * result + ((sendingMethod == null) ? 0 : sendingMethod.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		result = prime * result + (int) (timeStamp ^ (timeStamp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		if (!Arrays.equals(data, other.data))
			return false;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (msgID == null) {
			if (other.msgID != null)
				return false;
		} else if (!msgID.equals(other.msgID))
			return false;
		if (msgType == null) {
			if (other.msgType != null)
				return false;
		} else if (!msgType.equals(other.msgType))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sender == null) {
			if (other.sender != null)
				return false;
		} else if (!sender.equals(other.sender))
			return false;
		if (sendingMethod == null) {
			if (other.sendingMethod != null)
				return false;
		} else if (!sendingMethod.equals(other.sendingMethod))
			return false;
		if (status == null) {
			if (other.status != null)
				return false;
		} else if (!status.equals(other.status))
			return false;
		if (timeStamp != other.timeStamp)
			return false;
		return true;
	}
*/
	/**
	 * status is currently used for BLOCKING message calls the current valid
	 * state it can be in is null | BLOCKING | RETURN FIXME - this should be
	 * msgType not status
	 */
	@Element(required=false)
	public String status;
	
	@Element(required=false)
	public String msgType; // Broadcast|Blocking|Blocking Return - deprecated
	/**
	 * the method which will be invoked on the destination @see Service
	 */
	@Element
	public String method;

	/**
	 * the data which will be sent to the destination method data payload - if
	 * invoking a service request this would be the parameter (list) - this
	 * would the return type data if the message is outbound
	 */
	@ElementArray(required=false)
	public Object[] data;

	public Message() {
		timeStamp = System.currentTimeMillis();
		msgID = timeStamp; // currently just a timestamp - but it can be more unique if needed
		name = new String(); // FIXME - allow NULL !
		sender = new String(); // FIXME - allow NULL !
		sendingMethod = new String();
		historyList = new HashSet<String>();
		method = new String();
	}

	public Message(final Message other) {
		set(other);
	}

	final public void set(final Message other) {
		msgID = other.msgID;
		
		timeStamp = System.currentTimeMillis();// other.timeStamp;
		name = other.getName();
		sender = other.sender;
		sendingMethod = other.sendingMethod;
		// FIXED - not valid making a copy of a message
		// to send and copying there history list
		//historyList = other.historyList; 
		historyList = new HashSet<String>();
		status = other.status;
		msgType = other.msgType;
		method = other.method;
		// you know the dangers of reference copy
		data = other.data; 
	}
	
	// FIXME - put in Encoder
/*
	final public String getParameterSignature() {
		return getParameterSignature(data);
	}

	static final public String getParameterSignature(Object[] data) {
		if (data == null) {
			return "null";
		}

		StringBuffer ret = new StringBuffer();
		for (int i = 0; i < data.length; ++i) {
			if (data[i] != null) {
				Class<?> c = data[i].getClass(); // not all data types are safe
													// toString() e.g.
													// SerializableImage
				if (c == String.class || c == Integer.class || c == Boolean.class || c == Float.class) {
					ret.append(data[i].toString());
				} else {
					String type = data[i].getClass().getCanonicalName();
					String shortTypeName = type.substring(type.lastIndexOf(".") + 1);
					ret.append(shortTypeName);
				}

				if (data.length != i + 1) {
					ret.append(",");
				}
			} else {
				ret.append("null");
			}

		}
		return ret.toString();

	}
*/
	final public void setData(Object... params) {
		this.data = params;
	}

	// TODO - reduce to sender.sendingMethod --> name.method(paramter types)
	public String toString() {
		
		return String.format("msg %s.%s --> %s.%s(%s) - %d", sender, sendingMethod, name, method, Encoder.getParameterSignature(data), msgID);
		/*
		StringBuffer ret = new StringBuffer();
		// ret.append("{<Message");
		ret.append("{");
		ret.append("\"MSGID\":" + "\"" + msgID + "\"");
		ret.append("\"timeStamp\":" + "\"" + timeStamp + "\"");
		ret.append("\"name\":" + "\"" + name + "\"");
		ret.append("\"sender\":" + "\"" + sender + "\"");
		ret.append("\"sendingMethod\":" + "\"" + sendingMethod + "\"");
		// ret.append("\"historyList\":" + "\"" + historyList.toString() +
		// "\"");
		ret.append("\"status\":" + "\"" + status + "\"");
		ret.append("\"msgType\":" + "\"" + msgType + "\"");
		ret.append("\"method\":" + "\"" + method + "\"");
		ret.append("\"dataClass\":" + "\"" + Encoder.getParameterSignature(this.data) + "\"");
		ret.append("\"data\":" + "\"" + data + "\"");

		// ret.append("</Message>");
		ret.append("}");
		return ret.toString();
		*/
	}

	public Object[] getData() {
		return data;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	

	public static void main(String[] args) throws InterruptedException {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.DEBUG);
		
		Message msg = new Message();
		msg.method = "myMethod";
		msg.sendingMethod = "publishImage";
		msg.timeStamp = System.currentTimeMillis();
		msg.data = new Object[]{"hello"};
		
		Serializer serializer = new Persister();

		try {
			File cfg = new File(String.format("msg.xml"));
			serializer.write(msg, cfg);
		} catch (Exception e) {
			Logging.logException(e);
		}
	}
}