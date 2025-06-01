package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;

public interface HttpResponseListener extends NameProvider {

  public void onHttpResponse(String data);

}
