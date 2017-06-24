package org.myrobotlab.codec;

import java.io.OutputStream;
import java.net.URI;

import org.myrobotlab.framework.interfaces.MessageSender;

public interface ApiProcessor {

  public Object process(MessageSender sender, OutputStream out, URI uri, byte[] data) throws Exception;

}
