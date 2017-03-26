package org.myrobotlab.service.interfaces;

import java.util.HashMap;

import org.myrobotlab.framework.Message;

public interface AuthorizationProvider {

  boolean allowExport(String serviceName);

  // from remote not all inbound
  boolean isAuthorized(HashMap<String, String> security, String serviceName, String method);

  boolean isAuthorized(Message msg);

}
