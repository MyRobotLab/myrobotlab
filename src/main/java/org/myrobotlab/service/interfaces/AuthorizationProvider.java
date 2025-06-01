package org.myrobotlab.service.interfaces;

import java.util.Map;

import org.myrobotlab.framework.Message;

public interface AuthorizationProvider {

  boolean allowExport(String serviceName);

  // from remote not all inbound
  boolean isAuthorized(Map<String, Object> security, String serviceName, String method);

  boolean isAuthorized(Message msg);

}
