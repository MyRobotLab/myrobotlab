package org.myrobotlab.codec;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URLDecoder;

import org.myrobotlab.codec.ApiFactory.ApiDescription;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.interfaces.MessageSender;
import org.myrobotlab.framework.interfaces.ServiceInterface;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.Runtime;
import org.slf4j.Logger;

/**
 * 
 * @author GroG
 *
 *         <pre>
 * 
 * This is a "blocking" API - much like a simple function call with a return value, this
 * will "block" until the transaction has completed and the result is ready.
 * 
 * Usually, it is better "NOT TO BLOCK" - because we have better things to do than wait
 * around for returned data.  The Messages API is asynchronous and is designed to return
 * data without "blocking"
 * 
 * must be in one of the following forms
 * GET http://host/api/services/{name}                  return service's data
 * GET http://host/api/services/{name}/                 list methods
 * GET http://host/api/services/{name}/{method}         (call method - no params)
 * GET http://host/api/services/{name}/{method}         no parameters
 * GET http://host/api/services/{name}/{method}/.../.   parameters
 * 
 * POST http://host/api/services/{name}/{method}        POST data is a json array of json encoded strings for parameters
 *     
 * data can be in post or on parameters
 *         </pre>
 * 
 * 
 *         FIXME !!! - this is wrong .. needs to call a "framework" invoke -
 *         when a method is called, the notify lists need to be "notified" !
 *
 */
public class ApiService extends Api {

  public final static Logger log = LoggerFactory.getLogger(ApiService.class);

  /**
   * A message could be encoded in the requestUri ... or it could be in data
   * 
   * The precedence is to decode addressing and all data associated with the
   * requestUri over the data.
   * 
   * FIXME - beyond ApiFactory -&gt; a 'initial' message would be formed - which is
   * the results of the request URI being parsed
   * 
   */

  //
  public Object process(MessageSender sender, OutputStream out, Message msgFromUri, String data) throws Exception {

    // FIXME change to CodecUtils.MIME_TYPE_JSON
    Codec codec = CodecFactory.getCodec(CodecUtils.MIME_TYPE_JSON);

    // FIXME - MUST DECIDE IF BOTH msgFromUri data is preset and input String
    // data - what should happen ???
    Object ret = null;

    // GET - parameters are part of uri
    // REQUIREMENT must be in an encoded array - even binary
    // 1. array is URI /
    // 2. will need to decode contents of each parameter later based
    // on signature of reflected method

    // get params from uri - its our array
    // difference is initial state regardless of encoding we are
    // guaranteed the URI parts are strings
    // encodedArray = new Object[parts.length - 3];

    // prepare to get a service instance and go
    // through type conversions based on method signatures

    // registry based - merging all local definitions
    // with incoming request - to solve what request wants
    // to decode parameter

    // FIXME - TODO - conform to the Invoker interface - and build the "One
    // Invoker to Rule Them All"
    ServiceInterface si = Runtime.getService(msgFromUri.name);
    if (si == null) {
      throw new IOException(String.format("service %s not found", msgFromUri.name));
    }

    Class<?> clazz = si.getClass();
    Class<?>[] paramTypes = null;
    Object[] params = new Object[0];
    Object[] encodedArray = new Object[0];

    if (msgFromUri.data != null) {

      encodedArray = new Object[msgFromUri.data.length];

      for (int i = 0; i < encodedArray.length; ++i) {
        String result = URLDecoder.decode((String) msgFromUri.data[i], "UTF-8");
        encodedArray[i] = result;
      }

      // FETCH AND MERGE METHOD - we have ordinal count now - but NOT the
      // decoded
      // parameters
      // NOW HAVE ORDINAL - fetch the method with its types
      paramTypes = MethodCache.getCandidateOnOrdinalSignature(si.getClass(), msgFromUri.method, encodedArray.length);
      // WE NOW HAVE ORDINAL AND TYPES
      params = new Object[encodedArray.length];

      // DECODE AND FILL THE PARAMS
      for (int i = 0; i < params.length; ++i) {
        params[i] = codec.decode(encodedArray[i], paramTypes[i]);
      }
    }
    // FIXME - ONE INVOKER !!! ONE METHOD CACHE !!!
    Method method = clazz.getMethod(msgFromUri.method, paramTypes);
    // send vs send blocking ...
    // sender.send(msgFromUri);

    if (si.isLocal()) {
      log.debug("{} is local", msgFromUri.name);
      ret = method.invoke(si, params);
    } else {
      // FIXME - create blocking message request
      log.debug("{} is is remote", msgFromUri.name);
      // Message msg = Runtime.getInstance().createMessage(si.getName(),
      // CodecUtils.getCallBackName(methodName), params);
      // FIXME MUST DO BLOCKING MSG !!!
      // FIXME - sendBlocking should throw and exception if it can't send !!!
      // NOT JUST RETURN NULL !!!
      ret = sender.sendBlocking(msgFromUri.name, msgFromUri.method, params);
    }

    if (out != null) {
      if (ret == null){
        codec.encode(out, ret);
      } else if (Serializable.class.isAssignableFrom(ret.getClass())){
        codec.encode(out, ret);
      } else {
        log.error("could not serialize return from {} class {}", method, ret.getClass());
      }
    }

    MethodCache.cache(clazz, method);

    return ret;

  }
  
  public static ApiDescription getDescription() {
    ApiDescription desc = new ApiDescription("message", "{scheme}://{host}:{port}/api/service", "http://localhost:8888/api/service/runtime/getUptime",
        "An synchronous api useful for simple REST responses");
    return desc;
  }
}