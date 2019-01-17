package org.myrobotlab.codec;

import java.io.IOException;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.NameProvider;

public interface Recorder {

  public abstract void start(NameProvider service) throws IOException;

  public abstract void stop() throws IOException;

  public abstract void write(Message msg) throws IOException;

}