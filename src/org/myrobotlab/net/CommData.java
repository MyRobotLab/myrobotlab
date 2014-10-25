package org.myrobotlab.net;

import java.net.URI;
import java.util.HashMap;

/**
 * @author GroG
 * 
 * class to store connection information for Gateways
 * this will be the data component of a MRL_URI_KEY
 * since there are so many connection types and connection
 * protocols on top of those types we will make a data class
 * which has members which are common to all - then a
 * HashMap of properties for specific elements
 * 
 * future data might include session info, session time outs, heartbeat details, etc
 *
 */
public class CommData {
	
	/**
	 * proto key - mrlkey is mrl://gatewayName/protoKey
	 */
	
	public URI uri;
	//public URI protoKey; - use it if its useful
	
	String mode; // adaptive ?
	
	public int rx = 0;
	public int tx = 0;

	public String method;
	public String sender;
	
	public boolean authenticated = false;
	
	public HashMap<String, String> addInfo = new HashMap<String, String>();

}
