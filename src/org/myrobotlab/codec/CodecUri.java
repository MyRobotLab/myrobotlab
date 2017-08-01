package org.myrobotlab.codec;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;

import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.TypeConverter;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

public class CodecUri {

  public final static Logger log = LoggerFactory.getLogger(CodecUri.class);

  public static Message decodeURI(URI uri) throws IOException {
    log.info(String.format("authority %s", uri.getAuthority())); // gperry:blahblah@localhost:7777
    log.info(String.format("     host %s", uri.getHost())); // localhost
    log.info(String.format("     port %d", uri.getPort())); // 7777
    log.info(String.format("     path %s", uri.getPath()));
    log.info(String.format("    query %s", uri.getQuery())); // /api/string/gson/runtime/getUptime
    log.info(String.format("   scheme %s", uri.getScheme())); // http
    log.info(String.format(" userInfo %s", uri.getUserInfo())); // gperry:blahblah

    Message msg = decodePathInfo(uri.getPath());

    return msg;
  }

  /**
   * FIXME - this method requires the class to be loaded for type conversions
   * !!! Decoding a URI or path can depend on Context &amp; Environment part of
   * decoding relies on the method signature of an object - therefore it has to
   * be loaded in memory, but if the ability to send messages from outside this
   * system is desired - then the Message must be able to SPECIFY THE DECODING
   * IT NEEDS !!! - without the clazz available !!!
   * 
   * URI path decoder - decodes a path into a MRL Message. Details are here
   * http://myrobotlab.org/content/myrobotlab-api JSON is the default encoding
   * 
   * @param pathInfo
   *          - input path in the format -
   *          /{api-type}(/encoding=json/decoding=json/)/{method}/{param0}/{
   *          param1}/...
   * @return message
   * @throws IOException e 
   */

  // FIXME - reconcile with WebGUIServlet
  public static final Message decodePathInfo(String pathInfo) throws IOException {

    // FIXME optimization of HashSet combinations of supported encoding instead
    // of parsing...
    // e.g. HashMap<String> supportedEncoding.containsKey(
    // refer to - http://myrobotlab.org/content/myrobotlab-api

    String[] parts = pathInfo.split("/");
    // String trailingCharacter = pathInfo.substring(pathInfo.length() - 1);

    // synchronous - blocking
    // Encoder.invoke(Outputs = null, "path");
    // search for //: for protocol ?

    // api has functionality ..
    // it delivers the next "set" of access points - which is the services
    // this allows the calling interface to query

    if (!Api.PREFIX_API.equals(parts[1])) {
      throw new IOException(String.format("/api expected received %s", pathInfo));
    }

    // FIXME INVOKING VS PUTTING A MESSAGE ON THE BUS
    Message msg = new Message();

    if (parts.length > 3) {
      msg.name = parts[2];
      msg.method = parts[3];
    } else if (parts.length == 3) {
      // lazy runtime method call
      msg.method = parts[2];
      // FIXME - NOT GOOD - the encoder SHOULD NOT NEED OR DEPEND ON ANY RUNTIME
      // OR
      // INSTANCE INFO !!
      // precedence -
      // 1. Runtime method
      /*
       * if (Runtime.getInstance().getMessageSet().contains(msg.method)){
       * 
       * } // 2. get named instance of service if ()
       */

    } else {
      // lazy runtime help
      msg.method = "help";
      return msg;
    }

    if (parts.length > 4) {
      // FIXME - ALL STRINGS AT THE MOMENT !!!
      String[] jsonParams = new String[parts.length - 4];
      // System.arraycopy(parts, 4, jsonParams, 0, parts.length - 4);

      // FIXME - this is a huge assumption of type of encoding ! - needs to be
      // dynamic !
      for (int i = 0; i < jsonParams.length; ++i) {
        String result = URLDecoder.decode(parts[i + 4], "UTF-8");
        jsonParams[i] = result;
      }

      ServiceInterface si = org.myrobotlab.service.Runtime.getService(msg.name);
      if (si == null){
    	  si = org.myrobotlab.service.Runtime.getInstance();
      }
      // FIXME - this is a huge assumption of type of encoding ! - needs to be
      // dynamic !

      msg.data = TypeConverter.getTypedParamsFromJson(si.getClass(), msg.method, jsonParams);
    }

    return msg;
  }
}
