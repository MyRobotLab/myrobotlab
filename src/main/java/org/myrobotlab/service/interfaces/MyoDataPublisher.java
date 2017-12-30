package org.myrobotlab.service.interfaces;

import org.myrobotlab.service.data.MyoData;

public interface MyoDataPublisher {

  public String getName();

  public MyoData publishMyoData(MyoData data);

}
