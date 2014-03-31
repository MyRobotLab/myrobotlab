package org.myrobotlab.net;

import java.net.URI;

public class CommData {
	
	public URI uri;
	
	public int rx = 0;
	public int tx = 0;
	// implmented with String to be immutable / thread safe
	public String method;
	public String sender;
	
	public boolean authenticated = false;

}
