package org.myrobotlab.net;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;

import org.myrobotlab.framework.Platform;

/**
 * @author GroG
 * 
 *         class to store connection information for Gateways this will be the
 *         data component of a MRL_URI_KEY since there are so many connection
 *         types and connection protocols on top of those types we will make a
 *         data class which has members which are common to all - then a HashMap
 *         of properties for specific elements
 * 
 *         future data might include session info, session time outs, heartbeat
 *         details, etc
 * 
 *         this will contain all serializable contextual data regarding the
 *         connection without containing the connection itself
 *
 */
public class Connection implements Serializable {

  private static final long serialVersionUID = 1L;

  long lastStateChange = 0;

  // states
  public final static String DISCOVERED = "DISCOVERED";
  public final static String CONNECTED = "CONNECTED";
  public final static String UNKNOWN = "UNKNOWN";
  public final static String DISCONNECTED = "DISCONNECTED";
  public final static String CONNECTING = "CONNECTING";

  // types - connection / connection-less

  /**
   * proto key - mrlkey is mrl://gatewayName/protocolKey
   * 
   */
  private String service;
  public URI protocolKey;
  public String prefix;
  public Platform platform;

  // String mode; // adaptive ?
  public String state = UNKNOWN; // adaptive ?

  // statistics and info
  public int rx = 0;
  public int tx = 0;

  public String rxSender;
  public String rxSendingMethod;
  public String rxName;
  public String rxMethod;

  public String txSender;
  public String txSendingMethod;
  public String txName;
  public String txMethod;

  public boolean authenticated = false;

  public HashMap<String, String> addInfo = new HashMap<String, String>();

  public Connection() {
  }

  public Connection(String gatewayName, URI protocolKey) {
    this.service = gatewayName;
    this.protocolKey = protocolKey;
    /*
     * try { this.protocolKey = new URI(String.format("mrl://%s/%s",
     * gatewayName, uri)); } catch (URISyntaxException e) {
     * Logging.logException(e); }
     */
  }

  @Override
  public String toString() {
    return String.format("%s %s rx %d %s.%s --> %s.%s tx %d %s.%s --> %s.%s", protocolKey, state, rx, rxSender, rxSendingMethod, rxName, rxMethod, tx, txSender, txSendingMethod,
        txName, txMethod);
  }

}
