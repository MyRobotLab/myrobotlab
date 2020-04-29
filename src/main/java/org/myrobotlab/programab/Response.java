package org.myrobotlab.programab;

import java.util.Date;
import java.util.List;

/**
 * Internal class for the program ab response. TODO - probably should have a
 * generalized data response for all bots to support an interface
 */
public class Response {
  // FIXME - timestamps are usually longs System.currentTimeMillis()
  public Date timestamp = new Date();
  /**
   * the bot it came from
   */
  public String botName;
  /**
   * the user this response is for
   */
  public String userName;
  /**
   * text only response
   */
  public String msg;
  /**
   * filtered oob data
   */
  public List<OOBPayload> payloads;

  public Response(String userName, String botName, String msg, List<OOBPayload> payloads) {
    this.botName = botName;
    this.userName = userName;
    this.msg = msg;
    this.payloads = payloads;
  }

  public String toString() {
    StringBuilder str = new StringBuilder();
    str.append("[");
    str.append("Time:" + timestamp.getTime() + ", ");
    str.append("Bot:" + botName + ", ");
    str.append("User:" + userName + ", ");
    str.append("Msg:" + msg + ", ");
    str.append("Payloads:[");
    if (payloads != null) {
      for (OOBPayload payload : payloads) {
        str.append(payload.toString() + ", ");
      }
    }
    str.append("]]");
    return str.toString();
  }
}
