package org.myrobotlab.net;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;

import org.myrobotlab.codec.CodecUtils;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.NameProvider;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.Runtime;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.Gateway;
import org.slf4j.Logger;

/**
 * goal of this class is to provide the interface for non-blocking communication
 * (local &amp; remote) a good test of this goal is for this class to be used
 * outside of MRL process.
 * 
 * e.g. - the goal is the design of a very small library - using only native
 * dependencies can do all of the necessary messaging MRL supports with very
 * little work
 * 
 * @author GroG
 *
 */
public class CommunicationManager implements Serializable, CommunicationInterface, NameProvider {

  private static final long serialVersionUID = 1L;
  public final static Logger log = LoggerFactory.getLogger(CommunicationManager.class);
  String name;

  /**
   * mrlToProtocolKey -
   */
  static HashMap<URI, URI> mrlToProtocolKey = new HashMap<URI, URI>();

  public CommunicationManager(String name) {
    this.name = name;
  }

  // FIXME - put in Runtime
  @Override
  public void addRemote(URI mrlHost, URI protocolKey) {
    mrlToProtocolKey.put(mrlHost, protocolKey);
  }

  /*
   * mrl:/ get a gateway for remote communication
   */
  public Gateway getComm(URI uri) {
    if (uri.getScheme().equals(CodecUtils.SCHEME_MRL)) {
      Gateway gateway = (Gateway) Runtime.getService(uri.getHost());
      return gateway;
    } else {
      log.error(String.format("%s not SCHEME_MRL", uri));
      return null;
    }
  }

  @Override
  final public void send(final Message msg) {

    ServiceInterface sw = Runtime.getService(msg.getName());
    if (sw == null) {
      log.error(String.format("could not find service %s to process %s from sender %s - tearing down route", msg.name, msg.method, msg.sender));
      ServiceInterface sender = Runtime.getService(msg.sender);
      if (sender != null) {
        sender.removeListener(msg.sendingMethod, msg.getName(), msg.method);
      }
      return;
    }

    URI host = sw.getInstanceId();
    if (host == null) {
      // local message
      // log.info(String.format("local %s.%s->%s/%s.%s(%s)", msg.sender,
      // msg.sendingMethod, sw.getHost(), msg.name, msg.method,
      // Encoder.getParameterSignature(msg.data)));
      sw.in(msg);
    } else {
      // remote message
      // log.info(String.format("remote %s.%s->%s/%s.%s(%s)", msg.sender,
      // msg.sendingMethod, sw.getHost(), msg.name, msg.method,
      // Encoder.getParameterSignature(msg.data)));

      URI protocolKey = mrlToProtocolKey.get(host);
      getComm(host).sendRemote(protocolKey, msg);
    }
  }

  /**
   * get a gateway, send the message through the gateway with a protocol key
   */
  @Override
  final public void send(final URI uri, final Message msg) {
    getComm(uri).sendRemote(uri, msg);
  }

  // FIXME - remove all others !!!
  public Message createMessage(String name, String method, Object... data) {
    Message msg = new Message();
    msg.name = name; // destination instance name
    msg.sender = getName();
    msg.data = data;
    msg.method = method;

    return msg;
  }

  @Override
  public String getName() {
    return name;
  }

  static public int count(String data, char toCount) {
    int charCount = 0;

    for (int i = 0; i < data.length(); i++) {
      char tmp = data.charAt(i);

      if (toCount == tmp)
        ++charCount;
    }

    return charCount;
  }

  public static void main(String[] args) {
    LoggingFactory.init(Level.WARN);

    // TODO - send a verify for service & another verify for method ?

    /*
     * FIXME PUT IN JUNIT TestThrower thrower =
     * (TestThrower)Runtime.start("thrower", "TestThrower"); TestCatcher catcher
     * = (TestCatcher)Runtime.start("catcher", "TestCatcher");
     * 
     * CommunicationManager cm = new CommunicationManager("catcher");
     */

  }

}
