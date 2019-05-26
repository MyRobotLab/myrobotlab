package org.myrobotlab.service.interfaces;

import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.service.data.HttpData;

public interface HttpDataListener extends NameProvider {

  public void onHttpData(HttpData data);

}
