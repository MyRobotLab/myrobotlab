package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.MyoData;

public interface MyoDataListener {

  public String getName();

  public MyoData onMyoData(MyoData data);
}
