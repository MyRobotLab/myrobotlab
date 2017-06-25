package org.myrobotlab.framework;

import java.io.IOException;

import org.myrobotlab.codec.CodecUri;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.service.Runtime;

public class InvokerUtils {

  // FIXME - need to throw on error - returning null is "often" valid
  public static Object invoke(String uri) throws IOException {
    Message msg = CodecUri.decodePathInfo(uri);
    ServiceInterface si = Runtime.getService(msg.name);
    Object ret = si.invoke(msg.method, msg.data);
    return ret;
  }

}
