package org.myrobotlab.service.interfaces;

import java.util.HashMap;

import org.myrobotlab.framework.Message;

public interface AuthorizationProvider {
	
	boolean isAuthorized(Message msg);
	
	// from remote not all inbound
	boolean isAuthorized(HashMap<String,String> security, String serviceName, String method);

	boolean allowExport(String serviceName);

}
