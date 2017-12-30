package org.myrobotlab.codec;

import java.io.IOException;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.NameProvider;

public class RecorderXmlFile implements Recorder {

  @Override
  public void write(Message msg) throws IOException {
    // Object[] data = msg.data;
    // String msgName = (msg.name.equals(Runtime.getInstance().getName())) ?
    // "runtime" : msg.name;
    // TODO implement

  }

  @Override
  public void start(NameProvider service) throws IOException {
    // TODO Auto-generated method stub

  }

  @Override
  public void stop() throws IOException {
    // TODO Auto-generated method stub

  }

}
